package gui;

import data.Auto;
import data.Garaz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Náklady: palivo a servis po měsících, srovnání aut, cena za km a kniha tankování. */
class NakladyView extends Pohled {

    private int obdobi = 1;   // 0 = 6 měsíců, 1 = 12 měsíců, 2 = 24 měsíců
    private static final int[] MESICU = {6, 12, 24};

    NakladyView(MainFrame app) {
        super(app);
    }

    @Override
    String nazev() {
        return "Náklady";
    }

    @Override
    String ikona() {
        return "coins";
    }

    @Override
    String podtitul() {
        return "Kolik garáž stojí. Palivo se počítá z knihy tankování, servis ze servisní knihy.";
    }

    @Override
    JComponent akce() {
        Ui.Prepinac p = new Ui.Prepinac(new String[]{"6 měsíců", "12 měsíců", "24 měsíců"}, obdobi);
        p.naZmenu(i -> {
            obdobi = i;
            obnov();
        });
        return Ui.radek(10, p, Ui.sekundarni("Cena paliva", "fuel", app::nastaveni));
    }

    @Override
    void postav() {
        int n = MESICU[obdobi];
        YearMonth ted = YearMonth.now();
        Locale cz = Locale.forLanguageTag("cs-CZ");
        String[] popisky = new String[n];
        double[][] rady = new double[2][n];
        double palivo = 0, servis = 0, nejvic = 0;
        String nejdrazsi = "—";
        for (int i = 0; i < n; i++) {
            YearMonth m = ted.minusMonths(n - 1 - i);
            String kratky = m.getMonth().getDisplayName(TextStyle.SHORT_STANDALONE, cz);
            popisky[i] = n > 12 && i % 2 == 1 ? "" : kratky;
            float[] x = g.getNakladyMesic(m);
            rady[0][i] = x[0];
            rady[1][i] = x[1];
            palivo += x[0];
            servis += x[1];
            if (x[0] + x[1] > nejvic) {
                nejvic = x[0] + x[1];
                nejdrazsi = m.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, cz) + " " + m.getYear();
            }
        }
        double litru = 0;
        int tankovani = 0;
        for (Garaz.Tankovani t : g.getTankovani()) {
            if (!YearMonth.from(t.getDatum()).isBefore(ted.minusMonths(n - 1))) {
                litru += t.getLitry();
                tankovani++;
            }
        }
        blok(vedle(14, null,
                Ui.kpi("Celkem", Ui.kc(palivo + servis), "za " + n + " měsíců", null, "coins"),
                Ui.kpi("Palivo", Ui.kc(palivo), Ui.tis(litru) + " l · " + tankovani + " tankování", null, "fuel"),
                Ui.kpi("Servis", Ui.kc(servis), "ze servisní knihy", null, "wrench"),
                Ui.kpi("Průměr měsíčně", Ui.kc((palivo + servis) / n), "nejdražší " + nejdrazsi, null, "calendar")));

        Grafy.Sloupce graf = new Grafy.Sloupce(popisky, rady, new String[]{"Palivo", "Servis"},
                new Color[]{Ui.accent(), Ui.warn()}, "Kč");
        graf.setPreferredSize(new Dimension(600, 280));
        blok(Ui.sekce("Náklady po měsících", Ui.muted("najetím myší uvidíš přesné částky", 12), graf));

        blok(vedle(18, new double[]{1, 1}, podleAut(), cenaZaKm()));
        blok(tankovani());
    }

    private JComponent podleAut() {
        List<Auto> auta = new ArrayList<>(g.getAuta());
        auta.sort((a, b) -> {
            float[] x = g.getNakladyAuta(a), y = g.getNakladyAuta(b);
            return Float.compare(y[0] + y[1], x[0] + x[1]);
        });
        String[] p = new String[auta.size()];
        double[] h = new double[auta.size()];
        String[] t = new String[auta.size()];
        Color[] b = new Color[auta.size()];
        for (int i = 0; i < auta.size(); i++) {
            float[] n = g.getNakladyAuta(auta.get(i));
            p[i] = auta.get(i).getJmenoAuta();
            h[i] = n[0] + n[1];
            t[i] = Ui.kc(h[i]);
            b[i] = Ui.priorita(g.getPriorita(auta.get(i)));
        }
        return Ui.sekce("Náklady podle aut", Ui.muted("palivo + servis, celá historie", 12), new Grafy.Pruhy(p, h, t, b));
    }

    private JComponent cenaZaKm() {
        List<Auto> auta = new ArrayList<>(g.getAuta());
        auta.sort((a, b) -> Float.compare(g.getCenaZaKm(b), g.getCenaZaKm(a)));
        String[] p = new String[auta.size()];
        double[] h = new double[auta.size()];
        String[] t = new String[auta.size()];
        Color[] b = new Color[auta.size()];
        Color[] druhy = {Ui.accent(), Ui.text2(), Ui.ok(), Ui.yellow()};
        for (int i = 0; i < auta.size(); i++) {
            Auto a = auta.get(i);
            p[i] = a.getJmenoAuta();
            h[i] = g.getCenaZaKm(a);
            t[i] = MainFrame.format(Math.round(h[i] * 100) / 100f) + " Kč/km · " + a.getDruhPaliva();
            b[i] = druhy[a.getDruhPaliva().ordinal() % druhy.length];
        }
        JPanel o = Ui.pruhledny(new BorderLayout(0, 10));
        o.add(new Grafy.Pruhy(p, h, t, b), BorderLayout.CENTER);
        JPanel legenda = Ui.pruhledny(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (Auto.Palivo pal : Auto.Palivo.values()) {
            legenda.add(new Ui.Stitek(pal + " " + MainFrame.format(Math.round(g.getCenaPaliva() * pal.getKoeficientCeny() * 10) / 10f) + " Kč/l",
                    druhy[pal.ordinal() % druhy.length]));
        }
        o.add(legenda, BorderLayout.SOUTH);
        JPanel vpravo = Ui.pruhledny(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        vpravo.add(Ui.odhad("dle spotřeby"));
        return Ui.sekce("Cena za 1 km (palivo)", vpravo, o);
    }

    private JComponent tankovani() {
        List<Garaz.Tankovani> t = g.getTankovani();
        t.sort((a, b) -> b.getDatum().compareTo(a.getDatum()));
        JPanel tab = Ui.pruhledny(new GridLayout(0, 5, 12, 8));
        for (String h : new String[]{"Datum", "Auto", "Litrů", "Kč/l", "Celkem"}) {
            tab.add(Ui.label(h.toUpperCase(), Font.BOLD, 11, Ui.muted()));
        }
        for (int i = 0; i < Math.min(14, t.size()); i++) {
            Garaz.Tankovani x = t.get(i);
            Auto a = g.najdiAuto(x.getAutoId());
            tab.add(Ui.muted(MainFrame.datum(x.getDatum()), 13));
            tab.add(Ui.silny(a == null ? "odebrané auto" : a.getJmenoAuta(), 13));
            tab.add(Ui.text(MainFrame.format(Math.round(x.getLitry() * 10) / 10f) + " l"));
            tab.add(Ui.text(MainFrame.format(Math.round(x.getCenaZaLitr() * 100) / 100f)));
            tab.add(Ui.silny(Ui.kc(x.getCena()), 13));
        }
        JPanel o = Ui.pruhledny(new BorderLayout());
        o.add(tab, BorderLayout.NORTH);
        o.setBorder(new EmptyBorder(0, 0, 0, 0));
        return Ui.sekce("Poslední tankování", Ui.muted(t.size() + " záznamů celkem · zobrazeno 14 nejnovějších", 12), o);
    }
}
