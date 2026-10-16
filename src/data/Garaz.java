package data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

public class Garaz {

    /** Úspěchy, které se dají v garáži odemknout. */
    public enum Uspech {
        PRVNI_VYLET("První výlet", "Podnikni svůj první výlet", 1),
        STOVKAR("Stovkař", "Ujeď celkem 100 km", 100),
        TISICOVKA("Tisícovka", "Ujeď celkem 1 000 km", 1000),
        KRAL_SILNIC("Král silnic", "Ujeď celkem 10 000 km", 10000),
        DALKOVY_JEZDEC("Dálkový jezdec", "Jeden výlet alespoň 500 km (tam a zpět)", 500),
        STAMGAST("Štamgast na pumpě", "Natankuj celkem 10×", 10),
        SBERATEL("Sběratel", "Měj v garáži najednou 5 aut", 5),
        VETERAN("Veterán", "Měj v garáži auto staré alespoň 30 let", 30),
        MECHANIK("Mechanik", "Zapiš 5 servisů", 5),
        POKLADNIK("Pokladník", "Vejdi se s plánem servisu do rozpočtu", 1);

        private final String nazev;
        private final String popis;
        private final int cil;

        Uspech(String nazev, String popis, int cil) {
            this.nazev = nazev;
            this.popis = popis;
            this.cil = cil;
        }

        public String getNazev() {
            return nazev;
        }

        public String getPopis() {
            return popis;
        }

        /** Hodnota, které je potřeba dosáhnout (km, počet výletů, počet aut...). */
        public int getCil() {
            return cil;
        }
    }

    /** Faktory, ze kterých se počítá skóre priority údržby (0-100). */
    public enum Faktor {
        STARI("Stáří auta", "Starší auto = vyšší priorita (strop při 30 letech)"),
        NAJETO("Najeté km", "Víc kilometrů na výletech = vyšší priorita (strop při 5 000 km)"),
        SPOTREBA("Spotřeba", "Vyšší spotřeba = vyšší priorita (strop při 15 l/100 km)"),
        PALIVO("Málo paliva", "Prázdnější nádrž = vyšší priorita"),
        SERVIS("Servis", "Čím blíž servisnímu intervalu, tím vyšší priorita (strop při jeho překročení)"),
        STK("STK", "Blížící se nebo prošlá technická kontrola (počítá se posledních 90 dní)");

        private final String nazev;
        private final String popis;

        Faktor(String nazev, String popis) {
            this.nazev = nazev;
            this.popis = popis;
        }

        public String getNazev() {
            return nazev;
        }

        public String getPopis() {
            return popis;
        }
    }

    /** Předvolby vah: rychlé přepnutí, na co se při řazení dívat. Pořadí hodnot = pořadí enumu Faktor. */
    public enum Predvolba {
        VYROVNANA("Vyrovnaná", "Výchozí mix všech faktorů", new int[]{15, 15, 10, 15, 25, 20}),
        BEZPECNOST("Bezpečnost", "Servis a STK mají přednost", new int[]{10, 5, 0, 5, 45, 35}),
        USPORA("Úspora", "Žíznivá auta a prázdné nádrže první", new int[]{5, 10, 50, 25, 10, 0}),
        VETERANI("Veteráni", "Stará a hodně jetá auta první", new int[]{45, 30, 5, 0, 15, 5});

        private final String nazev;
        private final String popis;
        private final int[] vahy;

        Predvolba(String nazev, String popis, int[] vahy) {
            this.nazev = nazev;
            this.popis = popis;
            this.vahy = vahy;
        }

        public String getNazev() {
            return nazev;
        }

        public String getPopis() {
            return popis;
        }

        public int getVaha(Faktor f) {
            return vahy[f.ordinal()];
        }
    }

    /** Jeden záznam v historii změn: kdo, kdy a co se stalo. */
    public static class Zaznam {
        private final LocalDateTime cas;
        private final String kdo;
        private final String text;

        Zaznam(LocalDateTime cas, String kdo, String text) {
            this.cas = cas;
            this.kdo = kdo;
            this.text = text;
        }

        public LocalDateTime getCas() {
            return cas;
        }

        public String getCasText() {
            return cas.format(DTF);
        }

        public String getKdo() {
            return kdo;
        }

        public String getText() {
            return text;
        }
    }

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Locale CS = Locale.forLanguageTag("cs-CZ");

    /** Pevné prahy skóre pro třídy priority (zvoleno odhadem, dají se doladit). */
    public static final float PRAH_KRITICKA = 65;
    public static final float PRAH_VYSOKA = 45;
    public static final float PRAH_STREDNI = 25;

    /** Výchozí váhy faktorů ve stejném pořadí jako enum Faktor (zvoleno odhadem, uživatel je mění). */
    private static final int[] VYCHOZI_VAHY = {15, 15, 10, 15, 25, 20};

    private int garazId; private static int lastGarazId = 0;
    private int pocetMist;
    private final Auto[] auta;
    private int pocetAut = 0;

    // statistiky garáže (zůstávají, i když se auto odebere)
    private float celkemKm = 0;
    private int pocetVyletu = 0;
    private int pocetTankovani = 0;
    private float nejdelsiVylet = 0;
    private final EnumSet<Uspech> odemcene = EnumSet.noneOf(Uspech.class);

    private final int[] vahy = VYCHOZI_VAHY.clone();
    private final List<Zaznam> historie = new ArrayList<>();

    static {
        lastGarazId = 100;
    }

    public Garaz(int pocetMist) {
        garazId = ++ lastGarazId;
        this.pocetMist = pocetMist;   // dřív se nenastavovalo, takže garáž byla "plná" hned po vytvoření
        auta =  new Auto[pocetMist];
    }

    public int getID() {
        return garazId;
    }

    public Auto getCar(int index) {
        return auta[index];
    }

    public int getPocetAut() {
        return pocetAut;
    }

    public Auto removeCar(int index) {
        Auto auto = auta[index];
        if (auto == null) return null;   // z prázdného místa není co odebírat
        auta[index] = null;
        pocetAut--;
        return auto;
    }

    /** Odebere konkrétní auto (podle objektu). Vrací true, když se podařilo. */
    public boolean removeCar(Auto auto) {
        for (int i = 0; i < auta.length; i++) {
            if (auta[i] == auto) {
                removeCar(i);
                return true;
            }
        }
        return false;
    }

    public int getPocetMist() {
        return pocetMist;
    }

    public int getPocetVolnychMist() {
        return pocetMist - pocetAut;
    }

    private int indexOfNull() {
        int index = 0;

        for (; index < auta.length; index++) {
            if (auta[index] == null) break;
        }

        return index;
    }

    /** Přidá auto. Vrací false, když je garáž plná. */
    public boolean addCar(Auto auto) {
        if (jePlna()) return false;
        auta[indexOfNull()] = auto;
        pocetAut++;
        return true;
    }

    public boolean jePlna() {
        return pocetAut == pocetMist;
    }

    public boolean jePrazdna() {
        return pocetAut == 0;
    }

    public Auto[] getCarArray() {
        return Arrays.copyOf(auta, auta.length);
    }

    /** Seznam všech aut bez prázdných míst (po odebrání zůstávají v poli díry). */
    public List<Auto> getAuta() {
        List<Auto> seznam = new ArrayList<>();
        for (Auto a : auta) {
            if (a != null) seznam.add(a);
        }
        return seznam;
    }

    public Auto najdiAuto(int id) {
        for (Auto a : auta) if (a != null && a.getCarId() == id) return a;
        return null;
    }

    // ------------------------------------------------------------------
    // statistiky a úspěchy
    // ------------------------------------------------------------------

    /** Zapíše výlet (km jedním směrem, výlet je tam a zpět) a kolikrát se při něm tankovalo. */
    public void zaznamenejVylet(int kmJednimSmerem, int tankovani) {
        zaznamenejJizdu(kmJednimSmerem * 2f, tankovani);
    }

    /** Zapíše jízdu o celkové délce km (už sečtené tam i zpět). */
    public void zaznamenejJizdu(float km, int tankovani) {
        celkemKm += km;
        pocetVyletu++;
        pocetTankovani += Math.max(0, tankovani);
        if (km > nejdelsiVylet) nejdelsiVylet = km;
    }

    public void zaznamenejTankovani() {
        pocetTankovani++;
    }

    public float getCelkemKm() {
        return celkemKm;
    }

    public int getPocetVyletu() {
        return pocetVyletu;
    }

    public int getPocetTankovani() {
        return pocetTankovani;
    }

    public float getNejdelsiVylet() {
        return nejdelsiVylet;
    }

    /** Aktuální hodnota, kterou se úspěch měří (porovnává se s Uspech.getCil()). */
    public float getProgress(Uspech u) {
        switch (u) {
            case PRVNI_VYLET:
                return pocetVyletu;
            case STOVKAR:
            case TISICOVKA:
            case KRAL_SILNIC:
                return celkemKm;
            case DALKOVY_JEZDEC:
                return nejdelsiVylet;
            case STAMGAST:
                return pocetTankovani;
            case SBERATEL:
                return pocetAut;
            case VETERAN:
                int rok = Year.now().getValue();
                int nejstarsi = 0;
                for (Auto a : auta) {
                    if (a != null && a.getRokVyroby() > 0) {
                        nejstarsi = Math.max(nejstarsi, rok - a.getRokVyroby());
                    }
                }
                return nejstarsi;
            case MECHANIK:
                return servisKniha.size();
            case POKLADNIK:
                if (pocetAut == 0) return 0;
                for (PlanPolozka p : planRozpoctu(rocniRozpocet)) {
                    if (!p.vejdeSe) return 0;
                }
                return 1;
            default:
                return 0;
        }
    }

    public boolean jeOdemcen(Uspech u) {
        return odemcene.contains(u);
    }

    /** Odemkne všechny nově splněné úspěchy a vrátí je (prázdný seznam = nic nového). */
    public List<Uspech> zkontrolujUspechy() {
        List<Uspech> nove = new ArrayList<>();
        for (Uspech u : Uspech.values()) {
            if (!odemcene.contains(u) && getProgress(u) >= u.getCil()) {
                odemcene.add(u);
                nove.add(u);
            }
        }
        return nove;
    }

    // ------------------------------------------------------------------
    // priorita údržby: skóre 0-100, třídy, váhy, ruční připnutí
    // ------------------------------------------------------------------

    public int getVaha(Faktor f) {
        return vahy[f.ordinal()];
    }

    public void setVaha(Faktor f, int vaha) {
        vahy[f.ordinal()] = Math.max(0, Math.min(100, vaha));
    }

    public void obnovVychoziVahy() {
        System.arraycopy(VYCHOZI_VAHY, 0, vahy, 0, vahy.length);
    }

    public void pouzijPredvolbu(Predvolba p) {
        for (Faktor f : Faktor.values()) {
            vahy[f.ordinal()] = p.getVaha(f);
        }
    }

    /** Předvolba, které odpovídají aktuální váhy, nebo null (vlastní nastavení). */
    public Predvolba getAktivniPredvolba() {
        for (Predvolba p : Predvolba.values()) {
            boolean shoda = true;
            for (Faktor f : Faktor.values()) {
                if (p.getVaha(f) != vahy[f.ordinal()]) {
                    shoda = false;
                    break;
                }
            }
            if (shoda) return p;
        }
        return null;
    }

    private int soucetVah() {
        int soucet = 0;
        for (int v : vahy) soucet += v;
        return soucet;
    }

    private static float clamp01(float x) {
        return Math.max(0f, Math.min(1f, x));
    }

    /** Hodnota jednoho faktoru v rozsahu 0 až 1 (1 = nejvíc urgentní). */
    public float getHodnotaFaktoru(Auto a, Faktor f) {
        switch (f) {
            case STARI:
                if (a.getRokVyroby() <= 0) return 0;
                return clamp01((Year.now().getValue() - a.getRokVyroby()) / 30f);
            case NAJETO:
                return clamp01(a.getUjeteKm() / 5000f);
            case SPOTREBA:
                return clamp01(a.getSpotreba() / 15f);
            case PALIVO:
                return 1f - a.getNaplneni();
            case SERVIS:
                return clamp01(a.getOpotrebeniServisu());
            case STK:
                long dni = a.getDniDoStk();
                if (dni == Integer.MAX_VALUE) return 0;
                return clamp01((90f - dni) / 90f);
            default:
                return 0;
        }
    }

    /** Kolik bodů ze 100 přispívá faktor ke skóre auta (podle aktuálních vah). */
    public float getPrispevek(Auto a, Faktor f) {
        int soucet = soucetVah();
        if (soucet == 0) return 0;
        return 100f * vahy[f.ordinal()] * getHodnotaFaktoru(a, f) / soucet;
    }

    /** Skóre priority 0-100 (součet příspěvků všech faktorů). */
    public float getSkore(Auto a) {
        float skore = 0;
        for (Faktor f : Faktor.values()) {
            skore += getPrispevek(a, f);
        }
        return skore;
    }

    /** Faktor, který ke skóre auta přispívá nejvíc (pro krátké vysvětlení „proč“). */
    public Faktor getHlavniFaktor(Auto a) {
        Faktor hlavni = Faktor.values()[0];
        float max = -1;
        for (Faktor f : Faktor.values()) {
            float p = getPrispevek(a, f);
            if (p > max) {
                max = p;
                hlavni = f;
            }
        }
        return hlavni;
    }

    /** Třída podle skóre, bez ručního zásahu. */
    public Auto.Priorita getModelovaPriorita(Auto a) {
        float skore = getSkore(a);
        if (skore >= PRAH_KRITICKA) return Auto.Priorita.KRITICKA;
        if (skore >= PRAH_VYSOKA) return Auto.Priorita.VYSOKA;
        if (skore >= PRAH_STREDNI) return Auto.Priorita.STREDNI;
        return Auto.Priorita.NIZKA;
    }

    /** Platná třída: ručně připnutá, jinak modelová. Člověk má poslední slovo. */
    public Auto.Priorita getPriorita(Auto a) {
        return a.jePripnuta() ? a.getPripnuta() : getModelovaPriorita(a);
    }

    /** Řadí podle platné třídy (Kritická první), uvnitř třídy podle skóre sestupně. */
    public Comparator<Auto> porovnavacPriorit() {
        return (x, y) -> {
            int c = getPriorita(x).compareTo(getPriorita(y));
            if (c != 0) return c;
            return Float.compare(getSkore(y), getSkore(x));
        };
    }

    /** Auta seřazená podle priority (kterým se věnovat jako první). */
    public List<Auto> getZebricek() {
        List<Auto> seznam = getAuta();
        seznam.sort(porovnavacPriorit());
        return seznam;
    }

    /** Pořadí auta v žebříčku (1 = řešit jako první). */
    public int getPoradi(Auto a) {
        return getZebricek().indexOf(a) + 1;
    }

    /** Hodnota faktoru lidsky čitelně (do porovnání a exportu). */
    public String popisFaktoru(Auto a, Faktor f) {
        switch (f) {
            case STARI:
                return a.getRokVyroby() > 0 ? (Year.now().getValue() - a.getRokVyroby()) + " let" : "neznámé";
            case NAJETO:
                return Math.round(a.getUjeteKm()) + " km";
            case SPOTREBA:
                return String.format(CS, "%.1f l/100 km", a.getSpotreba());
            case PALIVO:
                return "v nádrži " + Math.round(a.getNaplneni() * 100) + " %";
            case SERVIS:
                return Math.round(a.getKmOdServisu()) + " / " + a.getServisniInterval() + " km";
            case STK:
                long dni = a.getDniDoStk();
                if (dni == Integer.MAX_VALUE) return "neznámá";
                return dni < 0 ? "prošlá před " + (-dni) + " dny" : "za " + dni + " dní";
            default:
                return "";
        }
    }

    /** Ručně připne prioritu a zapíše do historie, z čeho na co se změnila a proč. */
    public void pripniPrioritu(Auto a, Auto.Priorita nova, String duvod) {
        Auto.Priorita puvodni = getPriorita(a);
        a.pripni(nova, duvod);
        String d = a.getDuvodPripnuti();
        zapis("Auto " + a + ": priorita " + puvodni + " → " + nova + " (připnuto ručně)"
                + (d.isEmpty() ? "" : ", důvod: " + d));
    }

    public void odepniPrioritu(Auto a) {
        if (!a.jePripnuta()) return;
        Auto.Priorita puvodni = a.getPripnuta();
        a.odepni();
        zapis("Auto " + a + ": ruční priorita " + puvodni + " odebrána, platí modelová "
                + getModelovaPriorita(a));
    }

    // ------------------------------------------------------------------
    // historie změn
    // ------------------------------------------------------------------

    /** Zapíše záznam: kdy, kdo (přihlášený uživatel systému) a co se stalo. */
    public void zapis(String text) {
        historie.add(new Zaznam(LocalDateTime.now(), System.getProperty("user.name", "uživatel"), text));
    }

    /** Historie seřazená od nejstarší po nejnovější. */
    public List<Zaznam> getHistorie() {
        List<Zaznam> h = new ArrayList<>(historie);
        h.sort(Comparator.comparing(Zaznam::getCas));
        return h;
    }

    // ------------------------------------------------------------------
    // obnovení ze souboru (používá Uloziste)
    // ------------------------------------------------------------------

    void obnovZaznam(LocalDateTime cas, String kdo, String text) {
        historie.add(new Zaznam(cas, kdo, text));
    }

    void obnovStatistiky(float km, int vylety, int tankovani, float nejdelsi) {
        this.celkemKm = km;
        this.pocetVyletu = vylety;
        this.pocetTankovani = tankovani;
        this.nejdelsiVylet = nejdelsi;
    }

    void obnovUspech(Uspech u) {
        odemcene.add(u);
    }

    void obnovTankovani(Tankovani t) {
        tankovaniKniha.add(t);
    }

    void obnovServis(Servis s) {
        servisKniha.add(s);
    }

    /** Smaže auta, knihy i historii (nastavení zůstane). */
    public void vymazVse() {
        Arrays.fill(auta, null);
        pocetAut = 0;
        celkemKm = 0;
        pocetVyletu = 0;
        pocetTankovani = 0;
        nejdelsiVylet = 0;
        odemcene.clear();
        historie.clear();
        tankovaniKniha.clear();
        servisKniha.clear();
    }

    // ------------------------------------------------------------------
    // kniha tankování a servisů, náklady
    // ------------------------------------------------------------------

    /** Jedno tankování: kdy, kolik litrů a za kolik. */
    public static class Tankovani {
        private final int autoId;
        private final LocalDate datum;
        private final float litry;
        private final float cenaZaLitr;

        public Tankovani(int autoId, LocalDate datum, float litry, float cenaZaLitr) {
            this.autoId = autoId;
            this.datum = datum;
            this.litry = litry;
            this.cenaZaLitr = cenaZaLitr;
        }

        public int getAutoId() {
            return autoId;
        }

        public LocalDate getDatum() {
            return datum;
        }

        public float getLitry() {
            return litry;
        }

        public float getCenaZaLitr() {
            return cenaZaLitr;
        }

        public float getCena() {
            return litry * cenaZaLitr;
        }
    }

    /** Jeden servis: kdy, co se dělalo, za kolik a při kolika km. */
    public static class Servis {
        private final int autoId;
        private final LocalDate datum;
        private final String popis;
        private final float cena;
        private final float km;

        public Servis(int autoId, LocalDate datum, String popis, float cena, float km) {
            this.autoId = autoId;
            this.datum = datum;
            this.popis = popis == null ? "" : popis;
            this.cena = cena;
            this.km = km;
        }

        public int getAutoId() {
            return autoId;
        }

        public LocalDate getDatum() {
            return datum;
        }

        public String getPopis() {
            return popis;
        }

        public float getCena() {
            return cena;
        }

        public float getKm() {
            return km;
        }
    }

    private final List<Tankovani> tankovaniKniha = new ArrayList<>();
    private final List<Servis> servisKniha = new ArrayList<>();
    private float cenaPaliva = 38.9f;     // Kč za litr benzínu (ostatní paliva přes koeficient)
    private int rocniRozpocet = 60000;    // Kč na servis za rok

    public float getCenaPaliva() {
        return cenaPaliva;
    }

    public void setCenaPaliva(float cena) {
        this.cenaPaliva = Math.max(0, cena);
    }

    /** Cena za litr pro konkrétní auto (podle druhu paliva). */
    public float getCenaPaliva(Auto a) {
        return cenaPaliva * a.getDruhPaliva().getKoeficientCeny();
    }

    public int getRocniRozpocet() {
        return rocniRozpocet;
    }

    public void setRocniRozpocet(int rozpocet) {
        this.rocniRozpocet = Math.max(0, rozpocet);
    }

    /** Natankuje auto (nejvýš do plna) a zapíše tankování do knihy. Vrací skutečně natankované litry. */
    public float natankuj(Auto a, float litry) {
        float l = Math.min(litry, a.getMaxPalivo() - a.getPalivo());
        if (l <= 0) return 0;
        a.setPalivo(a.getPalivo() + l);
        tankovaniKniha.add(new Tankovani(a.getCarId(), LocalDate.now(), l, getCenaPaliva(a)));
        pocetTankovani++;
        return l;
    }

    /** Zapíše servis a vynuluje km od servisu. */
    public void servisuj(Auto a, String popis, float cena, LocalDate datum) {
        String p = popis == null ? "" : popis.trim();
        servisKniha.add(new Servis(a.getCarId(), datum, p, cena, a.getUjeteKm()));
        a.setKmOdServisu(0);
        a.setPosledniServis(datum);
        zapis("Servis " + a + ": " + (p.isEmpty() ? "bez popisu" : p) + ", " + Math.round(cena) + " Kč");
    }

    public List<Tankovani> getTankovani() {
        return new ArrayList<>(tankovaniKniha);
    }

    public List<Servis> getServisy() {
        return new ArrayList<>(servisKniha);
    }

    public List<Tankovani> getTankovani(Auto a) {
        List<Tankovani> v = new ArrayList<>();
        for (Tankovani t : tankovaniKniha) if (t.getAutoId() == a.getCarId()) v.add(t);
        return v;
    }

    public List<Servis> getServisy(Auto a) {
        List<Servis> v = new ArrayList<>();
        for (Servis s : servisKniha) if (s.getAutoId() == a.getCarId()) v.add(s);
        return v;
    }

    /** Náklady za palivo a servis v daném měsíci: {palivo, servis}. */
    public float[] getNakladyMesic(YearMonth mesic) {
        float palivo = 0, servis = 0;
        for (Tankovani t : tankovaniKniha) {
            if (YearMonth.from(t.getDatum()).equals(mesic)) palivo += t.getCena();
        }
        for (Servis s : servisKniha) {
            if (YearMonth.from(s.getDatum()).equals(mesic)) servis += s.getCena();
        }
        return new float[]{palivo, servis};
    }

    /** Celkové náklady jednoho auta: {palivo, servis}. */
    public float[] getNakladyAuta(Auto a) {
        float palivo = 0, servis = 0;
        for (Tankovani t : getTankovani(a)) palivo += t.getCena();
        for (Servis s : getServisy(a)) servis += s.getCena();
        return new float[]{palivo, servis};
    }

    /** Kolik Kč stojí ujetí 1 km (jen palivo, podle spotřeby a aktuální ceny). */
    public float getCenaZaKm(Auto a) {
        return a.getSpotreba() / 100f * getCenaPaliva(a);
    }

    /** Za servis utraceno v aktuálním roce. */
    public float getServisLetos() {
        int rok = Year.now().getValue();
        float soucet = 0;
        for (Servis s : servisKniha) if (s.getDatum().getYear() == rok) soucet += s.getCena();
        return soucet;
    }

    // ------------------------------------------------------------------
    // plán servisu do rozpočtu (odhad)
    // ------------------------------------------------------------------

    /** Odhad ceny příštího servisu v Kč (zaokrouhleno na stovky). Je to jen odhad, ne nabídka servisu. */
    public int odhadCenyServisu(Auto a) {
        int stari = a.getRokVyroby() > 0 ? Math.max(0, Year.now().getValue() - a.getRokVyroby()) : 10;
        float cena = 2800 + 190 * stari + 0.18f * a.getKmOdServisu();
        if (a.getDniDoStk() < 60) cena += 1600;            // k servisu se přidá STK a emise
        if (a.getOpotrebeniServisu() > 1) cena *= 1.15f;    // přetažený interval bývá dražší
        return Math.round(cena / 100f) * 100;
    }

    /** Jedna položka plánu: auto, odhad ceny, kolik je utraceno včetně ní a jestli se vejde. */
    public static class PlanPolozka {
        public final Auto auto;
        public final int cena;
        public final int kumulace;
        public final boolean vejdeSe;

        PlanPolozka(Auto auto, int cena, int kumulace, boolean vejdeSe) {
            this.auto = auto;
            this.cena = cena;
            this.kumulace = kumulace;
            this.vejdeSe = vejdeSe;
        }
    }

    /** Projde žebříček od nejvyšší priority a bere auta, dokud stačí zbytek rozpočtu. */
    public List<PlanPolozka> planRozpoctu(int rozpocet) {
        List<PlanPolozka> plan = new ArrayList<>();
        int utraceno = 0;
        for (Auto a : getZebricek()) {
            int cena = odhadCenyServisu(a);
            boolean vejde = utraceno + cena <= rozpocet;
            if (vejde) utraceno += cena;
            plan.add(new PlanPolozka(a, cena, utraceno, vejde));
        }
        return plan;
    }

    // ------------------------------------------------------------------
    // upozornění
    // ------------------------------------------------------------------

    public enum Zavaznost { KRITICKE, VAROVANI, INFO }

    public static class Upozorneni {
        public final Auto auto;
        public final Zavaznost zavaznost;
        public final String text;

        Upozorneni(Auto auto, Zavaznost zavaznost, String text) {
            this.auto = auto;
            this.zavaznost = zavaznost;
            this.text = text;
        }
    }

    /** Co je potřeba řešit: STK, servis, palivo, kritická priorita. Nejzávažnější nahoře. */
    public List<Upozorneni> getUpozorneni() {
        List<Upozorneni> v = new ArrayList<>();
        for (Auto a : getAuta()) {
            long dni = a.getDniDoStk();
            if (dni != Integer.MAX_VALUE) {
                if (dni < 0) v.add(new Upozorneni(a, Zavaznost.KRITICKE, "STK prošla před " + (-dni) + " dny"));
                else if (dni <= 30) v.add(new Upozorneni(a, Zavaznost.VAROVANI, "STK vyprší za " + dni + " dní"));
            }
            float km = a.getKmDoServisu();
            if (km < 0) {
                v.add(new Upozorneni(a, Zavaznost.KRITICKE, "Servis po termínu o " + Math.round(-km) + " km"));
            } else if (a.getOpotrebeniServisu() >= 0.9f) {
                v.add(new Upozorneni(a, Zavaznost.VAROVANI, "Servis za " + Math.round(km) + " km"));
            }
            if (a.getNaplneni() < 0.15f) {
                v.add(new Upozorneni(a, Zavaznost.INFO, "V nádrži jen " + Math.round(a.getNaplneni() * 100) + " %"));
            }
        }
        v.sort(Comparator.comparing(u -> u.zavaznost));
        return v;
    }

    // ------------------------------------------------------------------
    // simulace odstávky auta
    // ------------------------------------------------------------------

    public static class Simulace {
        public final Auto odstavene;
        public final Auto nahrada;          // null = není čím nahradit
        public final int dni;
        public final int kmCelkem;
        public final float palivoPuvodni;   // Kč, kdyby jezdilo původní auto
        public final float palivoNahrada;   // Kč s náhradou
        public final int tankovaniNahrady;
        public final int servisNahradyZa;   // za kolik dní by náhrada potřebovala servis (-1 = nestane se)

        Simulace(Auto odstavene, Auto nahrada, int dni, int kmCelkem, float palivoPuvodni, float palivoNahrada,
                 int tankovaniNahrady, int servisNahradyZa) {
            this.odstavene = odstavene;
            this.nahrada = nahrada;
            this.dni = dni;
            this.kmCelkem = kmCelkem;
            this.palivoPuvodni = palivoPuvodni;
            this.palivoNahrada = palivoNahrada;
            this.tankovaniNahrady = tankovaniNahrady;
            this.servisNahradyZa = servisNahradyZa;
        }

        public float getRozdil() {
            return palivoNahrada - palivoPuvodni;
        }
    }

    /**
     * Co když auto na pár dní odstavíme (servis, nehoda)? Jeho jízdy převezme auto,
     * kterému to vyjde nejlevněji a samo nemá kritickou prioritu. Je to simulace, ne předpověď.
     */
    public Simulace simulujOdstavku(Auto a, int dni, int kmDenne) {
        int km = dni * kmDenne;
        float puvodni = km * getCenaZaKm(a);
        Auto nejlepsi = null;
        float nejlevnejsi = Float.MAX_VALUE;
        for (Auto x : getAuta()) {
            if (x == a) continue;
            if (getPriorita(x) == Auto.Priorita.KRITICKA) continue;   // auto, které samo potřebuje do servisu, nebereme
            float cena = km * getCenaZaKm(x);
            if (cena < nejlevnejsi) {
                nejlevnejsi = cena;
                nejlepsi = x;
            }
        }
        if (nejlepsi == null) return new Simulace(a, null, dni, km, puvodni, 0, 0, -1);
        float litry = nejlepsi.getSpotreba() * km / 100f;
        int tank = Math.max(0, nejlepsi.fuelingCount(litry));
        int servisZa = -1;
        if (nejlepsi.getKmDoServisu() < km && kmDenne > 0) {
            servisZa = (int) Math.max(0, nejlepsi.getKmDoServisu() / kmDenne);
        }
        return new Simulace(a, nejlepsi, dni, km, puvodni, nejlevnejsi, tank, servisZa);
    }

    // ------------------------------------------------------------------
    // export do CSV (s popisem sloupců v hlavičce)
    // ------------------------------------------------------------------

    private static String csv(String text) {
        String t = text == null ? "" : text;
        // ochrana proti vzorcům v Excelu: text začínající = + - @ by se spustil jako vzorec
        if (!t.isEmpty() && "=+-@".indexOf(t.charAt(0)) >= 0) {
            t = "'" + t;
        }
        if (t.contains(";") || t.contains("\"") || t.contains("\n") || t.contains("\r")) {
            t = "\"" + t.replace("\"", "\"\"") + "\"";
        }
        return t;
    }

    private static String cislo(float x) {
        return String.format(CS, "%.1f", x);
    }

    /** Celá garáž jako CSV text (středník, desetinná čárka). Řádky začínající # jsou popis. */
    public String exportCsv() {
        StringBuilder sb = new StringBuilder();
        String nl = "\r\n";
        sb.append("# Export garáže ").append(garazId).append(nl);
        sb.append("# Vytvořeno: ").append(LocalDateTime.now().format(DTF)).append(nl);
        sb.append("# Formát: oddělovač středník, desetinná čárka, kódování UTF-8").append(nl);
        sb.append("# Řádky začínající # jsou popis a dají se smazat").append(nl);
        sb.append("# poradi = pořadí podle priority údržby (1 = řešit jako první)").append(nl);
        sb.append("# spotreba_l_100km, kapacita_l, palivo_l = údaje auta, najeto_km = km ujeté na výletech v aplikaci").append(nl);
        sb.append("# skore = priorita 0-100, součet bodů ze šesti faktorů podle vah").append(nl);
        StringBuilder vahyText = new StringBuilder();
        for (Faktor f : Faktor.values()) {
            if (vahyText.length() > 0) vahyText.append(", ");
            vahyText.append(f.getNazev()).append(" ").append(getVaha(f));
        }
        sb.append("# váhy faktorů: ").append(vahyText).append(" (přepočítají se na 100 %)").append(nl);
        for (Faktor f : Faktor.values()) {
            sb.append("# faktor ").append(f.getNazev()).append(": ").append(f.getPopis()).append(nl);
        }
        sb.append("# trida_model = třída podle skóre (Kritická od ").append((int) PRAH_KRITICKA)
                .append(", Vysoká od ").append((int) PRAH_VYSOKA)
                .append(", Střední od ").append((int) PRAH_STREDNI).append(", jinak Nízká)").append(nl);
        sb.append("# trida_platna = třída po případném ručním připnutí, duvod_pripnuti = nepovinný důvod").append(nl);
        sb.append("# km_od_servisu, servisni_interval_km, stk_do (RRRR-MM-DD), odhad_servisu_kc = odhad, ne nabídka").append(nl);
        sb.append("poradi;id;jmeno;znacka;spz;rok_vyroby;palivo_druh;spotreba_l_100km;kapacita_l;palivo_l;najeto_km;")
                .append("km_od_servisu;servisni_interval_km;stk_do;odhad_servisu_kc;")
                .append("skore;trida_model;trida_platna;pripnuto;duvod_pripnuti").append(nl);

        int poradi = 1;
        for (Auto a : getZebricek()) {
            sb.append(poradi++).append(';')
                    .append(a.getCarId()).append(';')
                    .append(csv(a.getJmenoAuta())).append(';')
                    .append(csv(a.getZnacka())).append(';')
                    .append(csv(a.getSpz())).append(';')
                    .append(a.getRokVyroby()).append(';')
                    .append(a.getDruhPaliva().getNazev()).append(';')
                    .append(cislo(a.getSpotreba())).append(';')
                    .append(cislo(a.getMaxPalivo())).append(';')
                    .append(cislo(a.getPalivo())).append(';')
                    .append(Math.round(a.getUjeteKm())).append(';')
                    .append(Math.round(a.getKmOdServisu())).append(';')
                    .append(a.getServisniInterval()).append(';')
                    .append(a.getStkDo() == null ? "" : a.getStkDo().toString()).append(';')
                    .append(odhadCenyServisu(a)).append(';')
                    .append(cislo(getSkore(a))).append(';')
                    .append(getModelovaPriorita(a)).append(';')
                    .append(getPriorita(a)).append(';')
                    .append(a.jePripnuta() ? "ano" : "ne").append(';')
                    .append(csv(a.getDuvodPripnuti())).append(nl);
        }
        return sb.toString();
    }
}
