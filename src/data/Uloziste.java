package data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Uložení garáže do textového souboru (UTF-8, jeden záznam na řádek, pole oddělená tabulátorem).
 * Soubor je čitelný i v Poznámkovém bloku. Tabulátor a konec řádku v textu se escapují.
 */
public final class Uloziste {

    private static final String VERZE = "GARAZ\t2";

    private Uloziste() {
    }

    /** Výchozí soubor: složka .garaz v domovském adresáři uživatele. */
    public static Path vychoziSoubor() {
        return Paths.get(System.getProperty("user.home"), ".garaz", "garaz.tsv");
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "");
    }

    private static String unesc(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char d = s.charAt(++i);
                sb.append(d == 't' ? '\t' : d == 'n' ? '\n' : d);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String f(float x) {
        return String.format(Locale.ROOT, "%.3f", x);
    }

    private static String d(LocalDate d) {
        return d == null ? "" : d.toString();
    }

    private static LocalDate pd(String s) {
        return s == null || s.isEmpty() ? null : LocalDate.parse(s);
    }

    public static void uloz(Garaz g, Path soubor) throws IOException {
        List<String> r = new ArrayList<>();
        r.add(VERZE);
        StringBuilder vahy = new StringBuilder();
        for (Garaz.Faktor f : Garaz.Faktor.values()) {
            vahy.append('\t').append(f.name()).append('=').append(g.getVaha(f));
        }
        r.add("NASTAVENI\t" + f(g.getCenaPaliva()) + "\t" + g.getRocniRozpocet() + vahy);
        r.add("STAT\t" + f(g.getCelkemKm()) + "\t" + g.getPocetVyletu() + "\t" + g.getPocetTankovani()
                + "\t" + f(g.getNejdelsiVylet()));
        for (Garaz.Uspech u : Garaz.Uspech.values()) {
            if (g.jeOdemcen(u)) r.add("USPECH\t" + u.name());
        }
        for (Auto a : g.getAuta()) {
            r.add(String.join("\t", "AUTO",
                    String.valueOf(a.getCarId()), esc(a.getJmenoAuta()), esc(a.getZnacka()), esc(a.getSpz()),
                    String.valueOf(a.getRokVyroby()), f(a.getSpotreba()), f(a.getMaxPalivo()), f(a.getPalivo()),
                    f(a.getUjeteKm()), String.valueOf(a.getBarva()), f(a.getKmOdServisu()),
                    String.valueOf(a.getServisniInterval()), d(a.getStkDo()), d(a.getPosledniServis()),
                    a.getDruhPaliva().name(), a.jePripnuta() ? a.getPripnuta().name() : "",
                    esc(a.getDuvodPripnuti()), esc(a.getPoznamka())));
        }
        for (Garaz.Tankovani t : g.getTankovani()) {
            r.add("TANK\t" + t.getAutoId() + "\t" + t.getDatum() + "\t" + f(t.getLitry()) + "\t" + f(t.getCenaZaLitr()));
        }
        for (Garaz.Servis s : g.getServisy()) {
            r.add("SERVIS\t" + s.getAutoId() + "\t" + s.getDatum() + "\t" + f(s.getCena()) + "\t" + f(s.getKm())
                    + "\t" + esc(s.getPopis()));
        }
        for (Garaz.Zaznam z : g.getHistorie()) {
            r.add("ZAZNAM\t" + z.getCas() + "\t" + esc(z.getKdo()) + "\t" + esc(z.getText()));
        }
        Files.createDirectories(soubor.getParent());
        Path tmp = soubor.resolveSibling(soubor.getFileName() + ".tmp");
        Files.write(tmp, r, StandardCharsets.UTF_8);
        Files.move(tmp, soubor, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /** Načte garáž ze souboru. Neznámé nebo poškozené řádky přeskočí. Vrací počet načtených aut. */
    public static int nacti(Garaz g, Path soubor) throws IOException {
        List<String> radky = Files.readAllLines(soubor, StandardCharsets.UTF_8);
        if (radky.isEmpty() || !radky.get(0).startsWith("GARAZ")) {
            throw new IOException("Soubor není uložená garáž");
        }
        g.vymazVse();
        int aut = 0;
        for (String radek : radky) {
            String[] p = radek.split("\t", -1);
            try {
                switch (p[0]) {
                    case "NASTAVENI":
                        g.setCenaPaliva(Float.parseFloat(p[1]));
                        g.setRocniRozpocet(Integer.parseInt(p[2]));
                        for (int i = 3; i < p.length; i++) {
                            String[] kv = p[i].split("=");
                            g.setVaha(Garaz.Faktor.valueOf(kv[0]), Integer.parseInt(kv[1]));
                        }
                        break;
                    case "STAT":
                        g.obnovStatistiky(Float.parseFloat(p[1]), Integer.parseInt(p[2]),
                                Integer.parseInt(p[3]), Float.parseFloat(p[4]));
                        break;
                    case "USPECH":
                        g.obnovUspech(Garaz.Uspech.valueOf(p[1]));
                        break;
                    case "AUTO": {
                        Auto a = new Auto(Float.parseFloat(p[6]), Float.parseFloat(p[7]), Float.parseFloat(p[8]),
                                unesc(p[2]), unesc(p[3]), unesc(p[4]), Integer.parseInt(p[5]));
                        a.obnovId(Integer.parseInt(p[1]));
                        a.obnovUjeteKm(Float.parseFloat(p[9]));
                        a.setBarva(Integer.parseInt(p[10]));
                        a.setKmOdServisu(Float.parseFloat(p[11]));
                        a.setServisniInterval(Integer.parseInt(p[12]));
                        a.setStkDo(pd(p[13]));
                        a.setPosledniServis(pd(p[14]));
                        a.setDruhPaliva(Auto.Palivo.valueOf(p[15]));
                        if (!p[16].isEmpty()) a.pripni(Auto.Priorita.valueOf(p[16]), unesc(p[17]));
                        a.setPoznamka(unesc(p[18]));
                        if (g.addCar(a)) aut++;
                        break;
                    }
                    case "TANK":
                        g.obnovTankovani(new Garaz.Tankovani(Integer.parseInt(p[1]), LocalDate.parse(p[2]),
                                Float.parseFloat(p[3]), Float.parseFloat(p[4])));
                        break;
                    case "SERVIS":
                        g.obnovServis(new Garaz.Servis(Integer.parseInt(p[1]), LocalDate.parse(p[2]),
                                unesc(p[5]), Float.parseFloat(p[3]), Float.parseFloat(p[4])));
                        break;
                    case "ZAZNAM":
                        g.obnovZaznam(LocalDateTime.parse(p[1]), unesc(p[2]), unesc(p[3]));
                        break;
                    default:
                        break;
                }
            } catch (RuntimeException ex) {
                // poškozený řádek přeskočíme, zbytek garáže se načte
            }
        }
        return aut;
    }

    // ------------------------------------------------------------------
    // ukázková data
    // ------------------------------------------------------------------

    /** Naplní garáž ukázkovými auty a historií tankování a servisů za poslední rok. */
    public static void ukazkovaData(Garaz g) {
        g.vymazVse();
        Random r = new Random(7);
        LocalDate dnes = LocalDate.now();
        Object[][] auta = {
                // jméno, značka, SPZ, rok, spotřeba, nádrž, palivo, km od servisu, interval, STK za dní, druh
                {"Octavia Combi", "Škoda", "5H24812", 2017, 6.1f, 50f, 31f, 13900f, 15000, 21, Auto.Palivo.NAFTA},
                {"Fabia", "Škoda", "1AB2345", 2012, 5.8f, 45f, 6f, 9200f, 15000, 190, Auto.Palivo.BENZIN},
                {"Golf GTI", "Volkswagen", "2BC9911", 2019, 7.9f, 50f, 38f, 4100f, 20000, 410, Auto.Palivo.BENZIN},
                {"Felicia", "Škoda", "3K10203", 1996, 7.2f, 40f, 22f, 16800f, 10000, -12, Auto.Palivo.BENZIN},
                {"Transit", "Ford", "4H77001", 2015, 9.6f, 80f, 64f, 11200f, 15000, 65, Auto.Palivo.NAFTA},
                {"Yaris Hybrid", "Toyota", "6A55120", 2021, 4.1f, 36f, 29f, 2600f, 15000, 520, Auto.Palivo.HYBRID},
                {"MX-5 Roadster", "Mazda", "7AC0042", 2008, 7.4f, 45f, 9f, 900f, 15000, 300, Auto.Palivo.BENZIN},
                {"Superb", "Škoda", "8M00717", 2014, 6.8f, 66f, 12f, 14600f, 15000, 8, Auto.Palivo.LPG},
        };
        for (Object[] x : auta) {
            Auto a = new Auto((Float) x[4], (Float) x[5], (Float) x[6], (String) x[0],
                    (String) x[1], (String) x[2], (Integer) x[3]);
            a.setKmOdServisu((Float) x[7]);
            a.setServisniInterval((Integer) x[8]);
            a.setStkDo(dnes.plusDays((Integer) x[9]));
            a.setDruhPaliva((Auto.Palivo) x[10]);
            a.obnovUjeteKm(800 + r.nextInt(4200));
            g.addCar(a);
        }
        // tankování a servisy za posledních 12 měsíců, ať mají grafy co ukázat
        float km = 0;
        for (int m = 11; m >= 0; m--) {
            LocalDate mesic = dnes.minusMonths(m).withDayOfMonth(1);
            for (Auto a : g.getAuta()) {
                int kolik = 1 + r.nextInt(3);
                for (int i = 0; i < kolik; i++) {
                    LocalDate den = mesic.plusDays(r.nextInt(27));
                    if (den.isAfter(dnes)) den = dnes;
                    float litry = a.getMaxPalivo() * (0.35f + r.nextFloat() * 0.5f);
                    float cena = (36.5f + r.nextFloat() * 4.5f) * a.getDruhPaliva().getKoeficientCeny();
                    g.obnovTankovani(new Garaz.Tankovani(a.getCarId(), den, litry, cena));
                    km += litry / a.getSpotreba() * 100;
                }
            }
            if (m % 3 == 1) {
                Auto a = g.getAuta().get(r.nextInt(g.getPocetAut()));
                String[] prace = {"Výměna oleje a filtrů", "Brzdové destičky", "Pneumatiky zima", "Velký servis",
                        "Rozvody a vodní pumpa", "Klimatizace"};
                g.obnovServis(new Garaz.Servis(a.getCarId(), mesic.plusDays(3 + r.nextInt(20)),
                        prace[r.nextInt(prace.length)], 2500 + r.nextInt(14) * 500, a.getUjeteKm()));
            }
        }
        g.getAuta().get(3).setPoznamka("Veterán, jezdí jen o víkendu. Prošlá STK, čeká na termín.");
        g.pripniPrioritu(g.getAuta().get(4), Auto.Priorita.KRITICKA, "Firemní dodávka, nesmí vypadnout");
        g.obnovStatistiky(km, 46, g.getTankovani().size(), 612);
        // ukázková historie změn za poslední dny
        String[] udalosti = {"Přidáno auto Octavia Combi (5H24812)", "Přidáno auto Fabia (1AB2345)",
                "Natankováno: Golf GTI (2BC9911), 41,2 l za 1 603 Kč", "Servis Fabia (1AB2345): Brzdové destičky, 4 500 Kč",
                "Výlet Superb (8M00717): 412 km tam a zpět, tankování po cestě 0×", "Váhy priority: předvolba Vyrovnaná",
                "Natankováno: Transit (4H77001), 58,0 l za 2 166 Kč", "Upravena poznámka u auta Felicia (3K10203)",
                "Cesta MX-5 Roadster (7AC0042): Praha → Český Krumlov, 172 km", "Odemčen úspěch Sběratel",
                "Roční rozpočet na servis změněn na 60 000 Kč", "Natankováno: Yaris Hybrid (6A55120), 22,4 l za 871 Kč"};
        java.time.LocalDateTime cas = java.time.LocalDateTime.now().minusDays(9).withHour(8);
        for (String u : udalosti) {
            cas = cas.plusHours(9 + r.nextInt(12)).plusMinutes(r.nextInt(60));
            g.obnovZaznam(cas, System.getProperty("user.name", "uživatel"), u);
        }
        g.zapis("Načtena ukázková data (8 aut, rok tankování a servisů)");
        g.zkontrolujUspechy();
    }
}
