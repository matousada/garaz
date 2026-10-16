package gui;

import data.Auto;
import data.Garaz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Společný základ pohledů v hlavním okně: hlavička (nadpis, popis, akce vpravo) a posouvatelný obsah.
 * Pohled se při každé změně dat postaví znovu (obnov), takže vždy ukazuje aktuální stav i barvy tématu.
 */
abstract class Pohled extends JPanel {

    protected final MainFrame app;
    protected final Garaz g;

    private JPanel obsah;
    private int radek;
    private JScrollPane posuv;

    Pohled(MainFrame app) {
        super(new BorderLayout());
        setOpaque(false);
        this.app = app;
        this.g = app.garaz();
    }

    abstract String nazev();

    abstract String ikona();

    abstract String podtitul();

    /** Tlačítka vpravo v hlavičce (nepovinné). */
    JComponent akce() {
        return null;
    }

    /** Naplní obsah pohledu bloky (volá se z obnov). */
    abstract void postav();

    void obnov() {
        int pozice = posuv == null ? 0 : posuv.getVerticalScrollBar().getValue();
        removeAll();

        JPanel hlava = Ui.pruhledny(new BorderLayout(16, 0));
        hlava.setBorder(new EmptyBorder(24, 30, 14, 30));
        JPanel texty = Ui.pruhledny(new BorderLayout(0, 4));
        texty.add(Ui.titulek(nazev(), 28), BorderLayout.NORTH);
        texty.add(Ui.muted(podtitul(), 13.5f), BorderLayout.CENTER);
        hlava.add(texty, BorderLayout.CENTER);
        JComponent a = akce();
        if (a != null) {
            JPanel obal = Ui.pruhledny(new GridBagLayout());
            obal.add(a);
            hlava.add(obal, BorderLayout.EAST);
        }
        add(hlava, BorderLayout.NORTH);
        add(telo(), BorderLayout.CENTER);
        revalidate();
        repaint();
        JScrollPane sp = posuv;
        if (sp != null) SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> sp.getVerticalScrollBar().setValue(pozice)));
    }

    /** Výchozí tělo: sloupec bloků ve svislém posuvníku. Pohled s jiným rozložením ho přepíše. */
    JComponent telo() {
        obsah = new Ui.SirkaPanel(new GridBagLayout());
        obsah.setBorder(new EmptyBorder(4, 30, 30, 30));
        radek = 0;
        postav();
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = radek;
        c.weighty = 1;
        obsah.add(Box.createGlue(), c);
        posuv = Ui.scroll(obsah);
        return posuv;
    }

    /** Přidá blok přes celou šířku pod předchozí. */
    void blok(JComponent k) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = radek++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 18, 0);
        obsah.add(uzky(k), c);
    }

    /** Obal, který hlásí šířku 10: GridBag pak dělí místo jen podle vah a nikdy nepřepne na minimální velikosti. */
    static JComponent uzky(JComponent c) {
        JPanel obal = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(10, super.getPreferredSize().height);
            }

            @Override
            public Dimension getMinimumSize() {
                return new Dimension(0, 0);
            }
        };
        obal.setOpaque(false);
        obal.add(c);
        return obal;
    }

    /** Vedle sebe postavené komponenty se zadanými poměry šířky (výška podle nejvyšší). */
    static JPanel vedle(int mezera, double[] vahy, JComponent... deti) {
        JPanel p = Ui.pruhledny(new GridBagLayout());
        for (int i = 0; i < deti.length; i++) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i;
            c.weightx = vahy == null ? 1 : vahy[i];
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(0, i == 0 ? 0 : mezera, 0, 0);
            p.add(uzky(deti[i]), c);
        }
        return p;
    }

    /** Prázdný stav: ikona, nadpis, text a volitelné tlačítko. */
    static JComponent prazdno(String ikona, String nadpis, String text, Ui.Tlacitko tlacitko) {
        Ui.Karta k = new Ui.Karta(new GridBagLayout(), 40, 30);
        JPanel s = Ui.pruhledny(null);
        s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));
        JLabel ik = new JLabel(new Ui.Ikona(ikona, 40, Ui.muted()));
        ik.setAlignmentX(CENTER_ALIGNMENT);
        JLabel n = Ui.titulek(nadpis, 20);
        n.setAlignmentX(CENTER_ALIGNMENT);
        JLabel t = Ui.muted(text, 13.5f);
        t.setAlignmentX(CENTER_ALIGNMENT);
        s.add(ik);
        s.add(Box.createVerticalStrut(12));
        s.add(n);
        s.add(Box.createVerticalStrut(6));
        s.add(t);
        if (tlacitko != null) {
            s.add(Box.createVerticalStrut(16));
            tlacitko.setAlignmentX(CENTER_ALIGNMENT);
            s.add(tlacitko);
        }
        k.add(s);
        return k;
    }

    /** Řádek žebříčku: pořadí, avatar, jméno + SPZ, hlavní důvod, skóre, třída. */
    JComponent radekAuta(Auto a, boolean kompaktni, Runnable klik) {
        Ui.Karta r = new Ui.Karta(new BorderLayout(12, 0), kompaktni ? 8 : 10, 12).vypln(Ui.surface());
        r.radius = 12;
        r.klik(klik);
        JLabel poradi = Ui.label(String.valueOf(g.getPoradi(a)), Font.BOLD, 15, Ui.muted());
        poradi.setFont(Ui.font(Font.BOLD, 15));
        poradi.setPreferredSize(new Dimension(24, 24));
        poradi.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel vlevo = Ui.pruhledny(new FlowLayout(FlowLayout.LEFT, 10, 0));
        vlevo.add(poradi);
        vlevo.add(new Ui.Avatar(a, kompaktni ? 34 : 40));
        JPanel obalVlevo = Ui.pruhledny(new GridBagLayout());
        obalVlevo.add(vlevo);

        JPanel texty = Ui.pruhledny(new GridLayout(2, 1, 0, 1));
        JPanel jmeno = Ui.pruhledny(new FlowLayout(FlowLayout.LEFT, 8, 0));
        jmeno.add(Ui.silny(a.getJmenoAuta(), 14));
        jmeno.add(Ui.muted(a.getZnacka() + " · " + a.getSpz(), 12.5f));
        if (a.jePripnuta()) jmeno.add(new JLabel(new Ui.Ikona("pin", 13, Ui.accent())));
        texty.add(jmeno);
        Garaz.Faktor f = g.getHlavniFaktor(a);
        JLabel duvod = Ui.muted("Hlavně: " + f.getNazev().toLowerCase() + " – " + g.popisFaktoru(a, f), 12.5f);
        duvod.setBorder(new EmptyBorder(0, 8, 0, 0));
        texty.add(duvod);

        JPanel vpravo = Ui.pruhledny(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        JLabel skore = Ui.label(String.valueOf(Math.round(g.getSkore(a))), Font.BOLD, 15, Ui.text());
        skore.setFont(Ui.serif(Font.BOLD, 18));
        skore.setToolTipText("Skóre 0–100");
        vpravo.add(skore);
        vpravo.add(Ui.stitekPriority(g.getPriorita(a)));
        JPanel obalVpravo = Ui.pruhledny(new GridBagLayout());
        obalVpravo.add(vpravo);

        r.add(obalVlevo, BorderLayout.WEST);
        r.add(texty, BorderLayout.CENTER);
        r.add(obalVpravo, BorderLayout.EAST);
        return r;
    }

    /** Text "za 12 dní" / "před 3 dny" / "dnes". */
    static String dny(long d) {
        if (d == Integer.MAX_VALUE) return "neznámo";
        if (d == 0) return "dnes";
        long x = Math.abs(d);
        String s = x == 1 ? "den" : x < 5 ? "dny" : "dní";
        if (d < 0) s = x == 1 ? "dnem" : "dny";
        return d > 0 ? "za " + x + " " + s : "před " + x + " " + s;
    }

    static Color barvaStk(long d) {
        if (d < 0) return Ui.danger();
        if (d <= 30) return Ui.crit();
        if (d <= 90) return Ui.warn();
        return Ui.ok();
    }

    static Color barvaServisu(float opotrebeni) {
        if (opotrebeni >= 1) return Ui.danger();
        if (opotrebeni >= 0.9f) return Ui.crit();
        if (opotrebeni >= 0.7f) return Ui.warn();
        return Ui.accent();
    }
}
