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
import java.util.List;
import java.util.Locale;

/**
 * Servis a rozpočet: kolik aut se vejde do ročního rozpočtu (od nejvyšší priority),
 * kalendář STK na rok dopředu a servisní kniha celé garáže. Ceny jsou odhad, ne nabídka servisu.
 */
class ServisView extends Pohled {

    private int rozpocet = -1;
    private JPanel plan;
    private JPanel souhrn;

    ServisView(MainFrame app) {
        super(app);
    }

    @Override
    String nazev() {
        return "Servis a rozpočet";
    }

    @Override
    String ikona() {
        return "wrench";
    }

    @Override
    String podtitul() {
        return "Na co rozpočet stačí, když se servisuje od nejvyšší priority. Ceny jsou odhad aplikace.";
    }

    @Override
    JComponent akce() {
        return Ui.radek(8, Ui.sekundarni("Změnit rozpočet", "settings", app::nastaveni));
    }

    @Override
    void postav() {
        if (g.jePrazdna()) {
            blok(prazdno("wrench", "Žádná auta", "Plán servisu se sestaví, až budou v garáži auta.", null));
            return;
        }
        if (rozpocet < 0) rozpocet = g.getRocniRozpocet();
        souhrn = Ui.pruhledny(new BorderLayout());
        plan = Ui.pruhledny(new GridLayout(0, 1, 0, 6));
        blok(planovac());
        prepocitej();
        blok(vedle(18, new double[]{1.3, 1}, kalendar(), kniha()));
    }

    private JComponent planovac() {
        JSlider s = Ui.posuvnik(0, 200, Math.min(200, rozpocet / 1000));
        JLabel hodnota = Ui.label(Ui.kc(rozpocet), Font.BOLD, 22, Ui.text());
        hodnota.setFont(Ui.serif(Font.BOLD, 26));
        s.addChangeListener(e -> {
            rozpocet = s.getValue() * 1000;
            hodnota.setText(Ui.kc(rozpocet));
            prepocitej();
        });
        Ui.Tlacitko ulozit = Ui.duch("Uložit jako roční rozpočet", "save", () -> {
            g.setRocniRozpocet(rozpocet);
            g.zapis("Roční rozpočet na servis nastaven na " + Ui.kc(rozpocet));
            app.zmena("Rozpočet uložen: " + Ui.kc(rozpocet));
        });

        JPanel hlava = Ui.pruhledny(new BorderLayout(18, 0));
        JPanel vl = Ui.pruhledny(new GridLayout(2, 1, 0, 2));
        vl.add(Ui.eyebrow("Rozpočet"));
        vl.add(hodnota);
        hlava.add(vl, BorderLayout.WEST);
        JPanel sObal = Ui.pruhledny(new BorderLayout(0, 2));
        sObal.add(s, BorderLayout.CENTER);
        JPanel osa = Ui.pruhledny(new BorderLayout());
        osa.add(Ui.muted("0 Kč", 11.5f), BorderLayout.WEST);
        osa.add(Ui.muted("200 000 Kč", 11.5f), BorderLayout.EAST);
        sObal.add(osa, BorderLayout.SOUTH);
        hlava.add(sObal, BorderLayout.CENTER);
        JPanel u = Ui.pruhledny(new GridBagLayout());
        u.add(ulozit);
        hlava.add(u, BorderLayout.EAST);

        JPanel obsah = Ui.pruhledny(new BorderLayout(0, 16));
        obsah.add(hlava, BorderLayout.NORTH);
        obsah.add(souhrn, BorderLayout.CENTER);
        obsah.add(plan, BorderLayout.SOUTH);
        JPanel vpravo = Ui.pruhledny(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        vpravo.add(Ui.odhad("odhad ceny"));
        return Ui.sekce("Plán servisu do rozpočtu", vpravo, obsah);
    }

    private void prepocitej() {
        List<Garaz.PlanPolozka> p = g.planRozpoctu(rozpocet);
        int vejde = 0, cena = 0, zbyva = 0, celkem = 0;
        for (Garaz.PlanPolozka x : p) {
            celkem += x.cena;
            if (x.vejdeSe) {
                vejde++;
                cena += x.cena;
            } else {
                zbyva += x.cena;
            }
        }
        souhrn.removeAll();
        souhrn.add(vedle(12, null,
                Ui.kpi("Vejde se", vejde + " z " + p.size(), "aut dostane servis", vejde == p.size() ? Ui.ok() : null, "check"),
                Ui.kpi("Plán stojí", Ui.kc(cena), "zbude " + Ui.kc(rozpocet - cena), null, "coins"),
                Ui.kpi("Chybí", Ui.kc(zbyva), zbyva == 0 ? "rozpočet stačí na vše" : "na zbylá auta",
                        zbyva == 0 ? Ui.ok() : Ui.crit(), "alert"),
                Ui.kpi("Všechna auta", Ui.kc(celkem), "kdyby šla všechna najednou", null, "wrench")));

        plan.removeAll();
        JPanel hlavicka = Ui.pruhledny(new BorderLayout(12, 0));
        hlavicka.setBorder(new EmptyBorder(0, 12, 0, 12));
        JLabel h1 = Ui.label("AUTO (PODLE PRIORITY)", Font.BOLD, 11, Ui.muted());
        hlavicka.add(h1, BorderLayout.WEST);
        JLabel h2 = Ui.label("ODHAD   ·   ČERPÁNÍ ROZPOČTU", Font.BOLD, 11, Ui.muted());
        hlavicka.add(h2, BorderLayout.EAST);
        plan.add(hlavicka);
        int maxKum = Math.max(rozpocet, 1);
        for (Garaz.PlanPolozka x : p) {
            Ui.Karta r = new Ui.Karta(new BorderLayout(12, 0), 8, 12).klik(() -> app.otevriAuto(x.auto));
            r.radius = 10;
            r.vypln(x.vejdeSe ? Ui.surface() : Ui.surface2());
            r.prouzek(Ui.priorita(g.getPriorita(x.auto)));
            JPanel vl = Ui.pruhledny(new FlowLayout(FlowLayout.LEFT, 10, 0));
            JLabel por = Ui.label(String.valueOf(g.getPoradi(x.auto)), Font.BOLD, 14, Ui.muted());
            por.setFont(Ui.font(Font.BOLD, 14));
            vl.add(por);
            vl.add(Ui.silny(x.auto.getJmenoAuta(), 13.5f));
            vl.add(Ui.muted(x.auto.getSpz(), 12));
            vl.add(Ui.stitekPriority(g.getPriorita(x.auto)));
            if (x.auto.getDniDoStk() < 60) vl.add(new Ui.Stitek("+ STK", Ui.warn()));
            if (x.auto.getOpotrebeniServisu() > 1) vl.add(new Ui.Stitek("přetažený interval", Ui.danger()));
            r.add(vl, BorderLayout.CENTER);
            JPanel vp = Ui.pruhledny(new FlowLayout(FlowLayout.RIGHT, 12, 0));
            vp.add(Ui.silny(Ui.kc(x.cena), 13.5f));
            Ui.Ukazatel u = new Ui.Ukazatel(Math.min(1f, x.kumulace / (float) maxKum),
                    x.vejdeSe ? Ui.accent() : Ui.lineStrong(), 8);
            u.setPreferredSize(new Dimension(140, 8));
            vp.add(u);
            vp.add(new Ui.Stitek(x.vejdeSe ? "vejde se" : "čeká", x.vejdeSe ? Ui.ok() : Ui.muted()));
            r.add(vp, BorderLayout.EAST);
            plan.add(r);
        }
        souhrn.revalidate();
        souhrn.repaint();
        plan.revalidate();
        plan.repaint();
    }

    private JComponent kalendar() {
        JPanel mrizka = Ui.pruhledny(new GridLayout(0, 4, 10, 10));
        YearMonth ted = YearMonth.now();
        Locale cz = Locale.forLanguageTag("cs-CZ");
        List<Auto> prosle = new ArrayList<>();
        for (Auto a : g.getAuta()) {
            if (a.getStkDo() != null && a.getStkDo().isBefore(LocalDate.now())) prosle.add(a);
        }
        for (int i = 0; i < 12; i++) {
            YearMonth m = ted.plusMonths(i);
            List<Auto> v = new ArrayList<>();
            if (i == 0) v.addAll(prosle);
            for (Auto a : g.getAuta()) {
                if (a.getStkDo() != null && YearMonth.from(a.getStkDo()).equals(m) && !prosle.contains(a)) v.add(a);
            }
            Ui.Karta k = new Ui.Karta(new BorderLayout(0, 6), 10, 12);
            k.radius = 10;
            k.vypln(i == 0 ? Ui.accentSoft() : Ui.surface2());
            String n = m.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, cz);
            JLabel nadpis = Ui.label(n.substring(0, 1).toUpperCase(cz) + n.substring(1) + " " + m.getYear(),
                    Font.BOLD, 12.5f, i == 0 ? Ui.accent() : Ui.text2());
            k.add(nadpis, BorderLayout.NORTH);
            JPanel auta = Ui.pruhledny(new GridLayout(0, 1, 0, 3));
            if (v.isEmpty()) auta.add(Ui.muted("—", 12));
            for (Auto a : v) {
                long d = a.getDniDoStk();
                JLabel l = Ui.label(a.getJmenoAuta() + (d < 0 ? " (prošlá)" : " " + a.getStkDo().getDayOfMonth() + "."),
                        Font.BOLD, 12, barvaStk(d));
                l.setIcon(new Ui.Ikona("calendar", 11, barvaStk(d)));
                auta.add(l);
            }
            k.add(auta, BorderLayout.CENTER);
            mrizka.add(k);
        }
        return Ui.sekce("Kalendář STK na 12 měsíců", Ui.muted("prošlé STK jsou v aktuálním měsíci", 12), mrizka);
    }

    private JComponent kniha() {
        List<Garaz.Servis> s = g.getServisy();
        s.sort((a, b) -> b.getDatum().compareTo(a.getDatum()));
        JPanel obsah = Ui.pruhledny(new GridLayout(0, 1, 0, 8));
        if (s.isEmpty()) obsah.add(Ui.muted("Žádný zapsaný servis."));
        for (int i = 0; i < Math.min(10, s.size()); i++) {
            Garaz.Servis x = s.get(i);
            Auto a = g.najdiAuto(x.getAutoId());
            JPanel r = Ui.pruhledny(new BorderLayout(10, 0));
            JLabel d = Ui.muted(MainFrame.datum(x.getDatum()), 12.5f);
            d.setPreferredSize(new Dimension(84, 10));
            r.add(d, BorderLayout.WEST);
            JPanel t = Ui.pruhledny(new GridLayout(2, 1));
            t.add(Ui.silny(x.getPopis(), 13));
            t.add(Ui.muted(a == null ? "odebrané auto" : a.getJmenoAuta() + " · " + a.getSpz(), 12));
            r.add(t, BorderLayout.CENTER);
            r.add(Ui.silny(Ui.kc(x.getCena()), 13), BorderLayout.EAST);
            obsah.add(r);
        }
        Auto akt = app.aktualni();
        Ui.Tlacitko zapsat = Ui.duch("Zapsat servis", "plus", () -> app.servis(app.aktualni()));
        zapsat.setToolTipText(akt == null ? "" : "Pro " + akt.getJmenoAuta());
        JPanel o = Ui.pruhledny(new BorderLayout(0, 12));
        o.add(Ui.muted("Letos za servis " + Ui.kc(g.getServisLetos()) + " z rozpočtu " + Ui.kc(g.getRocniRozpocet()), 12.5f),
                BorderLayout.NORTH);
        o.add(obsah, BorderLayout.CENTER);
        return Ui.sekce("Servisní kniha garáže", zapsat, o);
    }
}
