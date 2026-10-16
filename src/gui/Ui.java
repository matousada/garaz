package gui;

import data.Auto;
import data.Garaz;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Design systém aplikace: barevné tokeny (světlý / tmavý režim), písma a sada vlastních komponent.
 * Vzhled vychází z projektu Cesty KHK: teplé světlé pozadí, jedna petrolejová akcentová barva,
 * červená / oranžová / žlutá / zelená jen pro stav a prioritu.
 */
final class Ui {

    private Ui() {
    }

    // ------------------------------------------------------------------
    // tokeny
    // ------------------------------------------------------------------

    static boolean tmavy = false;

    private static Color c(int rgb) {
        return new Color(rgb);
    }

    static Color bg() { return tmavy ? c(0x11161d) : c(0xf4f2ee); }
    static Color surface() { return tmavy ? c(0x171d26) : c(0xffffff); }
    static Color surface2() { return tmavy ? c(0x1b222c) : c(0xf8f7f4); }
    static Color sunken() { return tmavy ? c(0x0e1319) : c(0xefece6); }
    static Color text() { return tmavy ? c(0xe6ebf1) : c(0x1a1f26); }
    static Color text2() { return tmavy ? c(0xc3cbd6) : c(0x3d4550); }
    static Color muted() { return tmavy ? c(0x8b96a5) : c(0x6b7280); }
    static Color line() { return tmavy ? c(0x262f3b) : c(0xe4e0d8); }
    static Color lineStrong() { return tmavy ? c(0x323d4b) : c(0xd4cfc5); }
    static Color accent() { return tmavy ? c(0x2bb3b1) : c(0x0e7c7b); }
    static Color accentHover() { return tmavy ? c(0x3cc6c4) : c(0x0b6968); }
    static Color onAccent() { return tmavy ? c(0x06201f) : Color.WHITE; }
    static Color crit() { return tmavy ? c(0xf97316) : c(0xc2410c); }
    static Color warn() { return tmavy ? c(0xf59e0b) : c(0xb45309); }
    static Color ok() { return tmavy ? c(0x4ade80) : c(0x15803d); }
    static Color danger() { return tmavy ? c(0xf87171) : c(0xb91c1c); }
    static Color yellow() { return tmavy ? c(0xfacc15) : c(0xa16207); }

    /** Průhledná verze barvy (pro jemná pozadí štítků). */
    static Color soft(Color c, float alfa) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(255 * alfa));
    }

    static Color accentSoft() {
        return soft(accent(), tmavy ? 0.14f : 0.09f);
    }

    /** Barva třídy priority: Kritická červená, Vysoká oranžová, Střední žlutá, Nízká zelená. */
    static Color priorita(Auto.Priorita p) {
        switch (p) {
            case KRITICKA:
                return danger();
            case VYSOKA:
                return crit();
            case STREDNI:
                return yellow();
            default:
                return ok();
        }
    }

    static Color zavaznost(Garaz.Zavaznost z) {
        switch (z) {
            case KRITICKE:
                return danger();
            case VAROVANI:
                return warn();
            default:
                return accent();
        }
    }

    /** Barva podle naplnění 0..1 (málo = červená). */
    static Color stav(float naplneni) {
        return naplneni > 0.5f ? ok() : (naplneni > 0.2f ? warn() : danger());
    }

    // ------------------------------------------------------------------
    // písma
    // ------------------------------------------------------------------

    private static final String SANS = vyberPismo("Segoe UI", "IBM Plex Sans", "Inter", "Helvetica Neue", Font.SANS_SERIF);
    private static final String SERIF = vyberPismo("Georgia", "Fraunces", "Cambria", Font.SERIF);

    private static String vyberPismo(String... kandidati) {
        java.util.Set<String> dostupne = new java.util.HashSet<>(java.util.Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String k : kandidati) {
            if (dostupne.contains(k)) return k;
        }
        return kandidati[kandidati.length - 1];
    }

    static Font font(int styl, float velikost) {
        return new Font(SANS, styl, Math.round(velikost));
    }

    static Font serif(int styl, float velikost) {
        return new Font(SERIF, styl, Math.round(velikost));
    }

    static void aa(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    // ------------------------------------------------------------------
    // texty
    // ------------------------------------------------------------------

    static JLabel label(String text, int styl, float velikost, Color barva) {
        JLabel l = new JLabel(text);
        l.setFont(font(styl, velikost));
        l.setForeground(barva);
        return l;
    }

    static JLabel text(String text) {
        return label(text, Font.PLAIN, 14, text());
    }

    static JLabel silny(String text, float velikost) {
        return label(text, Font.BOLD, velikost, text());
    }

    static JLabel muted(String text) {
        return label(text, Font.PLAIN, 13, muted());
    }

    static JLabel muted(String text, float velikost) {
        return label(text, Font.PLAIN, velikost, muted());
    }

    /** Malý nadpis velkými písmeny nad sekcí (eyebrow). */
    static JLabel eyebrow(String text) {
        JLabel l = new JLabel(text.toUpperCase(new java.util.Locale("cs")));
        l.setFont(font(Font.BOLD, 11.5f));
        l.setForeground(muted());
        return l;
    }

    /** Patkový nadpis stránky. */
    static JLabel titulek(String text, float velikost) {
        JLabel l = new JLabel(text);
        l.setFont(serif(Font.BOLD, velikost));
        l.setForeground(text());
        return l;
    }

    /** Víceřádkový text, který se zalamuje podle šířky (bez HTML, takže se nedá nic podstrčit). */
    static JTextArea odstavec(String text, float velikost, Color barva) {
        JTextArea t = new JTextArea(text);
        t.setEditable(false);
        t.setFocusable(false);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setOpaque(false);
        t.setBorder(null);
        t.setFont(font(Font.PLAIN, velikost));
        ((javax.swing.text.DefaultCaret) t.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);
        t.setForeground(barva);
        return t;
    }

    static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    /** Číslo s mezerami po tisících: 12500 -> "12 500". */
    static String tis(double v) {
        return String.format(new java.util.Locale("cs", "CZ"), "%,d", Math.round(v)).replace(' ', ' ');
    }

    static String kc(double v) {
        return tis(v) + " Kč";
    }

    static String zkrat(String text, FontMetrics fm, int sirka) {
        if (fm.stringWidth(text) <= sirka) return text;
        String t = text;
        while (t.length() > 1 && fm.stringWidth(t + "…") > sirka) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "…";
    }

    // ------------------------------------------------------------------
    // rozvržení
    // ------------------------------------------------------------------

    static JPanel pruhledny(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }

    /** Sloupec komponent pod sebou s mezerou. */
    static JPanel sloupec(int mezera, JComponent... deti) {
        JPanel p = pruhledny(null);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        boolean prvni = true;
        for (JComponent c : deti) {
            if (!prvni) p.add(Box.createVerticalStrut(mezera));
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(c);
            prvni = false;
        }
        return p;
    }

    /** Řádek komponent vedle sebe (FlowLayout zleva). */
    static JPanel radek(int mezera, JComponent... deti) {
        JPanel p = pruhledny(new FlowLayout(FlowLayout.LEFT, mezera, 0));
        for (JComponent c : deti) p.add(c);
        return p;
    }

    /** FlowLayout, který při nedostatku místa zalomí řádek a správně spočítá výšku (obyčejný FlowLayout to neumí). */
    static class Zalom extends FlowLayout {
        Zalom(int zarovnani, int h, int v) {
            super(zarovnani, h, v);
        }

        @Override
        public Dimension preferredLayoutSize(Container cil) {
            return velikost(cil, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container cil) {
            Dimension d = velikost(cil, false);
            d.width -= getHgap() + 1;
            return d;
        }

        private Dimension velikost(Container cil, boolean pref) {
            synchronized (cil.getTreeLock()) {
                int sirka = cil.getSize().width;
                Container c = cil;
                while (sirka == 0 && c.getParent() != null) {
                    c = c.getParent();
                    sirka = c.getSize().width;
                }
                if (sirka == 0) sirka = Integer.MAX_VALUE;
                Insets in = cil.getInsets();
                int max = sirka - in.left - in.right - getHgap() * 2;
                int w = 0, h = 0, radekW = 0, radekH = 0;
                for (Component k : cil.getComponents()) {
                    if (!k.isVisible()) continue;
                    Dimension d = pref ? k.getPreferredSize() : k.getMinimumSize();
                    if (radekW + d.width > max && radekW > 0) {
                        w = Math.max(w, radekW);
                        h += radekH + getVgap();
                        radekW = 0;
                        radekH = 0;
                    }
                    radekW += d.width + getHgap();
                    radekH = Math.max(radekH, d.height);
                }
                w = Math.max(w, radekW);
                h += radekH;
                return new Dimension(w + in.left + in.right + getHgap() * 2, h + in.top + in.bottom + getVgap() * 2);
            }
        }
    }

    static JComponent mezera(int w, int h) {
        return (JComponent) Box.createRigidArea(new Dimension(w, h));
    }

    /** Panel, který ve scrollu drží šířku podle viewportu (jen svislé posouvání). */
    static class SirkaPanel extends JPanel implements Scrollable {
        SirkaPanel(LayoutManager lm) {
            super(lm);
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return r.height - 40;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // karta
    // ------------------------------------------------------------------

    /** Zaoblená karta s jemným obrysem. Volitelně barevný proužek vlevo (stav). */
    static class Karta extends JPanel {
        private Color prouzek;
        private Color vypln;
        private boolean hover;
        private boolean klikaci;
        int radius = 14;

        Karta(LayoutManager lm) {
            this(lm, 18, 20);
        }

        Karta(LayoutManager lm, int v, int h) {
            super(lm);
            setOpaque(false);
            setBorder(new EmptyBorder(v, h, v, h));
        }

        Karta prouzek(Color c) {
            this.prouzek = c;
            return this;
        }

        Karta vypln(Color c) {
            this.vypln = c;
            return this;
        }

        /** Karta reaguje na najetí myší a klik. */
        Karta klik(Runnable akce) {
            klikaci = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!contains(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), Karta.this))) {
                        hover = false;
                        repaint();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    akce.run();
                }
            };
            addMouseListener(m);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight();
            // stín
            if (!tmavy) {
                g2.setColor(new Color(26, 31, 38, hover ? 22 : 10));
                g2.fill(new RoundRectangle2D.Float(0, 2, w - 1, h - 2, radius, radius));
            }
            g2.setColor(vypln != null ? vypln : (hover && klikaci ? surface2() : surface()));
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 3, radius, radius));
            g2.setColor(hover && klikaci ? lineStrong() : line());
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2, h - 4, radius, radius));
            if (prouzek != null) {
                g2.setClip(new RoundRectangle2D.Float(0, 0, w - 1, h - 3, radius, radius));
                g2.setColor(prouzek);
                g2.fillRect(0, 0, 4, h);
            }
            g2.dispose();
        }
    }

    /** Karta s nadpisem (eyebrow + volitelně pravá část) a obsahem. */
    static Karta sekce(String nadpis, JComponent vpravo, JComponent obsah) {
        Karta k = new Karta(new BorderLayout(0, 14));
        JPanel hlava = pruhledny(new BorderLayout());
        hlava.add(eyebrow(nadpis), BorderLayout.WEST);
        if (vpravo != null) hlava.add(vpravo, BorderLayout.EAST);
        k.add(hlava, BorderLayout.NORTH);
        JPanel nahoru = pruhledny(new BorderLayout());
        nahoru.add(obsah, BorderLayout.NORTH);
        k.add(nahoru, BorderLayout.CENTER);
        return k;
    }

    // ------------------------------------------------------------------
    // tlačítka
    // ------------------------------------------------------------------

    enum Styl { PRIMARNI, SEKUNDARNI, DUCH, NEBEZPECNE, JEMNE }

    static class Tlacitko extends JButton {
        private final Styl styl;
        private final String ikona;
        private boolean hover;
        private boolean vybrane;

        Tlacitko(String text, Styl styl) {
            this(text, styl, null);
        }

        Tlacitko(String text, Styl styl, String ikona) {
            super(text);
            this.styl = styl;
            this.ikona = ikona;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setFont(font(Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void setVybrane(boolean v) {
            vybrane = v;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            String t = getText() == null ? "" : getText();
            int w = fm.stringWidth(t) + (t.isEmpty() ? 18 : 30) + (ikona != null ? (t.isEmpty() ? 0 : 22) : 0);
            if (ikona != null && t.isEmpty()) w = 36;
            return new Dimension(Math.max(w, 36), 36);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight();
            boolean on = isEnabled();
            boolean stisk = getModel().isPressed();
            Color pozadi = null, obrys = null, popredi;
            switch (styl) {
                case PRIMARNI:
                    pozadi = !on ? sunken() : (stisk || hover ? accentHover() : accent());
                    popredi = on ? onAccent() : muted();
                    break;
                case NEBEZPECNE:
                    pozadi = hover && on ? soft(danger(), 0.1f) : surface();
                    obrys = on ? soft(danger(), 0.45f) : line();
                    popredi = on ? danger() : muted();
                    break;
                case JEMNE:
                    pozadi = vybrane ? accentSoft() : (hover && on ? sunken() : null);
                    popredi = vybrane ? accent() : (on ? text2() : muted());
                    break;
                case DUCH:
                    pozadi = hover && on ? sunken() : null;
                    popredi = on ? text2() : muted();
                    break;
                default:
                    pozadi = !on ? surface2() : (hover ? surface2() : surface());
                    obrys = hover && on ? lineStrong() : line();
                    popredi = on ? text() : muted();
            }
            if (pozadi != null) {
                g2.setColor(pozadi);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
            }
            if (obrys != null) {
                g2.setColor(obrys);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 10, 10));
            }
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String t = getText() == null ? "" : getText();
            int sirkaObsahu = fm.stringWidth(t) + (ikona != null ? (t.isEmpty() ? 16 : 22) : 0);
            int x = (w - sirkaObsahu) / 2;
            if (ikona != null) {
                Ikona.kresli(g2, ikona, x, (h - 16) / 2, 16, popredi);
                x += t.isEmpty() ? 16 : 22;
            }
            g2.setColor(popredi);
            g2.drawString(t, x, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    static Tlacitko primarni(String text, String ikona, Runnable akce) {
        Tlacitko t = new Tlacitko(text, Styl.PRIMARNI, ikona);
        if (akce != null) t.addActionListener(e -> akce.run());
        return t;
    }

    static Tlacitko sekundarni(String text, String ikona, Runnable akce) {
        Tlacitko t = new Tlacitko(text, Styl.SEKUNDARNI, ikona);
        if (akce != null) t.addActionListener(e -> akce.run());
        return t;
    }

    static Tlacitko duch(String text, String ikona, Runnable akce) {
        Tlacitko t = new Tlacitko(text, Styl.DUCH, ikona);
        if (akce != null) t.addActionListener(e -> akce.run());
        return t;
    }

    // ------------------------------------------------------------------
    // štítek (chip)
    // ------------------------------------------------------------------

    static class Stitek extends JComponent {
        private final String text;
        private final Color barva;
        private final boolean plny;

        Stitek(String text, Color barva) {
            this(text, barva, false);
        }

        Stitek(String text, Color barva, boolean plny) {
            this.text = text;
            this.barva = barva;
            this.plny = plny;
            setFont(font(Font.BOLD, 11.5f));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(text) + 18, 22);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int h = 22, y = (getHeight() - h) / 2, w = getPreferredSize().width;
            g2.setColor(plny ? barva : soft(barva, tmavy ? 0.16f : 0.1f));
            g2.fill(new RoundRectangle2D.Float(0, y, w, h, h, h));
            if (!plny) {
                g2.setColor(soft(barva, 0.3f));
                g2.draw(new RoundRectangle2D.Float(0.5f, y + 0.5f, w - 1, h - 1, h, h));
            }
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(plny ? Color.WHITE : barva);
            g2.drawString(text, 9, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    static Stitek stitekPriority(Auto.Priorita p) {
        return new Stitek(p.getNazev(), priorita(p));
    }

    /** Šedý štítek „odhad“ / „simulace“, aby bylo vidět, že číslo není přesné. */
    static Stitek odhad(String text) {
        return new Stitek(text, muted());
    }

    // ------------------------------------------------------------------
    // SPZ jako tabulka
    // ------------------------------------------------------------------

    static class Spz extends JComponent {
        private final String text;

        Spz(String text) {
            this.text = text.isEmpty() ? "BEZ SPZ" : text;
            setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(text) + 30, 24);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getPreferredSize().width, h = 24, y = (getHeight() - h) / 2;
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Float(0, y, w - 1, h - 1, 6, 6));
            g2.setClip(new RoundRectangle2D.Float(0, y, w - 1, h - 1, 6, 6));
            g2.setColor(new Color(0, 51, 153));
            g2.fillRect(0, y, 12, h);
            g2.setClip(null);
            g2.setColor(new Color(40, 40, 40));
            g2.draw(new RoundRectangle2D.Float(0.5f, y + 0.5f, w - 2, h - 2, 6, 6));
            g2.setColor(new Color(255, 204, 0));
            g2.fill(new Ellipse2D.Float(4, y + 6, 4, 4));
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(new Color(20, 20, 20));
            g2.drawString(text, 18, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // avatar auta (barevný čtverec s ikonou)
    // ------------------------------------------------------------------

    static class Avatar extends JComponent {
        private final Color barva;
        private final int velikost;

        Avatar(Auto a, int velikost) {
            this.barva = new Color(CarModel.barvaAuta(a));
            this.velikost = velikost;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(velikost, velikost);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int s = velikost;
            g2.setColor(soft(barva, tmavy ? 0.28f : 0.16f));
            g2.fill(new RoundRectangle2D.Float(0, 0, s, s, s * 0.32f, s * 0.32f));
            g2.setColor(soft(barva, 0.45f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, s - 1, s - 1, s * 0.32f, s * 0.32f));
            int i = Math.round(s * 0.56f);
            Color ikona = barva;
            float jas = (barva.getRed() * 0.3f + barva.getGreen() * 0.59f + barva.getBlue() * 0.11f) / 255f;
            if (jas > 0.82f) ikona = muted();
            Ikona.kresli(g2, "car", (s - i) / 2, (s - i) / 2, i, ikona);
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // ukazatele
    // ------------------------------------------------------------------

    /** Vodorovný ukazatel 0..1, volitelně s textem uprostřed. */
    static class Ukazatel extends JComponent {
        private float hodnota;
        private Color barva;
        private String popis = "";
        private float znacka = -1;   // svislá čárka (např. limit), -1 = žádná

        Ukazatel(float hodnota, Color barva, int vyska) {
            this.hodnota = hodnota;
            this.barva = barva;
            setPreferredSize(new Dimension(80, vyska));
        }

        Ukazatel popis(String p) {
            this.popis = p;
            return this;
        }

        Ukazatel znacka(float z) {
            this.znacka = z;
            return this;
        }

        void nastav(float hodnota, Color barva) {
            this.hodnota = hodnota;
            this.barva = barva;
            repaint();
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getPreferredSize().height, y = (getHeight() - h) / 2;
            g2.setColor(sunken());
            g2.fill(new RoundRectangle2D.Float(0, y, w, h, h, h));
            float v = Math.max(0, Math.min(1, hodnota));
            int vypln = Math.round(w * v);
            if (vypln > 0) {
                g2.setClip(new RoundRectangle2D.Float(0, y, w, h, h, h));
                g2.setColor(barva);
                g2.fillRect(0, y, Math.max(vypln, h), h);
                g2.setClip(null);
            }
            if (znacka >= 0) {
                int zx = Math.round(w * Math.min(1, znacka));
                g2.setColor(text2());
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new Line2D.Float(zx, y - 3, zx, y + h + 3));
            }
            if (!popis.isEmpty()) {
                g2.setFont(font(Font.BOLD, Math.max(10, h - 10)));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(popis)) / 2;
                g2.setColor(tx + fm.stringWidth(popis) <= vypln ? Color.WHITE : text());
                g2.drawString(popis, tx, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }

    /** Kruhový ukazatel (např. palivo) s velkým číslem uprostřed. */
    static class Prstenec extends JComponent {
        private final float hodnota;
        private final Color barva;
        private final String velky, maly;
        private final int velikost;

        Prstenec(float hodnota, Color barva, String velky, String maly, int velikost) {
            this.hodnota = hodnota;
            this.barva = barva;
            this.velky = velky;
            this.maly = maly;
            this.velikost = velikost;
            setPreferredSize(new Dimension(velikost, velikost));
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            float s = velikost, t = s * 0.11f;
            float x = (getWidth() - s) / 2f + t / 2, y = (getHeight() - s) / 2f + t / 2;
            g2.setStroke(new BasicStroke(t, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(sunken());
            g2.draw(new Arc2D.Float(x, y, s - t, s - t, 0, 360, Arc2D.OPEN));
            g2.setColor(barva);
            float v = Math.max(0, Math.min(1, hodnota));
            if (v > 0) g2.draw(new Arc2D.Float(x, y, s - t, s - t, 90, -360 * v, Arc2D.OPEN));
            g2.setFont(serif(Font.BOLD, s * 0.24f));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(text());
            int cy = getHeight() / 2;
            g2.drawString(velky, (getWidth() - fm.stringWidth(velky)) / 2, cy + fm.getAscent() / 2 - (maly.isEmpty() ? 0 : 6));
            if (!maly.isEmpty()) {
                g2.setFont(font(Font.PLAIN, Math.max(10, s * 0.09f)));
                FontMetrics fm2 = g2.getFontMetrics();
                g2.setColor(muted());
                g2.drawString(maly, (getWidth() - fm2.stringWidth(maly)) / 2, cy + fm.getAscent() / 2 + fm2.getHeight() - 4);
            }
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // KPI dlaždice
    // ------------------------------------------------------------------

    static Karta kpi(String nadpis, String hodnota, String pod, Color barvaPod, String ikona) {
        Karta k = new Karta(new BorderLayout(0, 6), 16, 18);
        JPanel hlava = pruhledny(new BorderLayout(8, 0));
        hlava.add(eyebrow(nadpis), BorderLayout.CENTER);
        if (ikona != null) {
            JLabel ik = new JLabel(new Ikona(ikona, 16, muted()));
            hlava.add(ik, BorderLayout.EAST);
        }
        JLabel h = new JLabel(hodnota);
        h.setFont(serif(Font.BOLD, 30));
        h.setForeground(text());
        JLabel p = label(pod, Font.PLAIN, 12.5f, barvaPod == null ? muted() : barvaPod);
        k.add(hlava, BorderLayout.NORTH);
        k.add(h, BorderLayout.CENTER);
        k.add(p, BorderLayout.SOUTH);
        return k;
    }

    // ------------------------------------------------------------------
    // vstupní pole
    // ------------------------------------------------------------------

    /** Zaoblený obrys pole, při fokusu akcentová barva. */
    static class ObrysPole extends AbstractBorder {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            boolean fokus = c.hasFocus();
            g2.setColor(fokus ? accent() : lineStrong());
            g2.setStroke(new BasicStroke(fokus ? 1.6f : 1f));
            g2.draw(new RoundRectangle2D.Float(x + 0.8f, y + 0.8f, w - 1.6f, h - 1.6f, 10, 10));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(7, 11, 7, 11);
        }
    }

    static JTextField pole(int sloupce) {
        JTextField t = new JTextField(sloupce) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                aa(g2);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        stylPole(t);
        t.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                t.repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                t.repaint();
            }
        });
        return t;
    }

    static void stylPole(JTextField t) {
        t.setOpaque(false);
        t.setBackground(surface());
        t.setForeground(text());
        t.setCaretColor(text());
        t.setSelectionColor(soft(accent(), 0.3f));
        t.setSelectedTextColor(text());
        t.setFont(font(Font.PLAIN, 14));
        t.setBorder(new ObrysPole());
    }

    /** Pole s šedým zástupným textem, když je prázdné. */
    static JTextField hledani(String zastupny) {
        JTextField t = new JTextField(18) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                aa(g2);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 10, 10));
                Ikona.kresli(g2, "search", 11, (getHeight() - 15) / 2, 15, muted());
                g2.dispose();
                super.paintComponent(g);
                if (getText().isEmpty() && !hasFocus()) {
                    Graphics2D g3 = (Graphics2D) g.create();
                    aa(g3);
                    g3.setColor(muted());
                    g3.setFont(getFont());
                    FontMetrics fm = g3.getFontMetrics();
                    g3.drawString(zastupny, 34, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                    g3.dispose();
                }
            }
        };
        stylPole(t);
        t.setBackground(surface2());
        t.setBorder(BorderFactory.createCompoundBorder(new ObrysPole(), new EmptyBorder(0, 22, 0, 0)));
        t.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                t.repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                t.repaint();
            }
        });
        return t;
    }

    static <T> JComboBox<T> vyber(T[] polozky) {
        JComboBox<T> c = new JComboBox<>(polozky);
        stylVyber(c);
        return c;
    }

    static void stylVyber(JComboBox<?> c) {
        c.setFont(font(Font.PLAIN, 14));
        c.setBackground(surface());
        c.setForeground(text());
        c.setBorder(new ObrysPole());
        c.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton b = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        aa(g2);
                        Ikona.kresli(g2, "chevron-down", (getWidth() - 14) / 2, (getHeight() - 14) / 2, 14, muted());
                        g2.dispose();
                    }
                };
                b.setBorder(null);
                b.setContentAreaFilled(false);
                b.setFocusPainted(false);
                b.setOpaque(false);
                return b;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(surface());
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
        c.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, false);
                l.setBorder(new EmptyBorder(5, 6, 5, 6));
                l.setFont(font(Font.PLAIN, 14));
                l.setBackground(isSelected ? accentSoft() : surface());
                l.setForeground(isSelected && index >= 0 ? accent() : text());
                list.setBackground(surface());
                list.setSelectionBackground(accentSoft());
                return l;
            }
        });
    }

    /** Posuvník v barvách aplikace. */
    static JSlider posuvnik(int min, int max, int hodnota) {
        JSlider s = new JSlider(min, max, hodnota);
        s.setOpaque(false);
        s.setFocusable(false);
        s.setUI(new BasicSliderUI(s) {
            @Override
            public void paintTrack(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                aa(g2);
                int y = trackRect.y + trackRect.height / 2 - 3;
                g2.setColor(sunken());
                g2.fill(new RoundRectangle2D.Float(trackRect.x, y, trackRect.width, 6, 6, 6));
                g2.setColor(accent());
                g2.fill(new RoundRectangle2D.Float(trackRect.x, y, thumbRect.x + thumbRect.width / 2f - trackRect.x, 6, 6, 6));
                g2.dispose();
            }

            @Override
            public void paintThumb(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                aa(g2);
                int d = 16;
                int x = thumbRect.x + (thumbRect.width - d) / 2, y = thumbRect.y + (thumbRect.height - d) / 2;
                g2.setColor(surface());
                g2.fill(new Ellipse2D.Float(x, y, d, d));
                g2.setColor(accent());
                g2.setStroke(new BasicStroke(2.2f));
                g2.draw(new Ellipse2D.Float(x + 1, y + 1, d - 2, d - 2));
                g2.dispose();
            }

            @Override
            protected Dimension getThumbSize() {
                return new Dimension(18, 18);
            }

            @Override
            public void paintFocus(Graphics g) {
            }
        });
        return s;
    }

    // ------------------------------------------------------------------
    // posuvné oblasti
    // ------------------------------------------------------------------

    static JScrollPane scroll(JComponent obsah) {
        JScrollPane sp = new JScrollPane(obsah);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.getVerticalScrollBar().setUI(new TenkyScrollbar());
        sp.getHorizontalScrollBar().setUI(new TenkyScrollbar());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
        sp.getVerticalScrollBar().setOpaque(false);
        return sp;
    }

    static class TenkyScrollbar extends BasicScrollBarUI {
        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            g2.setColor(isThumbRollover() ? muted() : lineStrong());
            boolean svisle = scrollbar.getOrientation() == JScrollBar.VERTICAL;
            if (svisle) g2.fill(new RoundRectangle2D.Float(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6));
            else g2.fill(new RoundRectangle2D.Float(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6));
            g2.dispose();
        }

        private JButton nic() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected JButton createDecreaseButton(int o) {
            return nic();
        }

        @Override
        protected JButton createIncreaseButton(int o) {
            return nic();
        }
    }

    // ------------------------------------------------------------------
    // přepínač (segmentované tlačítko)
    // ------------------------------------------------------------------

    static class Prepinac extends JComponent {
        private final String[] polozky;
        private int vybrana;
        private int hover = -1;
        private final List<IntConsumer> posluchaci = new ArrayList<>();

        Prepinac(String[] polozky, int vybrana) {
            this.polozky = polozky;
            this.vybrana = vybrana;
            setFont(font(Font.BOLD, 12.5f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int i = index(e.getX());
                    if (i >= 0) {
                        Prepinac.this.vybrana = i;
                        repaint();
                        for (IntConsumer p : posluchaci) p.accept(i);
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    int i = index(e.getX());
                    if (i != hover) {
                        hover = i;
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = -1;
                    repaint();
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        void naZmenu(IntConsumer c) {
            posluchaci.add(c);
        }

        void setVybrana(int i) {
            vybrana = i;
            repaint();
        }

        private int[] sirky() {
            FontMetrics fm = getFontMetrics(getFont());
            int[] s = new int[polozky.length];
            for (int i = 0; i < polozky.length; i++) s[i] = fm.stringWidth(polozky[i]) + 26;
            return s;
        }

        private int index(int x) {
            int[] s = sirky();
            int pos = 3;
            for (int i = 0; i < s.length; i++) {
                if (x >= pos && x < pos + s[i]) return i;
                pos += s[i];
            }
            return -1;
        }

        @Override
        public Dimension getPreferredSize() {
            int w = 6;
            for (int s : sirky()) w += s;
            return new Dimension(w, 34);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            Dimension d = getPreferredSize();
            g2.setColor(sunken());
            g2.fill(new RoundRectangle2D.Float(0, 0, d.width, d.height, 11, 11));
            int[] s = sirky();
            int x = 3;
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            for (int i = 0; i < s.length; i++) {
                if (i == vybrana) {
                    g2.setColor(surface());
                    g2.fill(new RoundRectangle2D.Float(x, 3, s[i], d.height - 6, 8, 8));
                    g2.setColor(line());
                    g2.draw(new RoundRectangle2D.Float(x + 0.5f, 3.5f, s[i] - 1, d.height - 7, 8, 8));
                }
                g2.setColor(i == vybrana ? text() : (i == hover ? text2() : muted()));
                g2.drawString(polozky[i], x + 13, (d.height - fm.getHeight()) / 2 + fm.getAscent());
                x += s[i];
            }
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // oznámení (toast) v pravém dolním rohu okna
    // ------------------------------------------------------------------

    /** Jedno plovoucí oznámení; průhlednost se mění při objevení a mizení. */
    private static final class Toast extends JComponent {
        private final String text;
        private final Color barva;
        private float alfa = 0f;

        Toast(String text, Color barva) {
            this.text = text;
            this.barva = barva;
            setFont(font(Font.PLAIN, 13.5f));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, Math.min(1, alfa))));
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fill(new RoundRectangle2D.Float(2, 4, w - 4, h - 4, 12, 12));
            g2.setColor(tmavy ? c(0x232b36) : c(0x1a1f26));
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 2, h - 4, 12, 12));
            g2.setColor(barva);
            g2.fill(new Ellipse2D.Float(16, (h - 4) / 2f - 4, 8, 8));
            g2.setColor(new Color(0xf4f2ee));
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(zkrat(text, fm, w - 52), 34, (h - 4 - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private static final List<Toast> AKTIVNI = new ArrayList<>();

    static void toast(JFrame okno, String text, Color barva) {
        if (okno == null || !okno.isShowing()) return;
        JLayeredPane vrstva = okno.getLayeredPane();
        Toast t = new Toast(text, barva);
        FontMetrics fm = t.getFontMetrics(t.getFont());
        int w = Math.min(560, fm.stringWidth(text) + 56), h = 46;
        int posun = AKTIVNI.size() * (h + 6);
        t.setBounds(vrstva.getWidth() - w - 24, vrstva.getHeight() - h - 44 - posun, w, h);
        vrstva.add(t, JLayeredPane.POPUP_LAYER);
        AKTIVNI.add(t);
        long start = System.currentTimeMillis();
        Timer anim = new Timer(16, null);
        anim.addActionListener(e -> {
            long uplynulo = System.currentTimeMillis() - start;
            if (uplynulo < 180) t.alfa = uplynulo / 180f;
            else if (uplynulo < 3400) t.alfa = 1f;
            else t.alfa = 1f - (uplynulo - 3400) / 300f;
            t.repaint();
            if (t.alfa <= 0) {
                anim.stop();
                AKTIVNI.remove(t);
                vrstva.remove(t);
                vrstva.repaint();
            }
        });
        anim.start();
    }

    // ------------------------------------------------------------------
    // tenká oddělovací čára
    // ------------------------------------------------------------------

    static JComponent cara() {
        JComponent c = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(line());
                g.fillRect(0, getHeight() / 2, getWidth(), 1);
            }
        };
        c.setPreferredSize(new Dimension(10, 9));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 9));
        return c;
    }

    // ------------------------------------------------------------------
    // ikony kreslené čarou (nepotřebují žádné obrázky)
    // ------------------------------------------------------------------

    static final class Ikona implements Icon {
        private final String nazev;
        private final int velikost;
        private final Color barva;

        Ikona(String nazev, int velikost, Color barva) {
            this.nazev = nazev;
            this.velikost = velikost;
            this.barva = barva;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            kresli(g2, nazev, x, y, velikost, barva);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return velikost;
        }

        @Override
        public int getIconHeight() {
            return velikost;
        }

        /** Nakreslí ikonu do čtverce (x, y, s). Kreslí se v mřížce 24×24 a pak se zmenší. */
        static void kresli(Graphics2D g0, String nazev, int x, int y, int s, Color barva) {
            Graphics2D g = (Graphics2D) g0.create();
            g.translate(x, y);
            g.scale(s / 24.0, s / 24.0);
            g.setColor(barva);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (nazev) {
                case "dashboard":
                    g.draw(new RoundRectangle2D.Float(3, 3, 7, 9, 2, 2));
                    g.draw(new RoundRectangle2D.Float(14, 3, 7, 5, 2, 2));
                    g.draw(new RoundRectangle2D.Float(14, 12, 7, 9, 2, 2));
                    g.draw(new RoundRectangle2D.Float(3, 16, 7, 5, 2, 2));
                    break;
                case "car": {
                    GeneralPath p = new GeneralPath();
                    p.moveTo(3, 16);
                    p.lineTo(3, 12.5);
                    p.lineTo(5.5, 7.5);
                    p.quadTo(6.2, 6, 8, 6);
                    p.lineTo(16, 6);
                    p.quadTo(17.8, 6, 18.5, 7.5);
                    p.lineTo(21, 12.5);
                    p.lineTo(21, 16);
                    p.closePath();
                    g.draw(p);
                    g.draw(new Line2D.Float(4, 12, 20, 12));
                    g.fill(new Ellipse2D.Float(5.5f, 15f, 4.5f, 4.5f));
                    g.fill(new Ellipse2D.Float(14f, 15f, 4.5f, 4.5f));
                    break;
                }
                case "priority":
                    g.draw(new Line2D.Float(4, 20, 4, 13));
                    g.draw(new Line2D.Float(10, 20, 10, 8));
                    g.draw(new Line2D.Float(16, 20, 16, 4));
                    g.draw(new Line2D.Float(21, 20, 21, 11));
                    break;
                case "wrench": {
                    GeneralPath p = new GeneralPath();
                    p.moveTo(14.5, 6.5);
                    p.curveTo(13, 3.5, 17, 1.8, 19.5, 3.2);
                    p.lineTo(16.5, 6.2);
                    p.lineTo(17.8, 7.5);
                    p.lineTo(20.8, 4.5);
                    p.curveTo(22.2, 7, 20.5, 11, 17.5, 9.5);
                    p.lineTo(7, 20);
                    p.curveTo(5.8, 21.2, 2.8, 18.2, 4, 17);
                    p.closePath();
                    g.draw(p);
                    break;
                }
                case "coins":
                    g.draw(new Ellipse2D.Float(3, 4, 12, 5));
                    g.draw(new Line2D.Float(3, 6.5f, 3, 15));
                    g.draw(new Line2D.Float(15, 6.5f, 15, 9));
                    g.draw(new Arc2D.Float(3, 12.5f, 12, 5, 180, 180, Arc2D.OPEN));
                    g.draw(new Ellipse2D.Float(9, 11, 12, 5));
                    g.draw(new Line2D.Float(9, 13.5f, 9, 18));
                    g.draw(new Line2D.Float(21, 13.5f, 21, 18));
                    g.draw(new Arc2D.Float(9, 15.5f, 12, 5, 180, 180, Arc2D.OPEN));
                    break;
                case "map": {
                    GeneralPath p = new GeneralPath();
                    p.moveTo(3, 6);
                    p.lineTo(9, 4);
                    p.lineTo(15, 6);
                    p.lineTo(21, 4);
                    p.lineTo(21, 18);
                    p.lineTo(15, 20);
                    p.lineTo(9, 18);
                    p.lineTo(3, 20);
                    p.closePath();
                    g.draw(p);
                    g.draw(new Line2D.Float(9, 4, 9, 18));
                    g.draw(new Line2D.Float(15, 6, 15, 20));
                    break;
                }
                case "globe":
                    g.draw(new Ellipse2D.Float(3, 3, 18, 18));
                    g.draw(new Ellipse2D.Float(8, 3, 8, 18));
                    g.draw(new Line2D.Float(3, 12, 21, 12));
                    break;
                case "history":
                    g.draw(new Arc2D.Float(3, 3, 18, 18, 140, 310, Arc2D.OPEN));
                    g.draw(new Line2D.Float(3.5f, 5, 4.5f, 9));
                    g.draw(new Line2D.Float(4.5f, 9, 8.5f, 8));
                    g.draw(new Line2D.Float(12, 7.5f, 12, 12));
                    g.draw(new Line2D.Float(12, 12, 15, 14));
                    break;
                case "trophy": {
                    g.draw(new Arc2D.Float(6, -2, 12, 16, 180, 180, Arc2D.OPEN));
                    g.draw(new Line2D.Float(6, 6, 6, 4));
                    g.draw(new Line2D.Float(18, 6, 18, 4));
                    g.draw(new Line2D.Float(6, 4, 18, 4));
                    g.draw(new Arc2D.Float(2, 4, 6, 6, 90, 180, Arc2D.OPEN));
                    g.draw(new Arc2D.Float(16, 4, 6, 6, 270, 180, Arc2D.OPEN));
                    g.draw(new Line2D.Float(12, 14, 12, 18));
                    g.draw(new Line2D.Float(8, 20, 16, 20));
                    break;
                }
                case "book":
                    g.draw(new RoundRectangle2D.Float(5, 3, 14, 18, 2, 2));
                    g.draw(new Line2D.Float(9, 3, 9, 21));
                    g.draw(new Line2D.Float(12, 8, 16, 8));
                    g.draw(new Line2D.Float(12, 12, 16, 12));
                    break;
                case "search":
                    g.draw(new Ellipse2D.Float(4, 4, 12, 12));
                    g.draw(new Line2D.Float(14.5f, 14.5f, 20, 20));
                    break;
                case "bell": {
                    GeneralPath p = new GeneralPath();
                    p.moveTo(6, 16);
                    p.lineTo(6, 10.5);
                    p.curveTo(6, 6.5, 8.7, 4, 12, 4);
                    p.curveTo(15.3, 4, 18, 6.5, 18, 10.5);
                    p.lineTo(18, 16);
                    p.lineTo(20, 18);
                    p.lineTo(4, 18);
                    p.closePath();
                    g.draw(p);
                    g.draw(new Arc2D.Float(9.5f, 18.5f, 5, 4, 180, 180, Arc2D.OPEN));
                    break;
                }
                case "sun":
                    g.draw(new Ellipse2D.Float(8, 8, 8, 8));
                    for (int i = 0; i < 8; i++) {
                        double a = Math.PI * i / 4;
                        g.draw(new Line2D.Double(12 + Math.cos(a) * 7, 12 + Math.sin(a) * 7,
                                12 + Math.cos(a) * 9.5, 12 + Math.sin(a) * 9.5));
                    }
                    break;
                case "moon": {
                    GeneralPath p = new GeneralPath();
                    p.moveTo(19, 14.5);
                    p.curveTo(14, 16.5, 8, 12, 10, 4.5);
                    p.curveTo(5.5, 5.8, 3.5, 10.5, 5, 14.5);
                    p.curveTo(7, 19.5, 15, 21, 19, 14.5);
                    p.closePath();
                    g.draw(p);
                    break;
                }
                case "plus":
                    g.draw(new Line2D.Float(12, 5, 12, 19));
                    g.draw(new Line2D.Float(5, 12, 19, 12));
                    break;
                case "fuel":
                    g.draw(new RoundRectangle2D.Float(4, 4, 10, 17, 2, 2));
                    g.draw(new Line2D.Float(4, 10, 14, 10));
                    g.draw(new Line2D.Float(14, 8, 17, 8));
                    g.draw(new Line2D.Float(17, 8, 19, 10));
                    g.draw(new Line2D.Float(19, 10, 19, 17));
                    g.draw(new Arc2D.Float(15, 15, 4, 4, 180, 180, Arc2D.OPEN));
                    g.draw(new Line2D.Float(15, 17, 15, 13));
                    break;
                case "pin": {
                    GeneralPath p = new GeneralPath();
                    p.moveTo(12, 21);
                    p.curveTo(12, 21, 5, 14.5, 5, 9.5);
                    p.curveTo(5, 5.5, 8.2, 3, 12, 3);
                    p.curveTo(15.8, 3, 19, 5.5, 19, 9.5);
                    p.curveTo(19, 14.5, 12, 21, 12, 21);
                    p.closePath();
                    g.draw(p);
                    g.draw(new Ellipse2D.Float(9.5f, 7, 5, 5));
                    break;
                }
                case "edit":
                    g.draw(new Line2D.Float(4, 20, 8, 19));
                    g.draw(new Line2D.Float(4, 20, 5, 16));
                    g.draw(new Line2D.Float(5, 16, 16, 5));
                    g.draw(new Line2D.Float(8, 19, 19, 8));
                    g.draw(new Line2D.Float(16, 5, 19, 8));
                    break;
                case "trash":
                    g.draw(new Line2D.Float(4, 7, 20, 7));
                    g.draw(new Line2D.Float(9, 7, 9, 4));
                    g.draw(new Line2D.Float(9, 4, 15, 4));
                    g.draw(new Line2D.Float(15, 4, 15, 7));
                    g.draw(new RoundRectangle2D.Float(6, 7, 12, 14, 2, 2));
                    g.draw(new Line2D.Float(10, 11, 10, 17));
                    g.draw(new Line2D.Float(14, 11, 14, 17));
                    break;
                case "route":
                    g.draw(new Ellipse2D.Float(3, 15, 5, 5));
                    g.draw(new Ellipse2D.Float(16, 4, 5, 5));
                    g.draw(new Line2D.Float(8, 17.5f, 15, 17.5f));
                    g.draw(new Arc2D.Float(11, 11.5f, 8, 6, 270, 180, Arc2D.OPEN));
                    g.draw(new Arc2D.Float(6, 6.5f, 8, 5, 90, 180, Arc2D.OPEN));
                    g.draw(new Line2D.Float(10, 6.5f, 16, 6.5f));
                    break;
                case "cube": {
                    GeneralPath p = new GeneralPath();
                    p.moveTo(12, 3);
                    p.lineTo(20, 7.5);
                    p.lineTo(20, 16.5);
                    p.lineTo(12, 21);
                    p.lineTo(4, 16.5);
                    p.lineTo(4, 7.5);
                    p.closePath();
                    g.draw(p);
                    g.draw(new Line2D.Float(4, 7.5f, 12, 12));
                    g.draw(new Line2D.Float(20, 7.5f, 12, 12));
                    g.draw(new Line2D.Float(12, 12, 12, 21));
                    break;
                }
                case "save":
                    g.draw(new RoundRectangle2D.Float(4, 4, 16, 16, 3, 3));
                    g.draw(new Line2D.Float(8, 4, 8, 9));
                    g.draw(new Line2D.Float(8, 9, 15, 9));
                    g.draw(new Line2D.Float(15, 9, 15, 4));
                    g.draw(new RoundRectangle2D.Float(8, 13, 8, 7, 1, 1));
                    break;
                case "close":
                    g.draw(new Line2D.Float(6, 6, 18, 18));
                    g.draw(new Line2D.Float(18, 6, 6, 18));
                    break;
                case "check":
                    g.draw(new Line2D.Float(5, 12.5f, 10, 17.5f));
                    g.draw(new Line2D.Float(10, 17.5f, 19.5f, 7));
                    break;
                case "alert": {
                    GeneralPath p = new GeneralPath();
                    p.moveTo(12, 3.5);
                    p.lineTo(21.5, 20);
                    p.lineTo(2.5, 20);
                    p.closePath();
                    g.draw(p);
                    g.draw(new Line2D.Float(12, 10, 12, 14));
                    g.fill(new Ellipse2D.Float(11, 16, 2, 2));
                    break;
                }
                case "chevron-down":
                    g.draw(new Line2D.Float(6, 9, 12, 15));
                    g.draw(new Line2D.Float(12, 15, 18, 9));
                    break;
                case "chevron-right":
                    g.draw(new Line2D.Float(9, 6, 15, 12));
                    g.draw(new Line2D.Float(15, 12, 9, 18));
                    break;
                case "calendar":
                    g.draw(new RoundRectangle2D.Float(3, 5, 18, 16, 3, 3));
                    g.draw(new Line2D.Float(3, 10, 21, 10));
                    g.draw(new Line2D.Float(8, 3, 8, 7));
                    g.draw(new Line2D.Float(16, 3, 16, 7));
                    break;
                case "settings":
                    g.draw(new Ellipse2D.Float(9, 9, 6, 6));
                    for (int i = 0; i < 8; i++) {
                        double a = Math.PI * i / 4;
                        g.draw(new Line2D.Double(12 + Math.cos(a) * 6, 12 + Math.sin(a) * 6,
                                12 + Math.cos(a) * 9, 12 + Math.sin(a) * 9));
                    }
                    g.draw(new Ellipse2D.Float(5.5f, 5.5f, 13, 13));
                    break;
                case "download":
                    g.draw(new Line2D.Float(12, 4, 12, 15));
                    g.draw(new Line2D.Float(7, 10, 12, 15));
                    g.draw(new Line2D.Float(17, 10, 12, 15));
                    g.draw(new Line2D.Float(4, 20, 20, 20));
                    break;
                case "play": {
                    GeneralPath p = new GeneralPath();
                    p.moveTo(7, 4.5);
                    p.lineTo(19, 12);
                    p.lineTo(7, 19.5);
                    p.closePath();
                    g.draw(p);
                    break;
                }
                case "pause":
                    g.draw(new Line2D.Float(8, 5, 8, 19));
                    g.draw(new Line2D.Float(16, 5, 16, 19));
                    break;
                case "swap":
                    g.draw(new Line2D.Float(4, 8, 18, 8));
                    g.draw(new Line2D.Float(14, 4, 18, 8));
                    g.draw(new Line2D.Float(14, 12, 18, 8));
                    g.draw(new Line2D.Float(20, 16, 6, 16));
                    g.draw(new Line2D.Float(10, 12, 6, 16));
                    g.draw(new Line2D.Float(10, 20, 6, 16));
                    break;
                case "keyboard":
                    g.draw(new RoundRectangle2D.Float(2, 6, 20, 12, 3, 3));
                    g.draw(new Line2D.Float(7, 14, 17, 14));
                    g.fill(new Ellipse2D.Float(5.5f, 9, 2, 2));
                    g.fill(new Ellipse2D.Float(9.5f, 9, 2, 2));
                    g.fill(new Ellipse2D.Float(13.5f, 9, 2, 2));
                    g.fill(new Ellipse2D.Float(17f, 9, 2, 2));
                    break;
                case "upload":
                    g.draw(new Line2D.Float(12, 15, 12, 4));
                    g.draw(new Line2D.Float(7, 9, 12, 4));
                    g.draw(new Line2D.Float(17, 9, 12, 4));
                    g.draw(new Line2D.Float(4, 20, 20, 20));
                    break;
                default:
                    g.draw(new Ellipse2D.Float(5, 5, 14, 14));
            }
            g.dispose();
        }
    }
}
