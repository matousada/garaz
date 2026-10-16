package gui;

import data.Auto;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Jednoduchý 3D model auta. Z názvu a značky se pozná typ karoserie, ke každému typu patří
 * profil (boční silueta), ze kterého se postaví trojúhelníky pro 3D vykreslení a kreslí se z něj i avatar.
 * Souřadnice jsou v metrech: x dopředu, y doleva, z nahoru.
 */
final class CarModel {

    enum Typ {
        HATCHBACK("Hatchback"), SEDAN("Sedan"), LIFTBACK("Liftback"), KOMBI("Kombi"), SUV("SUV"),
        SPORT("Sportovní kupé"), PICKUP("Pick-up"), VAN("Dodávka"), TRUCK("Nákladní auto");

        final String nazev;

        Typ(String nazev) {
            this.nazev = nazev;
        }

        @Override
        public String toString() {
            return nazev;
        }
    }

    static final class Detekce {
        final Typ typ;
        final boolean podleNazvu;   // false = jen odhad podle spotřeby a nádrže
        final String klic;

        Detekce(Typ typ, boolean podleNazvu, String klic) {
            this.typ = typ;
            this.podleNazvu = podleNazvu;
            this.klic = klic;
        }
    }

    /** Jeden trojúhelník modelu. */
    static final class Tri {
        final float[] v = new float[9];
        float nx, ny, nz;
        float bias;   // posun směrem ke kameře (v metrech), aby nálepky přebily podklad
        int rgb;
        int mode;     // 0 = lak, 1 = sklo, 2 = svítí, 3 = matné
        int side;     // u kol: strana (-1 / +1), jinak 0
    }

    static final class Znacka {
        final double x, z;
        final int smer;   // +1 přední, -1 zadní

        Znacka(double x, double z, int smer) {
            this.x = x;
            this.z = z;
            this.smer = smer;
        }
    }

    static final class Sestava {
        final List<Tri> tri = new ArrayList<>();
        final List<Znacka> znacky = new ArrayList<>();
    }

    private static final class Def {
        double halfW, zb, r, belt, winS, winE, mirrorX, len, height;
        double inset = 0.07;
        double[] pillars = {};
        double[] wheelX = {};
        double[][] prof;
        String seg;
        boolean bed;
    }

    // ------------------------------------------------------------------
    // barvy laku
    // ------------------------------------------------------------------

    static final String[] BARVY_JMENA = {"Bílá", "Černá", "Stříbrná", "Šedá", "Červená", "Modrá", "Tmavě modrá",
            "Zelená", "Žlutá", "Oranžová", "Fialová", "Hnědá", "Béžová", "Tyrkysová"};
    static final int[] BARVY = {0xF0F0F2, 0x1C1C20, 0xB2B8C0, 0x606670, 0xC82028, 0x2456BE, 0x16285C,
            0x248C54, 0xF0C428, 0xEC781C, 0x7846AA, 0x704C34, 0xCEBE96, 0x18A0AA};

    static int vychoziBarva(Auto a) {
        int h = (a.getZnacka() + "|" + a.getJmenoAuta()).hashCode() & 0x7fffffff;
        return BARVY[h % BARVY.length];
    }

    /** Barva laku auta: vybraná uživatelem, nebo automatická. */
    static int barvaAuta(Auto a) {
        return a.getBarva() >= 0 ? a.getBarva() : vychoziBarva(a);
    }

    static int mixRgb(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    // ------------------------------------------------------------------
    // rozpoznání typu karoserie podle názvu ("=xyz" = musí sedět celé slovo)
    // ------------------------------------------------------------------

    private static final Object[][] PRAVIDLA = {
            {Typ.SEDAN, new String[]{"tatra603", "tatra613", "tatra700"}},
            {Typ.KOMBI, new String[]{"combi", "kombi", "variant", "avant", "touring", "estate", "wagon",
                    "sportstourer", "sportbrake", "shootingbrake", "=sw"}},
            {Typ.TRUCK, new String[]{"tatra", "liaz", "scania", "=daf", "=man", "iveco", "kamion", "truck",
                    "nakladni", "actros", "volvofh", "=tgx", "=tgs"}},
            {Typ.PICKUP, new String[]{"hilux", "ranger", "l200", "navara", "amarok", "f150", "f250", "silverado",
                    "=ram", "tundra", "tacoma", "pickup", "dmax", "frontier", "triton", "gladiator"}},
            {Typ.VAN, new String[]{"transit", "sprinter", "crafter", "transporter", "caravelle", "multivan", "vito",
                    "viano", "trafic", "vivaro", "boxer", "ducato", "jumper", "=master", "berlingo", "kangoo",
                    "caddy", "=partner", "expert", "scudo", "combo", "doblo", "dodavka", "=avia", "multicar", "=van",
                    "minibus", "proace", "movano", "daily", "=t4", "=t5", "=t6"}},
            {Typ.SUV, new String[]{"kodiaq", "karoq", "kamiq", "yeti", "suv", "tiguan", "touareg", "tcross", "troc",
                    "taigo", "=q2", "=q3", "=q5", "=q7", "=q8", "=x1", "=x2", "=x3", "=x4", "=x5", "=x6", "=x7",
                    "rav4", "crv", "sportage", "tucson", "sorento", "qashqai", "juke", "kuga", "captur", "duster",
                    "outlander", "pajero", "landcruiser", "defender", "rangerover", "evoque", "discovery",
                    "wrangler", "jimny", "vitara", "cayenne", "macan", "stelvio", "kona", "santafe", "crossover",
                    "xtrail", "ateca", "arona", "bronco", "jeep", "4x4", "enyaq", "id4", "id5", "cx5", "cx30",
                    "cx3", "ix35", "ix20", "bayon", "puma", "ecosport", "formentor", "tarraco", "seltos", "niro",
                    "stonic", "mokka", "grandland", "crossland", "kadjar", "koleos", "arkana", "3008", "5008",
                    "2008"}},
            {Typ.SPORT, new String[]{"porsche", "ferrari", "lamborghini", "mustang", "corvette", "911", "gtr", "mx5",
                    "miata", "supra", "z4", "boxster", "cayman", "amggt", "viper", "lotus", "mclaren", "alpine",
                    "brz", "gt86", "camaro", "challenger", "roadster", "cabrio", "coupe", "kupe", "spider", "=tt",
                    "=gt", "aston", "maserati", "bugatti", "=r8", "=m3", "=m4", "=m5", "nsx", "charger"}},
            {Typ.LIFTBACK, new String[]{"octavia", "superb", "sportback", "liftback", "fastback", "arteon", "passatcc",
                    "insignia", "rapid", "=a5", "=a7"}},
            {Typ.SEDAN, new String[]{"sedan", "limuzina", "=120", "=105", "=100", "=130", "=110", "=125", "garde",
                    "estelle", "passat", "=a4", "=a6", "=a8", "cclass", "eclass", "sclass", "mondeo", "accord",
                    "camry", "corolla", "elantra", "=i40", "fluence", "logan", "versa", "jetta", "bora", "vento",
                    "taunus", "lada", "zhiguli", "moskvic", "wartburg", "trabant"}},
            {Typ.HATCHBACK, new String[]{"fabia", "felicia", "favorit", "forman", "citigo", "golf", "polo", "=up",
                    "ibiza", "leon", "clio", "corsa", "fiesta", "focus", "civic", "i20", "i30", "ceed", "=c3",
                    "208", "108", "500", "panda", "yaris", "aygo", "swift", "micra", "note", "mini", "=a1", "=a3",
                    "viva", "stilo", "punto", "twingo", "jazz", "spark", "picanto", "rio", "scirocco", "astra",
                    "megane", "207", "206", "307", "308", "tipo", "colt", "scala"}}
    };

    private static String norm(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT);
    }

    static Detekce detekuj(Auto a) {
        String n = norm(a.getZnacka() + " " + a.getJmenoAuta());
        String[] tok = n.split("[^a-z0-9]+");
        String c = n.replaceAll("[^a-z0-9]", "");
        for (Object[] pravidlo : PRAVIDLA) {
            String[] klice = (String[]) pravidlo[1];
            for (String k : klice) {
                if (k.startsWith("=")) {
                    String e = k.substring(1);
                    for (String t : tok) {
                        if (t.equals(e)) return new Detekce((Typ) pravidlo[0], true, e);
                    }
                } else if (c.contains(k)) {
                    return new Detekce((Typ) pravidlo[0], true, k);
                }
            }
        }
        // nic jsme nepoznali, odhadneme podle parametrů
        Typ odhad;
        if (a.getSpotreba() >= 18f) odhad = Typ.TRUCK;
        else if (a.getSpotreba() >= 12f || a.getMaxPalivo() >= 75f) odhad = Typ.SUV;
        else if (a.getMaxPalivo() >= 55f) odhad = Typ.SEDAN;
        else odhad = Typ.HATCHBACK;
        return new Detekce(odhad, false, "");
    }

    private static final Map<Typ, CarModel> CACHE = new EnumMap<>(Typ.class);

    static CarModel proTyp(Typ typ) {
        CarModel m = CACHE.get(typ);
        if (m == null) {
            m = new CarModel(typ);
            CACHE.put(typ, m);
        }
        return m;
    }

    // ------------------------------------------------------------------
    // profily karoserií: {x, z, šířka (0..1)}
    // ------------------------------------------------------------------

    private static Def def(Typ t) {
        Def d = new Def();
        switch (t) {
            case HATCHBACK:
                d.halfW = 0.88; d.zb = 0.30; d.r = 0.31; d.belt = 0.98; d.winS = -1.36; d.winE = 0.55;
                d.pillars = new double[]{-0.2}; d.wheelX = new double[]{-1.25, 1.2}; d.mirrorX = 0.72;
                d.len = 3.9; d.height = 1.45;
                d.prof = new double[][]{{-1.95, 0.30, 1.0}, {-1.95, 0.86, 1.0}, {-1.80, 0.98, 0.97},
                        {-1.45, 1.40, 0.92}, {-0.15, 1.45, 0.92}, {0.75, 1.00, 0.98}, {1.55, 0.86, 1.0},
                        {1.95, 0.62, 1.0}, {1.95, 0.30, 1.0}};
                d.seg = "BBGBGBBB";
                break;
            case SEDAN:
                d.halfW = 0.89; d.zb = 0.30; d.r = 0.32; d.belt = 0.99; d.winS = -0.98; d.winE = 0.85;
                d.pillars = new double[]{-0.05}; d.wheelX = new double[]{-1.45, 1.40}; d.mirrorX = 0.95;
                d.len = 4.6; d.height = 1.46;
                d.prof = new double[][]{{-2.30, 0.30, 1.0}, {-2.30, 0.84, 1.0}, {-1.70, 0.98, 0.97},
                        {-1.05, 1.44, 0.92}, {0.20, 1.46, 0.92}, {1.05, 1.00, 0.98}, {2.05, 0.90, 1.0},
                        {2.30, 0.66, 1.0}, {2.30, 0.30, 1.0}};
                d.seg = "BBGBGBBB";
                break;
            case LIFTBACK:
                d.halfW = 0.89; d.zb = 0.30; d.r = 0.32; d.belt = 1.01; d.winS = -1.05; d.winE = 0.90;
                d.pillars = new double[]{0.0}; d.wheelX = new double[]{-1.48, 1.42}; d.mirrorX = 1.0;
                d.len = 4.7; d.height = 1.46;
                d.prof = new double[][]{{-2.35, 0.30, 1.0}, {-2.35, 0.90, 1.0}, {-2.12, 1.02, 0.97},
                        {-1.15, 1.42, 0.92}, {0.25, 1.46, 0.92}, {1.10, 1.01, 0.98}, {2.05, 0.91, 1.0},
                        {2.35, 0.66, 1.0}, {2.35, 0.30, 1.0}};
                d.seg = "BBGBGBBB";
                break;
            case KOMBI:
                d.halfW = 0.89; d.zb = 0.30; d.r = 0.32; d.belt = 1.04; d.winS = -1.95; d.winE = 0.95;
                d.pillars = new double[]{-0.95, 0.1}; d.wheelX = new double[]{-1.48, 1.42}; d.mirrorX = 1.05;
                d.len = 4.7; d.height = 1.46;
                d.prof = new double[][]{{-2.35, 0.30, 1.0}, {-2.35, 0.98, 1.0}, {-2.25, 1.06, 0.97},
                        {-2.05, 1.42, 0.93}, {0.30, 1.46, 0.92}, {1.15, 1.01, 0.98}, {2.05, 0.91, 1.0},
                        {2.35, 0.66, 1.0}, {2.35, 0.30, 1.0}};
                d.seg = "BBGBGBBB";
                break;
            case SUV:
                d.halfW = 0.92; d.zb = 0.40; d.r = 0.40; d.belt = 1.22; d.winS = -1.85; d.winE = 0.85;
                d.pillars = new double[]{-0.85, 0.10}; d.wheelX = new double[]{-1.40, 1.40}; d.mirrorX = 0.95;
                d.len = 4.5; d.height = 1.72;
                d.prof = new double[][]{{-2.25, 0.40, 1.0}, {-2.25, 1.08, 1.0}, {-2.15, 1.20, 0.97},
                        {-1.95, 1.68, 0.93}, {0.30, 1.72, 0.92}, {1.00, 1.22, 0.98}, {1.95, 1.08, 1.0},
                        {2.25, 0.88, 1.0}, {2.25, 0.40, 1.0}};
                d.seg = "BBGBGBBB";
                break;
            case SPORT:
                d.halfW = 0.95; d.zb = 0.22; d.r = 0.33; d.belt = 0.80; d.winS = -0.88; d.winE = 0.50;
                d.pillars = new double[]{}; d.wheelX = new double[]{-1.35, 1.40}; d.mirrorX = 0.55;
                d.len = 4.4; d.height = 1.2;
                d.prof = new double[][]{{-2.20, 0.22, 1.0}, {-2.20, 0.66, 1.0}, {-1.75, 0.78, 0.98},
                        {-0.95, 1.14, 0.90}, {-0.10, 1.20, 0.88}, {0.60, 0.88, 0.98}, {1.90, 0.68, 1.0},
                        {2.20, 0.52, 1.0}, {2.20, 0.22, 1.0}};
                d.seg = "BBGBGBBB";
                break;
            case PICKUP:
                d.halfW = 0.93; d.zb = 0.45; d.r = 0.42; d.belt = 1.18; d.winS = -0.75; d.winE = 1.0;
                d.pillars = new double[]{-0.1}; d.wheelX = new double[]{-1.75, 1.65}; d.mirrorX = 1.05;
                d.len = 5.3; d.height = 1.65; d.bed = true;
                d.prof = new double[][]{{-2.65, 0.45, 1.0}, {-2.65, 1.10, 1.0}, {-0.95, 1.12, 1.0},
                        {-0.85, 1.62, 0.93}, {0.50, 1.65, 0.92}, {1.15, 1.18, 0.98}, {2.25, 1.05, 1.0},
                        {2.65, 0.85, 1.0}, {2.65, 0.45, 1.0}};
                d.seg = "BBGBGBBB";
                break;
            case VAN:
                d.halfW = 1.0; d.zb = 0.40; d.r = 0.38; d.belt = 1.35; d.winS = 0.75; d.winE = 1.40;
                d.pillars = new double[]{}; d.wheelX = new double[]{-1.55, 1.55}; d.mirrorX = 1.45;
                d.len = 5.0; d.height = 2.0;
                d.prof = new double[][]{{-2.50, 0.40, 1.0}, {-2.50, 1.90, 1.0}, {-2.40, 1.99, 0.97},
                        {0.90, 1.99, 0.97}, {1.55, 1.40, 0.98}, {2.15, 1.00, 1.0}, {2.50, 0.80, 1.0},
                        {2.50, 0.40, 1.0}};
                d.seg = "BBBGBBB";
                break;
            default:   // TRUCK
                d.halfW = 1.25; d.zb = 0.65; d.r = 0.50; d.belt = 1.95; d.winS = 1.95; d.winE = 3.05;
                d.pillars = new double[]{}; d.wheelX = new double[]{-2.6, -1.45, 2.55}; d.mirrorX = 3.1;
                d.len = 7.5; d.height = 3.15;
                d.prof = new double[][]{{-3.75, 0.65, 1.0}, {-3.75, 3.15, 1.0}, {0.95, 3.15, 1.0},
                        {1.00, 2.70, 0.96}, {2.55, 2.70, 0.96}, {3.30, 2.00, 0.98}, {3.75, 1.90, 1.0},
                        {3.75, 0.65, 1.0}};
                d.seg = "BBBBGBB";
                break;
        }
        return d;
    }

    // ------------------------------------------------------------------
    // model
    // ------------------------------------------------------------------

    final Typ typ;
    private final Def d;
    private final double[][] P;                 // vyhlazený profil
    private final String S;                     // typy úseků (B = lak, G = sklo)
    private final List<double[][]> okna = new ArrayList<>();

    private CarModel(Typ typ) {
        this.typ = typ;
        this.d = def(typ);

        // vyhlazení rohů (jedno seříznutí)
        List<double[]> pts = new ArrayList<>();
        StringBuilder st = new StringBuilder();
        int n = d.prof.length;
        pts.add(d.prof[0].clone());
        for (int i = 0; i < n - 1; i++) {
            double[] a = d.prof[i], b = d.prof[i + 1];
            if (i > 0) {
                pts.add(mix(a, b, 0.25));
                char c = d.seg.charAt(i - 1) == d.seg.charAt(i) ? d.seg.charAt(i) : 'B';
                st.append(c);
            }
            if (i < n - 2) {
                pts.add(mix(a, b, 0.75));
            } else {
                pts.add(b.clone());
            }
            st.append(d.seg.charAt(i));
        }
        P = pts.toArray(new double[0][]);
        S = st.toString();

        // okna na bocích
        List<double[]> useky = new ArrayList<>();
        double start = d.winS, mezera = 0.045;
        for (double p : d.pillars) {
            useky.add(new double[]{start, p - mezera});
            start = p + mezera;
        }
        useky.add(new double[]{start, d.winE});
        for (double[] u : useky) {
            if (u[1] - u[0] < 0.12) continue;
            List<double[]> horni = new ArrayList<>();
            horni.add(new double[]{u[0], okTop(u[0])});
            for (double[] p : P) {
                if (p[0] > u[0] + 1e-6 && p[0] < u[1] - 1e-6) horni.add(new double[]{p[0], okTop(p[0])});
            }
            horni.add(new double[]{u[1], okTop(u[1])});
            double[][] poly = new double[horni.size() + 2][];
            for (int i = 0; i < horni.size(); i++) poly[i] = horni.get(i);
            poly[horni.size()] = new double[]{u[1], d.belt};
            poly[horni.size() + 1] = new double[]{u[0], d.belt};
            okna.add(poly);
        }
    }

    double len() {
        return d.len;
    }

    double height() {
        return d.height;
    }

    double halfW() {
        return d.halfW;
    }

    private static double[] mix(double[] a, double[] b, double t) {
        return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    private double zTop(double x) {
        int n = P.length;
        if (x <= P[0][0]) return P[0][1];
        for (int i = 0; i < n - 1; i++) {
            double x0 = P[i][0], x1 = P[i + 1][0];
            if (x >= x0 && x <= x1) {
                if (x1 - x0 < 1e-9) return Math.max(P[i][1], P[i + 1][1]);
                double t = (x - x0) / (x1 - x0);
                return P[i][1] + (P[i + 1][1] - P[i][1]) * t;
            }
        }
        return P[n - 1][1];
    }

    private double okTop(double x) {
        return Math.max(zTop(x) - d.inset, d.belt + 0.05);
    }

    // ------------------------------------------------------------------
    // stavba trojúhelníků
    // ------------------------------------------------------------------

    Sestava postav(int rgb) {
        Sestava s = new Sestava();
        List<Tri> t = s.tri;
        double hw0 = d.halfW;
        int n = P.length;
        double[] ref = {0, 0, d.height * 0.4};
        int sklo = 0x1C2636;

        // horní plochy a čela
        for (int i = 0; i < n - 1; i++) {
            double[] a = P[i], b = P[i + 1];
            double[][] q = {
                    {a[0], -a[2] * hw0, a[1]}, {a[0], a[2] * hw0, a[1]},
                    {b[0], b[2] * hw0, b[1]}, {b[0], -b[2] * hw0, b[1]}};
            boolean g = S.charAt(i) == 'G';
            poly(t, q, g ? sklo : rgb, g ? 1 : 0, 0f, ref);
        }

        // boky, okna, kola, zrcátka
        for (int sd = -1; sd <= 1; sd += 2) {
            double[][] bok = new double[n][];
            for (int i = 0; i < n; i++) bok[i] = new double[]{P[i][0], sd * P[i][2] * hw0, P[i][1]};
            poly(t, bok, rgb, 0, 0f, ref);
            for (double[][] o : okna) {
                double[][] w = new double[o.length][];
                for (int i = 0; i < o.length; i++) w[i] = new double[]{o[i][0], sd * (hw0 + 0.012), o[i][1]};
                poly(t, w, sklo, 1, 0.6f, ref);
            }
            for (double xc : d.wheelX) kolo(t, xc, sd);
            box(t, d.mirrorX, sd * (hw0 + 0.085), d.belt + 0.03, 0.14, 0.16, 0.085, rgb, 0.9f);
        }

        // čelo a záď: světla, mřížka, SPZ
        double xf = P[n - 1][0], xr = P[0][0];
        double zfLo = P[n - 1][1], zfHi = P[n - 2][1], hf = zfHi - zfLo;
        double zrLo = P[0][1], zrHi = P[1][1], hr = zrHi - zrLo;
        for (int sd = -1; sd <= 1; sd += 2) {
            double y1 = sd * 0.50 * hw0, y2 = sd * 0.86 * hw0;
            double zf1 = zfLo + 0.50 * hf, zf2 = zfLo + 0.92 * hf;
            poly(t, new double[][]{{xf + 0.008, y1, zf1}, {xf + 0.008, y2, zf1}, {xf + 0.008, y2, zf2},
                    {xf + 0.008, y1, zf2}}, 0xFFF4C8, 2, 0.5f, ref);
            double zr1 = zrLo + 0.55 * hr, zr2 = zrLo + 0.92 * hr;
            poly(t, new double[][]{{xr - 0.008, y1, zr1}, {xr - 0.008, y2, zr1}, {xr - 0.008, y2, zr2},
                    {xr - 0.008, y1, zr2}}, 0xD01820, 2, 0.5f, ref);
        }
        poly(t, new double[][]{{xf + 0.006, -0.42 * hw0, zfLo + 0.28 * hf}, {xf + 0.006, 0.42 * hw0, zfLo + 0.28 * hf},
                {xf + 0.006, 0.42 * hw0, zfLo + 0.62 * hf}, {xf + 0.006, -0.42 * hw0, zfLo + 0.62 * hf}},
                0x1E1F23, 3, 0.5f, ref);
        spz(s, xf + 0.012, zfLo + 0.02, +1, ref);
        spz(s, xr - 0.012, zrLo + Math.min(0.30 * hr, hr - 0.16), -1, ref);

        // korba pick-upu
        if (d.bed) {
            double zb = d.prof[1][1] + 0.015;
            poly(t, new double[][]{{-2.50, -0.80 * hw0, zb}, {-1.10, -0.80 * hw0, zb}, {-1.10, 0.80 * hw0, zb},
                    {-2.50, 0.80 * hw0, zb}}, 0x2A2C30, 3, 0.5f, ref);
        }
        return s;
    }

    /** SPZ: bílá tabulka s modrým pruhem, text se dokresluje až při zobrazení. */
    private void spz(Sestava s, double x, double zDole, int smer, double[] ref) {
        double ph = 0.13, pw = 0.26;
        double z1 = zDole, z2 = zDole + ph;
        poly(s.tri, new double[][]{{x, -pw, z1}, {x, pw, z1}, {x, pw, z2}, {x, -pw, z2}}, 0xF2F2EE, 3, 0.55f, ref);
        double lev = smer > 0 ? -pw : pw;          // levý okraj tabulky, jak ho vidí divák
        double lev2 = smer > 0 ? -pw + 0.05 : pw - 0.05;
        double xs = x + smer * 0.002;
        poly(s.tri, new double[][]{{xs, lev, z1}, {xs, lev2, z1}, {xs, lev2, z2}, {xs, lev, z2}},
                0x1D3E9A, 3, 0.6f, ref);
        s.znacky.add(new Znacka(x, zDole + ph / 2, smer));
    }

    private void kolo(List<Tri> t, double xc, int sd) {
        double r = d.r, hw0 = d.halfW;
        int N = 20, od = t.size();
        double yOut = sd * (hw0 + 0.025), yIn = sd * hw0;
        double[] telo = {0, 0, d.height * 0.4};

        // tmavý oblouk blatníku
        double R = r * 1.2, yA = sd * (hw0 + 0.006);
        double[][] oblouk = new double[N][];
        for (int i = 0; i < N; i++) {
            double a = 2 * Math.PI * i / N;
            oblouk[i] = new double[]{xc + R * Math.cos(a), yA, Math.max(d.zb, r + R * Math.sin(a))};
        }
        fan(t, oblouk, 0x15161A, 3, 1.0f, telo);

        // běhoun (tenký pás po obvodu)
        double[] refT = {xc, (yOut + yIn) / 2, r};
        for (int i = 0; i < N; i++) {
            double a0 = 2 * Math.PI * i / N, a1 = 2 * Math.PI * (i + 1) / N;
            double[][] q = {
                    {xc + r * Math.cos(a0), yOut, r + r * Math.sin(a0)},
                    {xc + r * Math.cos(a1), yOut, r + r * Math.sin(a1)},
                    {xc + r * Math.cos(a1), yIn, r + r * Math.sin(a1)},
                    {xc + r * Math.cos(a0), yIn, r + r * Math.sin(a0)}};
            poly(t, q, 0x232326, 3, 1.2f, refT);
        }

        // pneumatika, ráfek, otvory a nábojka
        double[] refD = {xc, 0, r};
        fan(t, kruh(xc, yOut, r, r, N), 0x2A2A2E, 3, 1.2f, refD);
        fan(t, kruh(xc, yOut + sd * 0.004, r, r * 0.64, N), 0xC2C8D2, 0, 1.2f, refD);
        for (int k = 0; k < 5; k++) {
            double a = 2 * Math.PI * k / 5 + 0.3, yS = yOut + sd * 0.008;
            double[][] q = {
                    {xc + r * 0.24 * Math.cos(a - 0.22), yS, r + r * 0.24 * Math.sin(a - 0.22)},
                    {xc + r * 0.58 * Math.cos(a - 0.20), yS, r + r * 0.58 * Math.sin(a - 0.20)},
                    {xc + r * 0.58 * Math.cos(a + 0.20), yS, r + r * 0.58 * Math.sin(a + 0.20)},
                    {xc + r * 0.24 * Math.cos(a + 0.22), yS, r + r * 0.24 * Math.sin(a + 0.22)}};
            poly(t, q, 0x32353C, 3, 1.2f, refD);
        }
        fan(t, kruh(xc, yOut + sd * 0.012, r, r * 0.2, N), 0x8A909C, 0, 1.2f, refD);

        for (int i = od; i < t.size(); i++) t.get(i).side = sd;
    }

    private static double[][] kruh(double xc, double y, double zc, double R, int N) {
        double[][] v = new double[N][];
        for (int i = 0; i < N; i++) {
            double a = 2 * Math.PI * i / N;
            v[i] = new double[]{xc + R * Math.cos(a), y, zc + R * Math.sin(a)};
        }
        return v;
    }

    private void box(List<Tri> t, double cx, double cy, double cz, double lx, double ly, double lz,
                     int rgb, float bias) {
        double x0 = cx - lx / 2, x1 = cx + lx / 2, y0 = cy - ly / 2, y1 = cy + ly / 2, z0 = cz - lz / 2, z1 = cz + lz / 2;
        double[] ref = {cx, cy, cz};
        double[][][] steny = {
                {{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}},
                {{x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}},
                {{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}},
                {{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}},
                {{x0, y0, z0}, {x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}},
                {{x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}, {x1, y0, z1}}};
        for (double[][] f : steny) poly(t, f, rgb, 0, bias, ref);
    }

    // ------------------------------------------------------------------
    // geometrie: normály a triangulace
    // ------------------------------------------------------------------

    private static double[] normala(double[][] p, double[] ref) {
        double nx = 0, ny = 0, nz = 0, cx = 0, cy = 0, cz = 0;
        int n = p.length;
        for (int i = 0; i < n; i++) {
            double[] a = p[i], b = p[(i + 1) % n];
            nx += (a[1] - b[1]) * (a[2] + b[2]);
            ny += (a[2] - b[2]) * (a[0] + b[0]);
            nz += (a[0] - b[0]) * (a[1] + b[1]);
            cx += a[0];
            cy += a[1];
            cz += a[2];
        }
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-9) return null;
        nx /= len;
        ny /= len;
        nz /= len;
        cx /= n;
        cy /= n;
        cz /= n;
        if ((cx - ref[0]) * nx + (cy - ref[1]) * ny + (cz - ref[2]) * nz < 0) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
        }
        return new double[]{nx, ny, nz};
    }

    private static void emit(List<Tri> out, double[] a, double[] b, double[] c, double[] n,
                             int rgb, int mode, float bias) {
        Tri t = new Tri();
        t.v[0] = (float) a[0]; t.v[1] = (float) a[1]; t.v[2] = (float) a[2];
        t.v[3] = (float) b[0]; t.v[4] = (float) b[1]; t.v[5] = (float) b[2];
        t.v[6] = (float) c[0]; t.v[7] = (float) c[1]; t.v[8] = (float) c[2];
        t.nx = (float) n[0];
        t.ny = (float) n[1];
        t.nz = (float) n[2];
        t.rgb = rgb;
        t.mode = mode;
        t.bias = bias;
        out.add(t);
    }

    /** Libovolný rovinný mnohoúhelník (třeba nekonvexní bok auta). */
    private static void poly(List<Tri> out, double[][] p, int rgb, int mode, float bias, double[] ref) {
        double[] n = normala(p, ref);
        if (n == null) return;
        for (int[] k : triangulace(p)) emit(out, p[k[0]], p[k[1]], p[k[2]], n, rgb, mode, bias);
    }

    /** Konvexní mnohoúhelník rozdělený vějířem. */
    private static void fan(List<Tri> out, double[][] p, int rgb, int mode, float bias, double[] ref) {
        double[] n = normala(p, ref);
        if (n == null) return;
        for (int i = 1; i + 1 < p.length; i++) emit(out, p[0], p[i], p[i + 1], n, rgb, mode, bias);
    }

    private static List<int[]> triangulace(double[][] p) {
        int n = p.length;
        List<int[]> res = new ArrayList<>();
        if (n < 3) return res;
        if (n == 3) {
            res.add(new int[]{0, 1, 2});
            return res;
        }
        double nx = 0, ny = 0, nz = 0;
        for (int i = 0; i < n; i++) {
            double[] a = p[i], b = p[(i + 1) % n];
            nx += (a[1] - b[1]) * (a[2] + b[2]);
            ny += (a[2] - b[2]) * (a[0] + b[0]);
            nz += (a[0] - b[0]) * (a[1] + b[1]);
        }
        int drop = 0;
        double mx = Math.abs(nx);
        if (Math.abs(ny) > mx) {
            drop = 1;
            mx = Math.abs(ny);
        }
        if (Math.abs(nz) > mx) drop = 2;
        double[] u = new double[n], w = new double[n];
        for (int i = 0; i < n; i++) {
            if (drop == 0) {
                u[i] = p[i][1];
                w[i] = p[i][2];
            } else if (drop == 1) {
                u[i] = p[i][2];
                w[i] = p[i][0];
            } else {
                u[i] = p[i][0];
                w[i] = p[i][1];
            }
        }
        double plocha = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            plocha += u[i] * w[j] - u[j] * w[i];
        }
        boolean ccw = plocha > 0;
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < n; i++) idx.add(i);
        int pojistka = 0;
        while (idx.size() > 3 && pojistka++ < 2000) {
            boolean uriznuto = false;
            int m = idx.size();
            for (int k = 0; k < m; k++) {
                int i0 = idx.get((k + m - 1) % m), i1 = idx.get(k), i2 = idx.get((k + 1) % m);
                double cross = (u[i1] - u[i0]) * (w[i2] - w[i0]) - (w[i1] - w[i0]) * (u[i2] - u[i0]);
                if (Math.abs(cross) <= 1e-12) {      // body v jedné přímce: vrchol jen vypustíme
                    idx.remove(k);
                    uriznuto = true;
                    break;
                }
                if (ccw ? cross < 0 : cross > 0) continue;   // vrchol není konvexní
                boolean uvnitr = false;
                for (int q : idx) {
                    if (q == i0 || q == i1 || q == i2) continue;
                    if (vTrojuhelniku(u[q], w[q], u[i0], w[i0], u[i1], w[i1], u[i2], w[i2])) {
                        uvnitr = true;
                        break;
                    }
                }
                if (uvnitr) continue;
                res.add(new int[]{i0, i1, i2});
                idx.remove(k);
                uriznuto = true;
                break;
            }
            if (!uriznuto) break;
        }
        if (idx.size() >= 3) {
            for (int i = 1; i + 1 < idx.size(); i++) res.add(new int[]{idx.get(0), idx.get(i), idx.get(i + 1)});
        }
        return res;
    }

    private static boolean vTrojuhelniku(double px, double py, double ax, double ay, double bx, double by,
                                         double cx, double cy) {
        double d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by);
        double d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy);
        double d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay);
        boolean zaporne = d1 < -1e-12 || d2 < -1e-12 || d3 < -1e-12;
        boolean kladne = d1 > 1e-12 || d2 > 1e-12 || d3 > 1e-12;
        return !(zaporne && kladne);
    }

    // ------------------------------------------------------------------
    // avatar (2D boční pohled, auto směřuje doprava)
    // ------------------------------------------------------------------

    void kresliAvatar(Graphics2D gg, int px, int py, int pw, int ph, int rgb) {
        Graphics2D g = (Graphics2D) gg.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double s = Math.min((pw - 8) / (d.len + 0.2), (ph - 8) / (d.height + 0.1));
        double x0 = px + pw / 2.0;
        double base = py + ph - 4;

        g.setColor(new Color(0, 0, 0, 45));
        g.fill(new Ellipse2D.Double(x0 - d.len / 2 * s, base - 0.12 * s, d.len * s, 0.24 * s));

        Path2D.Double telo = new Path2D.Double();
        for (int i = 0; i < P.length; i++) {
            double x = x0 + P[i][0] * s, y = base - P[i][1] * s;
            if (i == 0) telo.moveTo(x, y);
            else telo.lineTo(x, y);
        }
        telo.closePath();
        Color svetla = new Color(mixRgb(rgb, 0xFFFFFF, 0.35f));
        Color tmava = new Color(mixRgb(rgb, 0x000000, 0.35f));
        g.setPaint(new GradientPaint(0f, (float) (base - d.height * s), svetla, 0f, (float) base, tmava));
        g.fill(telo);
        g.setColor(new Color(mixRgb(rgb, 0x000000, 0.55f)));
        g.setStroke(new BasicStroke(1f));
        g.draw(telo);

        g.setColor(new Color(0x27384F));
        for (double[][] o : okna) {
            Path2D.Double w = new Path2D.Double();
            for (int i = 0; i < o.length; i++) {
                double x = x0 + o[i][0] * s, y = base - o[i][1] * s;
                if (i == 0) w.moveTo(x, y);
                else w.lineTo(x, y);
            }
            w.closePath();
            g.fill(w);
        }

        for (double xc : d.wheelX) {
            double cx = x0 + xc * s, cy = base - d.r * s, r = d.r * s;
            g.setColor(new Color(0x1E1E22));
            g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
            g.setColor(new Color(0xC2C8D2));
            g.fill(new Ellipse2D.Double(cx - r * 0.62, cy - r * 0.62, r * 1.24, r * 1.24));
            g.setColor(new Color(0x4A4F5A));
            g.fill(new Ellipse2D.Double(cx - r * 0.22, cy - r * 0.22, r * 0.44, r * 0.44));
        }

        int n = P.length;
        double zf = P[n - 1][1] + (P[n - 2][1] - P[n - 1][1]) * 0.6;
        g.setColor(new Color(0xFFF4C8));
        g.fill(new Ellipse2D.Double(x0 + (P[n - 1][0] - 0.30) * s, base - zf * s - 0.06 * s, 0.28 * s, 0.12 * s));
        double zr = P[0][1] + (P[1][1] - P[0][1]) * 0.6;
        g.setColor(new Color(0xD01820));
        g.fill(new Ellipse2D.Double(x0 + (P[0][0] + 0.02) * s, base - zr * s - 0.06 * s, 0.26 * s, 0.12 * s));
        g.dispose();
    }
}