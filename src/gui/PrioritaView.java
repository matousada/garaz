package gui;

import data.Auto;
import data.Garaz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Priorita údržby: váhy faktorů (posuvníky a předvolby), živý žebříček, ruční připnutí a porovnání dvou aut.
 * Stejný princip jako vážené skóre v Cesty KHK: model navrhne pořadí, člověk ho může přebít.
 */
class PrioritaView extends Pohled {

    private JPanel zebricek;
    private JPanel rozdeleni;
    private Predvolby predvolby;
    private final JSlider[] posuvniky = new JSlider[Garaz.Faktor.values().length];
    private final JLabel[] hodnoty = new JLabel[Garaz.Faktor.values().length];
    private boolean nastavuji;

    PrioritaView(MainFrame app) {
        super(app);
    }

    @Override
    String nazev() {
        return "Priorita údržby";
    }

    @Override
    String ikona() {
        return "priority";
    }

    @Override
    String podtitul() {
        return "Skóre 0–100 z šesti faktorů. Posuň váhy a žebříček se hned přepočítá.";
    }

    @Override
    JComponent akce() {
        return Ui.radek(8, Ui.sekundarni("Jak se to počítá", "book", () -> app.prepni("metodika")));
    }

    @Override
    void postav() {
        if (g.jePrazdna()) {
            blok(prazdno("priority", "Není co řadit", "Žebříček se objeví, až budou v garáži auta.", null));
            return;
        }
        JPanel vlevo = Ui.pruhledny(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 16, 0);
        c.gridy = 0;
        vlevo.add(uzky(vahy()), c);
        c.gridy = 1;
        vlevo.add(uzky(porovnani()), c);
        c.gridy = 2;
        c.weighty = 1;
        vlevo.add(Box.createGlue(), c);

        zebricek = Ui.pruhledny(new GridLayout(0, 1, 0, 8));
        rozdeleni = Ui.pruhledny(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        naplnZebricek();
        Ui.Karta prava = Ui.sekce("Žebříček", rozdeleni, zebricek);
        blok(vedle(18, new double[]{0.85, 1.4}, vlevo, prava));
    }

    private JComponent vahy() {
        Garaz.Predvolba[] pp = Garaz.Predvolba.values();
        String[] nazvy = new String[pp.length];
        for (int i = 0; i < pp.length; i++) nazvy[i] = pp[i].getNazev();
        Garaz.Predvolba akt = g.getAktivniPredvolba();
        predvolby = new Predvolby(nazvy, akt == null ? -1 : akt.ordinal());
        JTextArea popisPredvolby = Ui.odstavec(akt == null ? "Vlastní nastavení vah." : akt.getPopis(), 12.5f, Ui.muted());
        predvolby.naZmenu(i -> {
            g.pouzijPredvolbu(pp[i]);
            g.zapis("Váhy priority: předvolba " + pp[i].getNazev());
            popisPredvolby.setText(pp[i].getPopis());
            nastavuji = true;
            for (Garaz.Faktor f : Garaz.Faktor.values()) posuvniky[f.ordinal()].setValue(g.getVaha(f));
            nastavuji = false;
            zmenaVah();
            app.ulozit();
        });

        JPanel obsah = Ui.pruhledny(null);
        obsah.setLayout(new BoxLayout(obsah, BoxLayout.Y_AXIS));
        JPanel pObal = Ui.pruhledny(new BorderLayout());
        pObal.add(predvolby, BorderLayout.CENTER);
        pObal.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        pObal.setAlignmentX(LEFT_ALIGNMENT);
        obsah.add(pObal);
        obsah.add(Box.createVerticalStrut(6));
        popisPredvolby.setAlignmentX(LEFT_ALIGNMENT);
        obsah.add(popisPredvolby);
        obsah.add(Box.createVerticalStrut(10));

        Color[] pal = Grafy.paletaFaktoru();
        for (Garaz.Faktor f : Garaz.Faktor.values()) {
            int i = f.ordinal();
            JPanel r = Ui.pruhledny(new BorderLayout(0, 2));
            JPanel h = Ui.pruhledny(new BorderLayout());
            JLabel n = Ui.silny(f.getNazev(), 13);
            n.setIcon(new Ui.Ikona("chevron-right", 10, pal[i]));
            h.add(n, BorderLayout.WEST);
            hodnoty[i] = Ui.label(String.valueOf(g.getVaha(f)), Font.BOLD, 13, pal[i]);
            h.add(hodnoty[i], BorderLayout.EAST);
            r.add(h, BorderLayout.NORTH);
            posuvniky[i] = Ui.posuvnik(0, 50, g.getVaha(f));
            posuvniky[i].addChangeListener(e -> {
                if (nastavuji) return;
                g.setVaha(f, posuvniky[i].getValue());
                Garaz.Predvolba p = g.getAktivniPredvolba();
                predvolby.setVybrana(p == null ? -1 : p.ordinal());
                popisPredvolby.setText(p == null ? "Vlastní nastavení vah." : p.getPopis());
                zmenaVah();
                if (!posuvniky[i].getValueIsAdjusting()) {
                    g.zapis("Váha „" + f.getNazev() + "“ nastavena na " + g.getVaha(f));
                    app.ulozit();
                }
            });
            r.add(posuvniky[i], BorderLayout.CENTER);
            r.add(Ui.muted(f.getPopis(), 11.5f), BorderLayout.SOUTH);
            r.setAlignmentX(LEFT_ALIGNMENT);
            obsah.add(r);
            obsah.add(Box.createVerticalStrut(10));
        }
        Ui.Tlacitko reset = Ui.duch("Výchozí", "swap", () -> {
            g.obnovVychoziVahy();
            nastavuji = true;
            for (Garaz.Faktor f : Garaz.Faktor.values()) posuvniky[f.ordinal()].setValue(g.getVaha(f));
            nastavuji = false;
            Garaz.Predvolba p = g.getAktivniPredvolba();
            predvolby.setVybrana(p == null ? -1 : p.ordinal());
            popisPredvolby.setText(p == null ? "Vlastní nastavení vah." : p.getPopis());
            zmenaVah();
            app.ulozit();
        });
        return Ui.sekce("Váhy faktorů", reset, obsah);
    }

    private void zmenaVah() {
        for (Garaz.Faktor f : Garaz.Faktor.values()) hodnoty[f.ordinal()].setText(String.valueOf(g.getVaha(f)));
        naplnZebricek();
    }

    private void naplnZebricek() {
        zebricek.removeAll();
        int[] pocty = new int[4];
        for (Auto a : g.getZebricek()) {
            pocty[g.getPriorita(a).ordinal()]++;
            JPanel r = Ui.pruhledny(new BorderLayout(8, 0));
            r.add(radekAuta(a, false, () -> app.otevriAuto(a)), BorderLayout.CENTER);
            Ui.Tlacitko pin = new Ui.Tlacitko("", a.jePripnuta() ? Ui.Styl.JEMNE : Ui.Styl.DUCH, "pin");
            pin.setToolTipText(a.jePripnuta() ? "Odepnout (" + a.getPripnuta() + ")" : "Připnout prioritu ručně");
            pin.addActionListener(e -> {
                if (a.jePripnuta()) app.odepnout(a);
                else app.pripnout(a);
            });
            JPanel pObal = Ui.pruhledny(new GridBagLayout());
            pObal.add(pin);
            r.add(pObal, BorderLayout.EAST);
            zebricek.add(r);
        }
        rozdeleni.removeAll();
        for (Auto.Priorita p : Auto.Priorita.values()) {
            if (pocty[p.ordinal()] > 0) rozdeleni.add(new Ui.Stitek(pocty[p.ordinal()] + "× " + p.getNazev().toLowerCase(), Ui.priorita(p)));
        }
        zebricek.revalidate();
        zebricek.repaint();
        rozdeleni.revalidate();
        rozdeleni.repaint();
    }

    private JComponent porovnani() {
        List<Auto> z = g.getZebricek();
        Auto[] pole = z.toArray(new Auto[0]);
        JComboBox<Auto> a = Ui.vyber(pole);
        JComboBox<Auto> b = Ui.vyber(pole);
        if (pole.length > 1) b.setSelectedIndex(1);
        JPanel obsah = Ui.pruhledny(new GridLayout(0, 1, 0, 8));
        obsah.add(a);
        obsah.add(b);
        JTextArea t = Ui.odstavec("Vyber dvě auta a uvidíš faktor po faktoru, proč je jedno výš.", 12.5f, Ui.muted());
        obsah.add(t);
        Ui.Tlacitko porovnej = Ui.primarni("Porovnat", "swap", () -> {
            Auto x = (Auto) a.getSelectedItem(), y = (Auto) b.getSelectedItem();
            if (x != null && y != null && x != y) app.porovnat(x, y);
        });
        JPanel obal = Ui.pruhledny(new FlowLayout(FlowLayout.LEFT, 0, 0));
        obal.add(porovnej);
        obsah.add(obal);
        obsah.setBorder(new EmptyBorder(0, 0, 0, 0));
        return Ui.sekce("Proč A před B", null, obsah);
    }

    /** Předvolby vah jako řada tlačítek přes celou šířku karty (vybraná je zvýrazněná). */
    private static final class Predvolby extends JPanel {
        private final Ui.Tlacitko[] tlacitka;
        private final java.util.List<java.util.function.IntConsumer> posluchaci = new java.util.ArrayList<>();

        Predvolby(String[] nazvy, int vybrana) {
            super(new GridLayout(1, nazvy.length, 6, 0));
            setOpaque(false);
            tlacitka = new Ui.Tlacitko[nazvy.length];
            for (int i = 0; i < nazvy.length; i++) {
                int idx = i;
                tlacitka[i] = new Ui.Tlacitko(nazvy[i], Ui.Styl.JEMNE);
                tlacitka[i].addActionListener(e -> {
                    setVybrana(idx);
                    for (java.util.function.IntConsumer c : posluchaci) c.accept(idx);
                });
                add(tlacitka[i]);
            }
            setVybrana(vybrana);
        }

        void naZmenu(java.util.function.IntConsumer c) {
            posluchaci.add(c);
        }

        void setVybrana(int v) {
            for (int i = 0; i < tlacitka.length; i++) tlacitka[i].setVybrane(i == v);
        }
    }
}
