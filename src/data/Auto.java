package data;

import java.time.LocalDate;

public class Auto {

    /** Třída priority údržby auta. Pořadí v enumu = pořadí důležitosti (Kritická je první). */
    public enum Priorita {
        KRITICKA("Kritická"),
        VYSOKA("Vysoká"),
        STREDNI("Střední"),
        NIZKA("Nízká");

        private final String nazev;

        Priorita(String nazev) {
            this.nazev = nazev;
        }

        public String getNazev() {
            return nazev;
        }

        @Override
        public String toString() {
            return nazev;
        }
    }

    /** Druh paliva, podle něj se počítá cena za litr. */
    public enum Palivo {
        BENZIN("Benzín", 1.00f),
        NAFTA("Nafta", 0.96f),
        LPG("LPG", 0.48f),
        HYBRID("Hybrid", 1.00f);

        private final String nazev;
        private final float koeficientCeny;   // násobek základní ceny paliva

        Palivo(String nazev, float koeficientCeny) {
            this.nazev = nazev;
            this.koeficientCeny = koeficientCeny;
        }

        public String getNazev() {
            return nazev;
        }

        public float getKoeficientCeny() {
            return koeficientCeny;
        }

        @Override
        public String toString() {
            return nazev;
        }
    }

    private int carId; private static int lastCarId = 0;
    private float spotreba;
    private float maxPalivo;
    private float palivo;
    private String jmenoAuta = "";
    private String znacka = "";
    private String spz = "";
    private int rokVyroby;
    private float ujeteKm;   // kolik km auto celkem najelo na výletech

    private Priorita pripnuta;            // ručně připnutá priorita (null = nepřipnuto, platí modelová)
    private String duvodPripnuti = "";    // nepovinný důvod ručního připnutí

    // ---- rozšíření: servis, STK, barva, palivo ----
    private int barva = -1;                    // barva laku pro 3D model (-1 = automatická)
    private float kmOdServisu = 0;             // km od posledního servisu
    private int servisniInterval = 15000;      // po kolika km je potřeba servis
    private LocalDate stkDo;                   // platnost technické kontroly (null = neznámá)
    private LocalDate posledniServis;          // datum posledního servisu (null = neznámé)
    private Palivo druhPaliva = Palivo.BENZIN;
    private String poznamka = "";

    public Auto() {
        carId = ++lastCarId;
    }

    public Auto(float spotreba, float maxPalivo, float palivo, String jmenoAuta) {
        this();
        this.spotreba = spotreba;
        this.maxPalivo = maxPalivo;
        this.palivo = palivo;
        this.jmenoAuta = jmenoAuta;
    }

    public Auto(float spotreba, float maxPalivo, float palivo, String jmenoAuta,
                String znacka, String spz, int rokVyroby) {
        this(spotreba, maxPalivo, palivo, jmenoAuta);
        this.znacka = znacka;
        this.spz = spz;
        this.rokVyroby = rokVyroby;
    }

    /** Při načítání ze souboru: auto dostane svoje původní ID a čítač se posune, ať se ID neopakují. */
    void obnovId(int id) {
        this.carId = id;
        if (id > lastCarId) lastCarId = id;
    }

    void obnovUjeteKm(float km) {
        this.ujeteKm = km;
    }

    public int getCarId() {
        return carId;
    }

    public static int getNewCarId() {
        return lastCarId;
    }

    public String getJmenoAuta() {
        return jmenoAuta;
    }

    public void setJmenoAuta(String jmenoAuta) {
        this.jmenoAuta = jmenoAuta;
    }

    public String getZnacka() {
        return znacka;
    }

    public void setZnacka(String znacka) {
        this.znacka = znacka;
    }

    public String getSpz() {
        return spz;
    }

    public void setSpz(String spz) {
        this.spz = spz;
    }

    public int getRokVyroby() {
        return rokVyroby;
    }

    public void setRokVyroby(int rokVyroby) {
        this.rokVyroby = rokVyroby;
    }

    public float getUjeteKm() {
        return ujeteKm;
    }

    public float getMaxPalivo() {
        return maxPalivo;
    }

    public void setMaxPalivo(float maxPalivo) {
        this.maxPalivo = maxPalivo;
    }

    public float getSpotreba() {
        return spotreba;
    }

    public void setSpotreba(float spotreba) {
        this.spotreba = spotreba;
    }

    public float getPalivo() {
        return palivo;
    }

    public void setPalivo(float palivo) {
        this.palivo = palivo;
    }

    public int getBarva() {
        return barva;
    }

    public void setBarva(int barva) {
        this.barva = barva;
    }

    public float getKmOdServisu() {
        return kmOdServisu;
    }

    public void setKmOdServisu(float kmOdServisu) {
        this.kmOdServisu = Math.max(0, kmOdServisu);
    }

    public int getServisniInterval() {
        return servisniInterval;
    }

    public void setServisniInterval(int servisniInterval) {
        this.servisniInterval = Math.max(1000, servisniInterval);
    }

    public LocalDate getStkDo() {
        return stkDo;
    }

    public void setStkDo(LocalDate stkDo) {
        this.stkDo = stkDo;
    }

    public LocalDate getPosledniServis() {
        return posledniServis;
    }

    public void setPosledniServis(LocalDate posledniServis) {
        this.posledniServis = posledniServis;
    }

    public Palivo getDruhPaliva() {
        return druhPaliva;
    }

    public void setDruhPaliva(Palivo druhPaliva) {
        this.druhPaliva = druhPaliva == null ? Palivo.BENZIN : druhPaliva;
    }

    public String getPoznamka() {
        return poznamka;
    }

    public void setPoznamka(String poznamka) {
        this.poznamka = poznamka == null ? "" : poznamka.trim();
    }

    /** Kolik procent servisního intervalu už auto ujelo (0 až 1, může být i víc). */
    public float getOpotrebeniServisu() {
        return servisniInterval <= 0 ? 0 : kmOdServisu / servisniInterval;
    }

    /** Kolik km zbývá do servisu (záporné číslo = servis je po termínu). */
    public float getKmDoServisu() {
        return servisniInterval - kmOdServisu;
    }

    /** Za kolik dní vyprší STK (záporné = prošlá). Integer.MAX_VALUE, když datum neznáme. */
    public long getDniDoStk() {
        if (stkDo == null) return Integer.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), stkDo);
    }

    // ------------------------------------------------------------------
    // ručně připnutá priorita
    // ------------------------------------------------------------------

    public boolean jePripnuta() {
        return pripnuta != null;
    }

    public Priorita getPripnuta() {
        return pripnuta;
    }

    public String getDuvodPripnuti() {
        return duvodPripnuti;
    }

    /** Připne prioritu ručně (přebije modelovou). Důvod je nepovinný. */
    public void pripni(Priorita priorita, String duvod) {
        this.pripnuta = priorita;
        this.duvodPripnuti = (duvod == null) ? "" : duvod.trim();
    }

    public void odepni() {
        this.pripnuta = null;
        this.duvodPripnuti = "";
    }

    /** Kolik km ještě ujedu na aktuální nádrž. */
    public float getDojezd() {
        if (spotreba <= 0) return 0;
        return palivo / spotreba * 100f;
    }

    /** Naplnění nádrže od 0 do 1. */
    public float getNaplneni() {
        if (maxPalivo <= 0) return 0;
        return Math.max(0f, Math.min(1f, palivo / maxPalivo));
    }

    /** Jízda jedním směrem: započítá km a odečte palivo (po cestě se dotankuje, kolikrát je potřeba). */
    public void jizda(int km) {
        ujeteKm += km;
        kmOdServisu += km;
        float palivaPouzito = spotreba * km / 100;
        int pocetTankovani = Math.max(0, (int) Math.ceil((palivaPouzito - palivo) / maxPalivo));
        palivo = palivo + (pocetTankovani * maxPalivo) - palivaPouzito;
    }

    public void vylet(int length) {
        length *= 2;
        ujeteKm += length;   // započítáme ujetou vzdálenost (tam a zpět)
        kmOdServisu += length;
        float palivaPouzito = spotreba * length / 100;
        // AI
        int pocetTankovani = (int) Math.ceil((palivaPouzito - palivo) / maxPalivo);
        palivo = palivo + (pocetTankovani * maxPalivo) - palivaPouzito;
        // konec AI
    }

    public float vyletTest(int length, boolean canFuel) {
        length *= 2;
        float palivaPouzito = spotreba * length / 100;
        if (!canFuel && palivaPouzito > palivo) return -1;
        // AI
        int pocetTankovani = (int) Math.ceil((palivaPouzito - palivo) / maxPalivo);
        return palivo + (pocetTankovani * maxPalivo) - palivaPouzito;
        // konec AI
    }

    public int fuelingCount(float fuel) {
        return (int) Math.ceil((fuel - palivo) / maxPalivo);
    }

    @Override
    public String toString() {
        return jmenoAuta + " (" + spz + ")";
    }
}
