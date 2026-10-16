package gui;

import data.Auto;
import data.Garaz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Seznam aut vlevo (hledání, filtr podle třídy, řazení) a detail vybraného auta vpravo. */
class VozidlaView extends Pohled {

    private static final String[] FILTRY = {"Vše", "Kritická", "Vysoká", "Střední", "Nízká"};
    private static final String[] RAZENI = {"Podle priority", "Podle jména", "Podle STK", "Podle paliva", "Podle stáří"};

    private String hledano = "";
    private int filtr = 0;
    private int razeni = 0;
    private JPanel seznam;
    private JScrollPane detailPosuv;

    VozidlaView(MainFrame app) {
        super(app);
    }

    @Override
    String nazev() {
        return "Vozidla";
    }

    @Override
    String ikona() {
        return "car";
    }

    @Override
    String podtitul() {
        return g.getPocetAut() + " aut v garáži. Vyber auto vlevo, vpravo uvidíš všechno o něm a co s ním udělat.";
    }

    @Override
    JComponent akce() {
        return Ui.radek(8, Ui.sekundarni("Export CSV", "download", app::exportovatCsv),
                Ui.primarni("Přidat auto", "plus", app::pridatAuto));
    }

    @Override
    void postav() {
        // nepoužívá se, tělo má vlastní rozložení
    }

    @Override
    JComponent telo() {
        if (g.jePrazdna()) {
            JPanel p = Ui.pruhledny(new BorderLayout());
            p.setBorder(new EmptyBorder(4, 30, 30, 30));
            p.add(prazdno("car", "Žádná auta", "Přidej první auto tlačítkem vpravo nahoře nebo Ctrl+N.",
                    Ui.primarni("Přidat auto", "plus", app::pridatAuto)), BorderLayout.NORTH);
            return p;
        }
        JPanel p = Ui.pruhledny(new BorderLayout(18, 0));
        p.setBorder(new EmptyBorder(4, 30, 0, 22));
        p.add(levySloupec(), BorderLayout.WEST);
        int pozice = detailPosuv == null ? 0 : detailPosuv.getVerticalScrollBar().getValue();
        JPanel detail = new Ui.SirkaPanel(new BorderLayout());
        detail.setBorder(new EmptyBorder(0, 0, 24, 8));
        detail.add(detail(app.aktualni()), BorderLayout.NORTH);
        detailPosuv = Ui.scroll(detail);
        JScrollPane sp = detailPosuv;
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> sp.getVerticalScrollBar().setValue(pozice)));
        p.add(detailPosuv, BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------------------
    // seznam
    // ------------------------------------------------------------------

    private JComponent levySloupec() {
        JPanel s = Ui.pruhledny(new BorderLayout(0, 10));
        s.setPreferredSize(new Dimension(350, 10));

        JTextField hledej = Ui.hledani("Hledat jméno, značku, SPZ…");
        hledej.setText(hledano);
        hledej.getDocument().addDocumentListener(MainFrame.zmenaTextu(() -> {
            hledano = hledej.getText();
            naplnSeznam();
        }));
        Ui.Prepinac f = new Ui.Prepinac(FILTRY, filtr);
        f.naZmenu(i -> {
            filtr = i;
            naplnSeznam();
        });
        JComboBox<String> r = Ui.vyber(RAZENI);
        r.setSelectedIndex(razeni);
        r.addActionListener(e -> {
            razeni = r.getSelectedIndex();
            naplnSeznam();
        });
        JPanel ovladani = Ui.pruhledny(new GridLayout(0, 1, 0, 8));
        ovladani.add(hledej);
        JPanel fObal = Ui.pruhledny(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fObal.add(f);
        ovladani.add(fObal);
        ovladani.add(r);

        seznam = new Ui.SirkaPanel(new GridBagLayout());
        seznam.setBorder(new EmptyBorder(0, 0, 20, 6));
        naplnSeznam();
        s.add(ovladani, BorderLayout.NORTH);
        s.add(Ui.scroll(seznam), BorderLayout.CENTER);
        return s;
    }

    private List<Auto> vyfiltrovana() {
        String q = MainFrame.bezDiakritiky(hledano.trim().toLowerCase(Locale.ROOT));
        List<Auto> v = new ArrayList<>();
        for (Auto a : g.getZebricek()) {
            if (filtr > 0 && g.getPriorita(a).ordinal() != filtr - 1) continue;
            String h = MainFrame.bezDiakritiky((a.getJmenoAuta() + " " + a.getZnacka() + " " + a.getSpz() + " "
                    + a.getDruhPaliva()).toLowerCase(Locale.ROOT));
            if (!q.isEmpty() && !h.contains(q)) continue;
            v.add(a);
        }
        Comparator<Auto> c = null;
        switch (razeni) {
            case 1:
                c = Comparator.comparing(a -> a.getJmenoAuta().toLowerCase(Locale.ROOT));
                break;
            case 2:
                c = Comparator.comparingLong(Auto::getDniDoStk);
                break;
            case 3:
                c = Comparator.comparingDouble(Auto::getNaplneni);
                break;
            case 4:
                c = Comparator.comparingInt(Auto::getRokVyroby);
                break;
            default:
                break;
        }
        if (c != null) v.sort(c);
        return v;
    }

    private void naplnSeznam() {
        seznam.removeAll();
        List<Auto> v = vyfiltrovana();
        Auto akt = app.aktualni();
        int y = 0;
        for (Auto a : v) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = y++;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 8, 0);
            seznam.add(polozka(a, a == akt), c);
        }
        if (v.isEmpty()) {
            JLabel l = Ui.muted("Nic neodpovídá hledání.");
            l.setBorder(new EmptyBorder(12, 6, 0, 0));
            seznam.add(l, new GridBagConstraints());
            y++;
        }
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.weighty = 1;
        seznam.add(Box.createGlue(), c);
        seznam.revalidate();
        seznam.repaint();
    }

    private JComponent polozka(Auto a, boolean vybrana) {
        Ui.Karta k = new Ui.Karta(new BorderLayout(12, 0), 11, 12);
        k.radius = 12;
        k.prouzek(Ui.priorita(g.getPriorita(a)));
        if (vybrana) k.vypln(Ui.accentSoft());
        k.klik(() -> {
            app.vyber(a);
            obnov();
        });
        k.add(new Ui.Avatar(a, 40), BorderLayout.WEST);
        JPanel t = Ui.pruhledny(new GridLayout(3, 1, 0, 1));
        JPanel j = Ui.pruhledny(new BorderLayout(6, 0));
        j.add(Ui.label(a.getJmenoAuta(), Font.BOLD, 14, vybrana ? Ui.accent() : Ui.text()), BorderLayout.CENTER);
        j.add(Ui.label(String.valueOf(Math.round(g.getSkore(a))), Font.BOLD, 13, Ui.text2()), BorderLayout.EAST);
        t.add(j);
        t.add(Ui.muted(a.getZnacka() + " · " + a.getSpz() + " · " + (a.getRokVyroby() > 0 ? a.getRokVyroby() : "—"), 12));
        JPanel stav = Ui.pruhledny(new FlowLayout(FlowLayout.LEFT, 6, 0));
        stav.add(new JLabel(new Ui.Ikona("fuel", 12, Ui.stav(a.getNaplneni()))));
        stav.add(Ui.label(Math.round(a.getNaplneni() * 100) + " %", Font.PLAIN, 11.5f, Ui.muted()));
        stav.add(new JLabel(new Ui.Ikona("wrench", 12, barvaServisu(a.getOpotrebeniServisu()))));
        stav.add(Ui.label(Math.round(a.getOpotrebeniServisu() * 100) + " %", Font.PLAIN, 11.5f, Ui.muted()));
        stav.add(new JLabel(new Ui.Ikona("calendar", 12, barvaStk(a.getDniDoStk()))));
        stav.add(Ui.label("STK " + (a.getDniDoStk() < 0 ? "prošlá" : dny(a.getDniDoStk())), Font.PLAIN, 11.5f,
                a.getDniDoStk() <= 30 ? barvaStk(a.getDniDoStk()) : Ui.muted()));
        t.add(stav);
        k.add(t, BorderLayout.CENTER);
        return k;
    }

    // ------------------------------------------------------------------
    // detail
    // ------------------------------------------------------------------

    private JComponent detail(Auto a) {
        if (a == null) return Ui.muted("Vyber auto.");
        JPanel s = new Ui.SirkaPanel(new GridBagLayout());
        int[] y = {0};
        java.util.function.Consumer<JComponent> pridej = c -> {
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridy = y[0]++;
            gc.weightx = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.insets = new Insets(0, 0, 16, 0);
            s.add(uzky(c), gc);
        };
        pridej.accept(hlavicka(a));
        pridej.accept(vedle(16, null, nadrz(a), servis(a), stk(a)));
        pridej.accept(skore(a));
        pridej.accept(vedle(16, new double[]{1, 1}, naklady(a), knihaTankovani(a)));
        pridej.accept(vedle(16, new double[]{1, 1}, knihaServisu(a), poznamka(a)));
        GridBagConstraints konec = new GridBagConstraints();
        konec.gridy = y[0];
        konec.weighty = 1;
        s.add(Box.createGlue(), konec);
        return s;
    }

    private JComponent hlavicka(Auto a) {
        Ui.Karta k = new Ui.Karta(new BorderLayout(0, 16), 20, 22);
        k.prouzek(Ui.priorita(g.getPriorita(a)));

        JPanel horni = Ui.pruhledny(new BorderLayout(16, 0));
        horni.add(new Ui.Avatar(a, 64), BorderLayout.WEST);
        JPanel texty = Ui.pruhledny(new GridLayout(2, 1, 0, 2));
        JPanel r1 = Ui.pruhledny(new FlowLayout(FlowLayout.LEFT, 10, 0));
        r1.add(Ui.titulek(a.getJmenoAuta(), 26));
        r1.add(new Ui.Spz(a.getSpz()));
        texty.add(r1);
        int stari = a.getRokVyroby() > 0 ? Year.now().getValue() - a.getRokVyroby() : -1;
        JLabel pod = Ui.muted(a.getZnacka() + "  ·  rok " + (a.getRokVyroby() > 0 ? a.getRokVyroby() + " (" + stari + " let)" : "neuveden")
                + "  ·  " + a.getDruhPaliva() + "  ·  " + MainFrame.format(a.getSpotreba()) + " l/100 km  ·  najeto "
                + Ui.tis(a.getUjeteKm()) + " km", 13.5f);
        pod.setBorder(new EmptyBorder(0, 10, 0, 0));
        texty.add(pod);
        horni.add(texty, BorderLayout.CENTER);

        JPanel prio = Ui.pruhledny(new GridLayout(0, 1, 0, 4));
        JPanel p1 = Ui.pruhledny(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        p1.add(Ui.muted("#" + g.getPoradi(a) + " z " + g.getPocetAut(), 12.5f));
        p1.add(Ui.stitekPriority(g.getPriorita(a)));
        prio.add(p1);
        JLabel sk = Ui.label("skóre " + Math.round(g.getSkore(a)) + " / 100", Font.BOLD, 13, Ui.text2());
        sk.setHorizontalAlignment(SwingConstants.RIGHT);
        prio.add(sk);
        if (a.jePripnuta()) {
            JLabel pin = Ui.label("připnuto" + (a.getDuvodPripnuti().isEmpty() ? "" : ": " + a.getDuvodPripnuti()),
                    Font.PLAIN, 12, Ui.accent());
            pin.setIcon(new Ui.Ikona("pin", 12, Ui.accent()));
            pin.setHorizontalAlignment(SwingConstants.RIGHT);
            prio.add(pin);
        }
        horni.add(prio, BorderLayout.EAST);

        JPanel akce = Ui.pruhledny(new Ui.Zalom(FlowLayout.LEFT, 8, 8));
        akce.add(Ui.primarni("Natankovat", "fuel", () -> app.natankovat(a)));
        akce.add(Ui.sekundarni("Výlet", "route", () -> app.vylet(a)));
        akce.add(Ui.sekundarni("Trasa na mapě", "map", () -> app.mapaCR(a)));
        akce.add(Ui.sekundarni("Zapsat servis", "wrench", () -> app.servis(a)));
        akce.add(Ui.sekundarni("Odstávka", "swap", () -> app.simulace(a)));
        akce.add(Ui.sekundarni("3D model", "cube", () -> app.model3D(a)));
        akce.add(Ui.sekundarni("Upravit", "edit", () -> app.upravitAuto(a)));
        if (a.jePripnuta()) akce.add(Ui.duch("Odepnout", "pin", () -> app.odepnout(a)));
        else akce.add(Ui.duch("Připnout", "pin", () -> app.pripnout(a)));
        Ui.Tlacitko odebrat = new Ui.Tlacitko("", Ui.Styl.NEBEZPECNE, "trash");
        odebrat.setToolTipText("Odebrat auto");
        odebrat.addActionListener(e -> app.odebratAuto(a));
        akce.add(odebrat);

        k.add(horni, BorderLayout.NORTH);
        k.add(akce, BorderLayout.CENTER);
        return k;
    }

    private JComponent nadrz(Auto a) {
        float n = a.getNaplneni();
        JPanel obsah = Ui.pruhledny(new BorderLayout(14, 8));
        obsah.add(new Ui.Prstenec(n, Ui.stav(n), Math.round(n * 100) + " %", "", 74), BorderLayout.WEST);
        JPanel t = Ui.pruhledny(new GridLayout(0, 1, 0, 2));
        JLabel l = Ui.label(MainFrame.format(Math.round(a.getPalivo() * 10) / 10f) + " l", Font.BOLD, 20, Ui.text());
        l.setFont(Ui.serif(Font.BOLD, 22));
        t.add(l);
        t.add(Ui.muted("z " + MainFrame.format(a.getMaxPalivo()) + " l", 12.5f));
        t.add(Ui.muted("≈ " + Ui.tis(a.getDojezd()) + " km", 12.5f));
        JPanel tObal = Ui.pruhledny(new GridBagLayout());
        tObal.add(t);
        obsah.add(tObal, BorderLayout.CENTER);
        obsah.add(Ui.muted("do plna ≈ " + Ui.kc((a.getMaxPalivo() - a.getPalivo()) * g.getCenaPaliva(a))
                + " · " + MainFrame.format(g.getCenaPaliva(a)) + " Kč/l", 12), BorderLayout.SOUTH);
        return Ui.sekce("Nádrž", new Ui.Stitek(a.getDruhPaliva().toString(), Ui.text2()), obsah);
    }

    private JComponent servis(Auto a) {
        float o = a.getOpotrebeniServisu();
        Color b = barvaServisu(o);
        JPanel obsah = Ui.pruhledny(new GridLayout(0, 1, 0, 6));
        JLabel velke = Ui.label(Ui.tis(a.getKmOdServisu()) + " km", Font.BOLD, 22, Ui.text());
        velke.setFont(Ui.serif(Font.BOLD, 24));
        obsah.add(velke);
        obsah.add(new Ui.Ukazatel(o, b, 10).znacka(0.9f));
        float zbyva = a.getKmDoServisu();
        obsah.add(Ui.label(zbyva >= 0 ? "do servisu zbývá " + Ui.tis(zbyva) + " km" : "servis přetažen o " + Ui.tis(-zbyva) + " km",
                Font.BOLD, 12.5f, o >= 0.9f ? b : Ui.text2()));
        obsah.add(Ui.muted("interval " + Ui.tis(a.getServisniInterval()) + " km · poslední " + MainFrame.datum(a.getPosledniServis()), 12));
        JPanel vpravo = Ui.pruhledny(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        vpravo.add(Ui.odhad("≈ " + Ui.kc(g.odhadCenyServisu(a))));
        return Ui.sekce("Servis", vpravo, obsah);
    }

    private JComponent stk(Auto a) {
        long d = a.getDniDoStk();
        JPanel obsah = Ui.pruhledny(new GridLayout(0, 1, 0, 6));
        JLabel velke = Ui.label(MainFrame.datum(a.getStkDo()), Font.BOLD, 22, Ui.text());
        velke.setFont(Ui.serif(Font.BOLD, 24));
        obsah.add(velke);
        JPanel st = Ui.pruhledny(new FlowLayout(FlowLayout.LEFT, 0, 0));
        st.add(new Ui.Stitek(d == Integer.MAX_VALUE ? "datum neznámé" : d < 0 ? "prošlá " + dny(d) : "platí ještě " + dny(d).replace("za ", ""),
                barvaStk(d)));
        obsah.add(st);
        obsah.add(Ui.odstavec(d < 0 ? "Auto nesmí na silnici, dokud neprojde kontrolou." :
                d <= 30 ? "Objednej termín, STK se blíží." : "Technická kontrola je v pořádku.", 12, Ui.muted()));
        return Ui.sekce("STK", null, obsah);
    }

    private JComponent skore(Auto a) {
        Color[] pal = Grafy.paletaFaktoru();
        Garaz.Faktor[] ff = Garaz.Faktor.values();
        double[] dily = new double[ff.length];
        for (int i = 0; i < ff.length; i++) dily[i] = g.getPrispevek(a, ff[i]);
        JPanel obsah = Ui.pruhledny(new BorderLayout(0, 14));
        obsah.add(new Grafy.Rozklad(dily, pal), BorderLayout.NORTH);
        JPanel mrizka = Ui.pruhledny(new GridLayout(0, 3, 18, 10));
        for (int i = 0; i < ff.length; i++) {
            Garaz.Faktor f = ff[i];
            JPanel bunka = Ui.pruhledny(new BorderLayout(8, 0));
            Color c = pal[i];
            JComponent tecka = new JComponent() {
                @Override
                protected void paintComponent(Graphics gr) {
                    Graphics2D g2 = (Graphics2D) gr.create();
                    Ui.aa(g2);
                    g2.setColor(c);
                    g2.fillRoundRect(0, 4, 10, 10, 4, 4);
                    g2.dispose();
                }
            };
            tecka.setPreferredSize(new Dimension(10, 18));
            bunka.add(tecka, BorderLayout.WEST);
            JPanel t = Ui.pruhledny(new GridLayout(2, 1));
            JPanel h = Ui.pruhledny(new BorderLayout());
            h.add(Ui.silny(f.getNazev(), 13), BorderLayout.WEST);
            h.add(Ui.label(MainFrame.cislo1(dily[i] > 0 ? (float) dily[i] : 0) + " b", Font.BOLD, 13, c), BorderLayout.EAST);
            t.add(h);
            t.add(Ui.muted(g.popisFaktoru(a, f) + " · váha " + g.getVaha(f), 12));
            bunka.add(t, BorderLayout.CENTER);
            mrizka.add(bunka);
        }
        obsah.add(mrizka, BorderLayout.CENTER);
        Ui.Tlacitko vahy = Ui.duch("Upravit váhy", "settings", () -> app.prepni("priorita"));
        return Ui.sekce("Proč má tuhle prioritu", vahy, obsah);
    }

    private JComponent naklady(Auto a) {
        float[] n = g.getNakladyAuta(a);
        YearMonth ted = YearMonth.now();
        double[] mesice = new double[12];
        for (Garaz.Tankovani t : g.getTankovani(a)) {
            YearMonth m = YearMonth.from(t.getDatum());
            int i = 11 - (int) java.time.temporal.ChronoUnit.MONTHS.between(m, ted);
            if (i >= 0 && i < 12) mesice[i] += t.getCena();
        }
        JPanel obsah = Ui.pruhledny(new BorderLayout(0, 12));
        JPanel cisla = Ui.pruhledny(new GridLayout(1, 3, 12, 0));
        cisla.add(male("Palivo", Ui.kc(n[0])));
        cisla.add(male("Servis", Ui.kc(n[1])));
        cisla.add(male("Za 1 km", MainFrame.format(Math.round(g.getCenaZaKm(a) * 100) / 100f) + " Kč"));
        obsah.add(cisla, BorderLayout.NORTH);
        JPanel graf = Ui.pruhledny(new BorderLayout(0, 4));
        graf.add(Ui.muted("Palivo po měsících (12 měsíců)", 12), BorderLayout.NORTH);
        graf.add(new Grafy.Krivka(mesice, Ui.accent(), 300, 70), BorderLayout.CENTER);
        obsah.add(graf, BorderLayout.CENTER);
        return Ui.sekce("Náklady", null, obsah);
    }

    private static JComponent male(String nadpis, String hodnota) {
        JPanel p = Ui.pruhledny(new GridLayout(2, 1, 0, 2));
        p.add(Ui.eyebrow(nadpis));
        JLabel h = Ui.label(hodnota, Font.BOLD, 17, Ui.text());
        h.setFont(Ui.serif(Font.BOLD, 19));
        p.add(h);
        return p;
    }

    private JComponent knihaTankovani(Auto a) {
        List<Garaz.Tankovani> t = g.getTankovani(a);
        JPanel obsah = Ui.pruhledny(new GridLayout(0, 1, 0, 6));
        if (t.isEmpty()) obsah.add(Ui.muted("Zatím žádné tankování."));
        int n = 0;
        for (int i = t.size() - 1; i >= 0 && n < 7; i--, n++) {
            Garaz.Tankovani x = t.get(i);
            JPanel r = Ui.pruhledny(new GridLayout(1, 3, 8, 0));
            r.add(Ui.muted(MainFrame.datum(x.getDatum()), 12.5f));
            r.add(Ui.text(MainFrame.format(Math.round(x.getLitry() * 10) / 10f) + " l"));
            JLabel c = Ui.silny(Ui.kc(x.getCena()), 13);
            c.setHorizontalAlignment(SwingConstants.RIGHT);
            r.add(c);
            obsah.add(r);
        }
        return Ui.sekce("Kniha tankování", Ui.muted(t.size() + " záznamů", 12), obsah);
    }

    private JComponent knihaServisu(Auto a) {
        List<Garaz.Servis> s = g.getServisy(a);
        JPanel obsah = Ui.pruhledny(new GridLayout(0, 1, 0, 6));
        if (s.isEmpty()) obsah.add(Ui.muted("Žádný zapsaný servis."));
        int n = 0;
        for (int i = s.size() - 1; i >= 0 && n < 6; i--, n++) {
            Garaz.Servis x = s.get(i);
            JPanel r = Ui.pruhledny(new BorderLayout(10, 0));
            JLabel d = Ui.muted(MainFrame.datum(x.getDatum()), 12.5f);
            d.setPreferredSize(new Dimension(84, 10));
            r.add(d, BorderLayout.WEST);
            r.add(Ui.text(x.getPopis()), BorderLayout.CENTER);
            r.add(Ui.silny(Ui.kc(x.getCena()), 13), BorderLayout.EAST);
            obsah.add(r);
        }
        return Ui.sekce("Servisní kniha", Ui.duch("Zapsat", "plus", () -> app.servis(a)), obsah);
    }

    private JComponent poznamka(Auto a) {
        JTextArea t = new JTextArea(a.getPoznamka(), 4, 20);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setFont(Ui.font(Font.PLAIN, 13.5f));
        t.setBackground(Ui.surface2());
        t.setForeground(Ui.text());
        t.setCaretColor(Ui.text());
        ((javax.swing.text.DefaultCaret) t.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);
        t.setBorder(new EmptyBorder(10, 12, 10, 12));
        JLabel stav = Ui.muted("Uloží se po opuštění pole.", 12);
        t.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (!t.getText().trim().equals(a.getPoznamka())) {
                    a.setPoznamka(t.getText());
                    g.zapis("Poznámka u auta " + a + " upravena");
                    app.ulozit();
                    stav.setText("Uloženo.");
                }
            }
        });
        JPanel obsah = Ui.pruhledny(new BorderLayout(0, 6));
        obsah.add(t, BorderLayout.CENTER);
        obsah.add(stav, BorderLayout.SOUTH);
        return Ui.sekce("Poznámka", null, obsah);
    }
}
