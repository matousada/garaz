package gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/** Vlastní přibližná mapa ČR: obrys státu, města a hlavní silnice. Nepotřebuje internet. */
final class MapaData {

    static final class Mesto {
        final int id;
        final String nazev;
        final double lon, lat;
        final int velikost;   // 2 = velké město, 1 = střední, 0 = menší

        Mesto(int id, String nazev, double lon, double lat, int velikost) {
            this.id = id;
            this.nazev = nazev;
            this.lon = lon;
            this.lat = lat;
            this.velikost = velikost;
        }

        @Override
        public String toString() {
            return nazev;
        }
    }

    static final class Hrana {
        final int a, b;
        final boolean dalnice;
        final double km;

        Hrana(int a, int b, boolean dalnice, double km) {
            this.a = a;
            this.b = b;
            this.dalnice = dalnice;
            this.km = km;
        }

        double hodiny() {
            return km / (dalnice ? 105.0 : 62.0);
        }
    }

    static final class Trasa {
        final List<Integer> uzly = new ArrayList<>();
        double km, hodiny, kmDalnice;
    }

    /** Obrys ČR: {zeměpisná délka, šířka}, po směru hodinových ručiček od Aše. */
    static final double[][] HRANICE = {
            {12.09, 50.25}, {12.25, 50.33}, {12.50, 50.40}, {12.75, 50.41}, {12.95, 50.42}, {13.20, 50.50},
            {13.50, 50.62}, {13.75, 50.73}, {13.95, 50.80}, {14.22, 50.88}, {14.33, 50.97}, {14.50, 51.05},
            {14.72, 50.97}, {14.85, 50.87}, {14.99, 51.03}, {15.28, 50.99}, {15.40, 50.78}, {15.60, 50.76},
            {15.74, 50.74}, {16.00, 50.65}, {16.25, 50.58}, {16.40, 50.55}, {16.55, 50.37}, {16.65, 50.20},
            {16.78, 50.12}, {16.88, 50.21}, {17.05, 50.25}, {17.20, 50.40}, {17.35, 50.32}, {17.62, 50.28},
            {17.72, 50.27}, {17.75, 50.10}, {18.05, 50.07}, {18.20, 49.98}, {18.30, 49.92}, {18.55, 49.95},
            {18.62, 49.75}, {18.85, 49.67}, {18.85, 49.52}, {18.60, 49.42}, {18.30, 49.40}, {18.10, 49.28},
            {17.80, 49.10}, {17.55, 49.02}, {17.40, 48.90}, {17.15, 48.85}, {17.05, 48.78}, {16.95, 48.62},
            {16.75, 48.72}, {16.50, 48.80}, {16.10, 48.76}, {15.80, 48.88}, {15.40, 48.98}, {15.05, 48.98},
            {14.80, 48.78}, {14.65, 48.72}, {14.45, 48.63}, {14.30, 48.58}, {14.00, 48.68}, {13.75, 48.80},
            {13.60, 48.95}, {13.35, 49.05}, {13.20, 49.15}, {13.00, 49.30}, {12.85, 49.38}, {12.75, 49.55},
            {12.55, 49.70}, {12.45, 49.95}, {12.40, 50.05}, {12.28, 50.18}
    };

    private static final String[] MESTA_DATA = {
            "Praha|14.42|50.08|2", "Brno|16.61|49.20|2", "Ostrava|18.28|49.83|2", "Plzeň|13.38|49.75|2",
            "Liberec|15.06|50.77|1", "Olomouc|17.25|49.59|1", "České Budějovice|14.47|48.97|1",
            "Hradec Králové|15.83|50.21|1", "Ústí nad Labem|14.04|50.66|1", "Pardubice|15.78|50.04|1",
            "Zlín|17.67|49.22|1", "Jihlava|15.59|49.40|1", "Karlovy Vary|12.87|50.23|1", "Opava|17.91|49.94|1",
            "Frýdek-Místek|18.35|49.69|1", "Teplice|13.84|50.64|1",
            "Havlíčkův Brod|15.58|49.61|0", "Kladno|14.10|50.15|0", "Most|13.64|50.50|0", "Děčín|14.21|50.78|0",
            "Karviná|18.54|49.86|0", "Chomutov|13.42|50.46|0", "Jablonec nad Nisou|15.17|50.73|0",
            "Mladá Boleslav|14.90|50.41|0", "Prostějov|17.11|49.47|0", "Přerov|17.45|49.46|0",
            "Třebíč|15.88|49.21|0", "Třinec|18.67|49.68|0", "Tábor|14.66|49.41|0", "Znojmo|16.05|48.86|0",
            "Příbram|14.01|49.69|0", "Cheb|12.37|50.08|0", "Kolín|15.20|50.03|0", "Trutnov|15.91|50.56|0",
            "Písek|14.15|49.31|0", "Kroměříž|17.39|49.30|0", "Šumperk|17.07|49.97|0", "Vsetín|17.99|49.34|0",
            "Uherské Hradiště|17.46|49.07|0", "Břeclav|16.88|48.76|0", "Hodonín|17.13|48.85|0",
            "Český Těšín|18.62|49.75|0", "Litoměřice|14.13|50.54|0", "Klatovy|13.29|49.40|0",
            "Sokolov|12.64|50.18|0", "Jičín|15.35|50.44|0", "Náchod|16.16|50.42|0",
            "Ústí nad Orlicí|16.39|49.97|0", "Svitavy|16.47|49.76|0", "Žďár nad Sázavou|15.94|49.56|0",
            "Pelhřimov|15.22|49.43|0", "Strakonice|13.90|49.26|0", "Prachatice|14.00|49.01|0",
            "Český Krumlov|14.32|48.81|0", "Jindřichův Hradec|15.00|49.14|0", "Domažlice|12.93|49.44|0",
            "Tachov|12.63|49.80|0", "Rokycany|13.59|49.74|0", "Beroun|14.07|49.96|0", "Mělník|14.47|50.35|0",
            "Nymburk|15.04|50.19|0", "Chrudim|15.79|49.95|0", "Blansko|16.64|49.36|0", "Vyškov|17.00|49.28|0",
            "Kyjov|17.12|49.01|0", "Jeseník|17.20|50.23|0", "Krnov|17.70|50.09|0", "Bruntál|17.46|49.99|0",
            "Nový Jičín|18.01|49.59|0", "Havířov|18.43|49.78|0", "Česká Lípa|14.54|50.69|0",
            "Rumburk|14.56|50.95|0", "Semily|15.33|50.60|0", "Louny|13.80|50.36|0", "Rakovník|13.73|50.10|0",
            "Benešov|14.69|49.78|0", "Kutná Hora|15.27|49.95|0", "Humpolec|15.36|49.54|0",
            "Velké Meziříčí|16.02|49.36|0", "Mohelnice|16.92|49.78|0", "Turnov|15.16|50.59|0",
            "Valašské Meziříčí|17.97|49.47|0"
    };

    /** Silnice: první prvek je typ (D = dálnice, R = silnice I. třídy), pak města za sebou. */
    private static final String[][] SILNICE = {
            {"D", "Praha", "Benešov", "Humpolec", "Jihlava", "Velké Meziříčí", "Brno", "Vyškov", "Přerov",
                    "Nový Jičín", "Ostrava", "Karviná"},
            {"D", "Brno", "Břeclav"},
            {"D", "Praha", "Tábor", "České Budějovice"},
            {"D", "Praha", "Beroun", "Rokycany", "Plzeň", "Tachov"},
            {"D", "Praha", "Kladno", "Rakovník", "Karlovy Vary", "Sokolov", "Cheb"},
            {"D", "Praha", "Louny", "Chomutov"},
            {"D", "Praha", "Litoměřice", "Ústí nad Labem", "Teplice"},
            {"D", "Praha", "Mladá Boleslav", "Turnov"},
            {"D", "Praha", "Nymburk", "Hradec Králové"},
            {"D", "Mohelnice", "Olomouc", "Přerov"},
            {"D", "Vyškov", "Prostějov", "Olomouc"},
            {"D", "Český Těšín", "Frýdek-Místek", "Nový Jičín"},
            {"D", "Přerov", "Kroměříž", "Uherské Hradiště", "Hodonín", "Břeclav"},
            {"R", "Praha", "Kolín", "Pardubice"},
            {"R", "Kolín", "Kutná Hora", "Havlíčkův Brod", "Jihlava"},
            {"R", "Kolín", "Nymburk"},
            {"R", "Pardubice", "Hradec Králové"},
            {"R", "Pardubice", "Chrudim", "Havlíčkův Brod"},
            {"R", "Brno", "Blansko", "Svitavy", "Ústí nad Orlicí", "Hradec Králové"},
            {"R", "Svitavy", "Mohelnice", "Šumperk", "Jeseník"},
            {"R", "Olomouc", "Bruntál", "Krnov", "Opava", "Ostrava"},
            {"R", "Ostrava", "Frýdek-Místek"},
            {"R", "Ostrava", "Havířov", "Karviná"},
            {"R", "Frýdek-Místek", "Třinec", "Český Těšín"},
            {"R", "Nový Jičín", "Valašské Meziříčí", "Vsetín", "Zlín"},
            {"R", "Přerov", "Valašské Meziříčí"},
            {"R", "Zlín", "Kroměříž"},
            {"R", "Zlín", "Uherské Hradiště"},
            {"R", "Brno", "Znojmo"},
            {"R", "Jihlava", "Třebíč", "Znojmo"},
            {"R", "Brno", "Třebíč", "Jindřichův Hradec", "České Budějovice"},
            {"R", "Brno", "Žďár nad Sázavou", "Havlíčkův Brod"},
            {"R", "Tábor", "Pelhřimov", "Havlíčkův Brod"},
            {"R", "Humpolec", "Pelhřimov"},
            {"R", "České Budějovice", "Prachatice"},
            {"R", "České Budějovice", "Český Krumlov"},
            {"R", "České Budějovice", "Písek", "Strakonice", "Klatovy", "Domažlice"},
            {"R", "Strakonice", "Prachatice"},
            {"R", "Plzeň", "Písek"},
            {"R", "Plzeň", "Klatovy"},
            {"R", "Praha", "Příbram", "Písek"},
            {"R", "Tachov", "Cheb"},
            {"R", "Plzeň", "Karlovy Vary"},
            {"R", "Chomutov", "Karlovy Vary"},
            {"R", "Chomutov", "Most", "Teplice"},
            {"R", "Louny", "Most"},
            {"R", "Ústí nad Labem", "Děčín", "Česká Lípa", "Liberec"},
            {"R", "Děčín", "Rumburk", "Česká Lípa"},
            {"R", "Liberec", "Jablonec nad Nisou", "Turnov"},
            {"R", "Liberec", "Turnov"},
            {"R", "Turnov", "Semily"},
            {"R", "Turnov", "Jičín", "Hradec Králové"},
            {"R", "Praha", "Mělník", "Česká Lípa"},
            {"R", "Mladá Boleslav", "Jičín"},
            {"R", "Mladá Boleslav", "Česká Lípa"},
            {"R", "Hradec Králové", "Náchod"},
            {"R", "Hradec Králové", "Trutnov"},
            {"R", "Hodonín", "Kyjov", "Vyškov"}
    };

    static final List<Mesto> MESTA = new ArrayList<>();
    static final List<Hrana> HRANY = new ArrayList<>();
    private static final List<List<Hrana>> SOUSEDE = new ArrayList<>();

    static {
        Map<String, Integer> index = new HashMap<>();
        for (String radek : MESTA_DATA) {
            String[] p = radek.split("\\|");
            int id = MESTA.size();
            MESTA.add(new Mesto(id, p[0], Double.parseDouble(p[1]), Double.parseDouble(p[2]),
                    Integer.parseInt(p[3])));
            index.put(p[0], id);
            SOUSEDE.add(new ArrayList<>());
        }
        Map<Long, Hrana> hrany = new HashMap<>();
        for (String[] s : SILNICE) {
            boolean dal = s[0].equals("D");
            for (int i = 1; i + 1 < s.length; i++) {
                Integer a = index.get(s[i]), b = index.get(s[i + 1]);
                if (a == null || b == null || a.equals(b)) continue;
                int lo = Math.min(a, b), hi = Math.max(a, b);
                long klic = lo * 1000L + hi;
                Hrana stara = hrany.get(klic);
                if (stara != null && (stara.dalnice || !dal)) continue;
                Mesto ma = MESTA.get(lo), mb = MESTA.get(hi);
                double km = vzdalenost(ma.lon, ma.lat, mb.lon, mb.lat) * (dal ? 1.08 : 1.22);
                hrany.put(klic, new Hrana(lo, hi, dal, km));
            }
        }
        for (Hrana h : hrany.values()) {
            HRANY.add(h);
            SOUSEDE.get(h.a).add(h);
            SOUSEDE.get(h.b).add(h);
        }
    }

    private MapaData() {
    }

    // ------------------------------------------------------------------
    // projekce (kilometry od středu mapy)
    // ------------------------------------------------------------------

    static double mx(double lon) {
        return (lon - 15.4) * 71.5;
    }

    static double my(double lat) {
        return (49.8 - lat) * 111.2;
    }

    static double vzdalenost(double lon1, double lat1, double lon2, double lat2) {
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * 6371.0 * Math.asin(Math.sqrt(a));
    }

    // ------------------------------------------------------------------
    // hledání trasy
    // ------------------------------------------------------------------

    /** Trasa přes všechna zadaná města za sebou, nebo null když mezi nějakými dvěma spojení není. */
    static Trasa najdi(List<Integer> body, boolean nejrychlejsi) {
        Trasa vysl = new Trasa();
        if (body.size() < 2) return null;
        for (int i = 0; i + 1 < body.size(); i++) {
            Trasa t = spoj(body.get(i), body.get(i + 1), nejrychlejsi);
            if (t == null) return null;
            for (int k = 0; k < t.uzly.size(); k++) {
                if (!(i > 0 && k == 0)) vysl.uzly.add(t.uzly.get(k));
            }
            vysl.km += t.km;
            vysl.hodiny += t.hodiny;
            vysl.kmDalnice += t.kmDalnice;
        }
        return vysl;
    }

    private static Trasa spoj(int z, int d, boolean cas) {
        int n = MESTA.size();
        double[] dist = new double[n];
        Arrays.fill(dist, Double.MAX_VALUE);
        Hrana[] pred = new Hrana[n];
        int[] odkud = new int[n];
        Arrays.fill(odkud, -1);
        PriorityQueue<double[]> fronta = new PriorityQueue<>((x, y) -> Double.compare(x[0], y[0]));
        dist[z] = 0;
        fronta.add(new double[]{0, z});
        while (!fronta.isEmpty()) {
            double[] c = fronta.poll();
            int u = (int) c[1];
            if (c[0] > dist[u]) continue;
            if (u == d) break;
            for (Hrana h : SOUSEDE.get(u)) {
                int v = h.a == u ? h.b : h.a;
                double w = cas ? h.hodiny() : h.km;
                if (dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                    pred[v] = h;
                    odkud[v] = u;
                    fronta.add(new double[]{dist[v], v});
                }
            }
        }
        if (dist[d] == Double.MAX_VALUE) return null;

        Trasa t = new Trasa();
        LinkedList<Integer> cesta = new LinkedList<>();
        for (int v = d; v != -1; v = odkud[v]) {
            cesta.addFirst(v);
            if (pred[v] != null) {
                t.km += pred[v].km;
                t.hodiny += pred[v].hodiny();
                if (pred[v].dalnice) t.kmDalnice += pred[v].km;
            }
        }
        t.uzly.addAll(cesta);
        return t;
    }
}