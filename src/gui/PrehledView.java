package gui;

import data.Auto;
import data.Garaz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Úvodní přehled: čísla, co řešit dřív, upozornění, náklady za rok a blížící se STK. */
class PrehledView extends Pohled {

    PrehledView(MainFrame app) {
        super(app);
    }

    @Override
    String nazev() {
        return "Přehled";
    }

    @Override
    String ikona() {
        return "dashboard";
    }

    @Override
    String podtitul() {
        String den = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE d. MMMM yyyy",
                Locale.forLanguageTag("cs-CZ")));
        return "Stav vozového parku k " + den + ". Kliknutím na auto otevřeš jeho detail.";
    }

    @Override
    JComponent akce() {
        return Ui.radek(8,
                Ui.sekundarni("Plán servisu", "wrench", () -> app.prepni("servis")),
                Ui.sekundarni("Export CSV", "download", app::exportovatCsv));
    }

    @Override
    void postav() {
        if (g.jePrazdna()) {
            blok(prazdno("car", "Garáž je zatím prázdná", "Přidej první auto a aplikace začne počítat priority,"
                    + " náklady a termíny.", Ui.primarni("Přidat auto", "plus", app::pridatAuto)));
            return;
        }
        blok(kpi());
        blok(vedle(18, new double[]{1.45, 1}, coResit(), upozorneni()));
        blok(vedle(18, new double[]{1.45, 1}, naklady(), stk()));
        blok(vedle(18, new double[]{1.45, 1}, historie(), palivo()));
    }

    private JComponent kpi() {
        int krit = 0, vysoka = 0;
        for (Auto a : g.getAuta()) {
            if (g.getPriorita(a) == Auto.Priorita.KRITICKA) krit++;
            if (g.getPriorita(a) == Auto.Priorita.VYSOKA) vysoka++;
        }
        YearMonth ted = YearMonth.now();
        float[] tento = g.getNakladyMesic(ted), minuly = g.getNakladyMesic(ted.minusMonths(1));
        float t = tento[0] + tento[1], m = minuly[0] + minuly[1];
        String srovnani;
        Color barvaSrovnani;
        if (m <= 0) {
            srovnani = "minulý měsíc bez nákladů";
            barvaSrovnani = null;
        } else {
            int proc = Math.round((t - m) / m * 100);
            srovnani = (proc > 0 ? "+" : "") + proc + " % proti " + ted.minusMonths(1).getMonth()
                    .getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("cs-CZ"));
            barvaSrovnani = proc > 0 ? Ui.crit() : Ui.ok();
        }
        List<Garaz.Upozorneni> up = g.getUpozorneni();
        int kritUp = 0;
        for (Garaz.Upozorneni u : up) if (u.zavaznost == Garaz.Zavaznost.KRITICKE) kritUp++;
        int plan = 0;
        for (Garaz.PlanPolozka p : g.planRozpoctu(Integer.MAX_VALUE)) plan += p.cena;
        float zbyva = g.getRocniRozpocet() - g.getServisLetos();

        return vedle(14, null,
                Ui.kpi("Vozidla", String.valueOf(g.getPocetAut()),
                        krit + " kritických · " + vysoka + " vysokých", krit > 0 ? Ui.danger() : null, "car"),
                Ui.kpi("Náklady tento měsíc", Ui.kc(t), srovnani, barvaSrovnani, "coins"),
                Ui.kpi("K řešení", String.valueOf(up.size()), kritUp + " kritických upozornění",
                        kritUp > 0 ? Ui.danger() : Ui.ok(), "bell"),
                Ui.kpi("Servis letos", Ui.kc(g.getServisLetos()), "z rozpočtu zbývá " + Ui.kc(Math.max(0, zbyva)),
                        zbyva < 0 ? Ui.danger() : null, "wrench"),
                Ui.kpi("Servis všech aut", Ui.kc(plan), "odhad, kdyby šla všechna teď", null, "calendar"));
    }

    private JComponent coResit() {
        JPanel seznam = Ui.pruhledny(new GridLayout(0, 1, 0, 8));
        List<Auto> z = g.getZebricek();
        for (int i = 0; i < Math.min(5, z.size()); i++) {
            Auto a = z.get(i);
            seznam.add(radekAuta(a, true, () -> app.otevriAuto(a)));
        }
        Ui.Tlacitko vse = Ui.duch("Celý žebříček", "chevron-right", () -> app.prepni("priorita"));
        return Ui.sekce("Co řešit dřív", vse, seznam);
    }

    private JComponent upozorneni() {
        List<Garaz.Upozorneni> up = g.getUpozorneni();
        JPanel seznam = Ui.pruhledny(null);
        seznam.setLayout(new BoxLayout(seznam, BoxLayout.Y_AXIS));
        if (up.isEmpty()) {
            JLabel l = Ui.muted("Žádné auto teď nic nepotřebuje.");
            seznam.add(l);
        }
        for (int i = 0; i < Math.min(6, up.size()); i++) {
            Garaz.Upozorneni u = up.get(i);
            seznam.add(MainFrame.radekUpozorneni(u, () -> app.otevriAuto(u.auto)));
        }
        if (up.size() > 6) {
            JLabel dalsi = Ui.muted("+ dalších " + (up.size() - 6) + " v ikoně zvonku nahoře", 12);
            dalsi.setBorder(new EmptyBorder(6, 8, 0, 0));
            seznam.add(dalsi);
        }
        return Ui.sekce("Upozornění", new Ui.Stitek(up.size() + " aktivních", up.isEmpty() ? Ui.ok() : Ui.crit()), seznam);
    }

    private JComponent naklady() {
        YearMonth ted = YearMonth.now();
        String[] popisky = new String[12];
        double[][] rady = new double[2][12];
        double celkem = 0;
        for (int i = 0; i < 12; i++) {
            YearMonth m = ted.minusMonths(11 - i);
            popisky[i] = m.getMonth().getDisplayName(TextStyle.SHORT_STANDALONE, Locale.forLanguageTag("cs-CZ"));
            float[] n = g.getNakladyMesic(m);
            rady[0][i] = n[0];
            rady[1][i] = n[1];
            celkem += n[0] + n[1];
        }
        Grafy.Sloupce graf = new Grafy.Sloupce(popisky, rady, new String[]{"Palivo", "Servis"},
                new Color[]{Ui.accent(), Ui.warn()}, "Kč");
        graf.setPreferredSize(new Dimension(500, 250));
        JLabel vpravo = Ui.muted("celkem " + Ui.kc(celkem) + " za 12 měsíců", 12.5f);
        return Ui.sekce("Náklady za poslední rok", vpravo, graf);
    }

    private JComponent stk() {
        List<Auto> auta = new ArrayList<>(g.getAuta());
        auta.sort((a, b) -> Long.compare(a.getDniDoStk(), b.getDniDoStk()));
        JPanel seznam = Ui.pruhledny(new GridLayout(0, 1, 0, 2));
        for (int i = 0; i < Math.min(6, auta.size()); i++) {
            Auto a = auta.get(i);
            long d = a.getDniDoStk();
            Ui.Karta r = new Ui.Karta(new BorderLayout(10, 0), 7, 8).klik(() -> app.otevriAuto(a));
            r.vypln(Ui.surface());
            r.radius = 10;
            JPanel vlevo = Ui.pruhledny(new FlowLayout(FlowLayout.LEFT, 10, 0));
            vlevo.add(new Ui.Spz(a.getSpz()));
            vlevo.add(Ui.silny(a.getJmenoAuta(), 13.5f));
            r.add(vlevo, BorderLayout.CENTER);
            JPanel vpravo = Ui.pruhledny(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            vpravo.add(Ui.muted(MainFrame.datum(a.getStkDo()), 12.5f));
            vpravo.add(new Ui.Stitek(d < 0 ? "prošlá" : dny(d), barvaStk(d)));
            r.add(vpravo, BorderLayout.EAST);
            seznam.add(r);
        }
        return Ui.sekce("Technická kontrola", Ui.duch("Kalendář", "calendar", () -> app.prepni("servis")), seznam);
    }

    private JComponent historie() {
        List<Garaz.Zaznam> h = g.getHistorie();
        Collections.reverse(h);
        JPanel seznam = Ui.pruhledny(new GridLayout(0, 1, 0, 6));
        if (h.isEmpty()) seznam.add(Ui.muted("Zatím žádné změny."));
        for (int i = 0; i < Math.min(6, h.size()); i++) {
            Garaz.Zaznam z = h.get(i);
            JPanel r = Ui.pruhledny(new BorderLayout(12, 0));
            JLabel cas = Ui.muted(z.getCasText(), 12);
            cas.setPreferredSize(new Dimension(118, 10));
            r.add(cas, BorderLayout.WEST);
            r.add(Ui.text(z.getText()), BorderLayout.CENTER);
            seznam.add(r);
        }
        return Ui.sekce("Poslední změny", Ui.duch("Celá historie", "history", () -> app.prepni("historie")), seznam);
    }

    private JComponent palivo() {
        List<Auto> auta = new ArrayList<>(g.getAuta());
        auta.sort((a, b) -> Float.compare(a.getNaplneni(), b.getNaplneni()));
        JPanel seznam = Ui.pruhledny(new GridLayout(0, 1, 0, 10));
        for (int i = 0; i < Math.min(6, auta.size()); i++) {
            Auto a = auta.get(i);
            JPanel r = Ui.pruhledny(new BorderLayout(12, 0));
            JLabel j = Ui.text(a.getJmenoAuta());
            j.setPreferredSize(new Dimension(118, 10));
            r.add(j, BorderLayout.WEST);
            r.add(new Ui.Ukazatel(a.getNaplneni(), Ui.stav(a.getNaplneni()), 8), BorderLayout.CENTER);
            JLabel l = Ui.muted(Math.round(a.getNaplneni() * 100) + " % · " + Math.round(a.getDojezd()) + " km", 12);
            l.setPreferredSize(new Dimension(96, 10));
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            r.add(l, BorderLayout.EAST);
            seznam.add(r);
        }
        return Ui.sekce("Nádrže", Ui.muted("dojezd na aktuální palivo", 12), seznam);
    }
}
