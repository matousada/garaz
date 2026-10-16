package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/** Jednoduché grafy kreslené přes Graphics2D. Barvy berou z tokenů Ui, takže fungují v obou režimech. */
final class Grafy {

    private Grafy() {
    }

    /** Hezká horní mez osy (1, 2, 2.5, 5 × 10^n). */
    static double mez(double max) {
        if (max <= 0) return 1;
        double e = Math.pow(10, Math.floor(Math.log10(max)));
        for (double k : new double[]{1, 2, 2.5, 5, 10}) {
            if (k * e >= max) return k * e;
        }
        return 10 * e;
    }

    static String kratce(double v) {
        if (v >= 1_000_000) return String.format("%.1f mil.", v / 1_000_000).replace('.', ',');
        if (v >= 1000) return Math.round(v / 1000) + " tis.";
        return String.valueOf(Math.round(v));
    }

    // ------------------------------------------------------------------
    // skládaný sloupcový graf (např. náklady po měsících: palivo + servis)
    // ------------------------------------------------------------------

    static class Sloupce extends JComponent {
        private final String[] popisky;
        private final double[][] rady;     // [řada][sloupec]
        private final String[] nazvyRad;
        private final Color[] barvy;
        private final String jednotka;
        private int hover = -1;

        Sloupce(String[] popisky, double[][] rady, String[] nazvyRad, Color[] barvy, String jednotka) {
            this.popisky = popisky;
            this.rady = rady;
            this.nazvyRad = nazvyRad;
            this.barvy = barvy;
            this.jednotka = jednotka;
            setPreferredSize(new Dimension(520, 240));
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int i = sloupec(e.getX());
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

        private static final int VLEVO = 52, DOLE = 26, NAHORE = 30;

        private int sloupec(int x) {
            int n = popisky.length;
            if (n == 0) return -1;
            double krok = (getWidth() - VLEVO - 8) / (double) n;
            int i = (int) ((x - VLEVO) / krok);
            return i >= 0 && i < n ? i : -1;
        }

        private double soucet(int i) {
            double s = 0;
            for (double[] r : rady) s += r[i];
            return s;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Ui.aa(g2);
            int w = getWidth(), h = getHeight(), n = popisky.length;
            double max = 0;
            for (int i = 0; i < n; i++) max = Math.max(max, soucet(i));
            double horni = mez(max);
            int vyska = h - DOLE - NAHORE;

            // legenda
            g2.setFont(Ui.font(Font.PLAIN, 12));
            FontMetrics fm = g2.getFontMetrics();
            int lx = VLEVO;
            for (int r = 0; r < nazvyRad.length; r++) {
                g2.setColor(barvy[r]);
                g2.fill(new RoundRectangle2D.Float(lx, 6, 10, 10, 3, 3));
                g2.setColor(Ui.text2());
                g2.drawString(nazvyRad[r], lx + 15, 6 + fm.getAscent() - 2);
                lx += fm.stringWidth(nazvyRad[r]) + 32;
            }

            // mřížka
            g2.setFont(Ui.font(Font.PLAIN, 11));
            fm = g2.getFontMetrics();
            for (int k = 0; k <= 4; k++) {
                int y = NAHORE + vyska - (int) Math.round(vyska * k / 4.0);
                g2.setColor(Ui.line());
                g2.draw(new Line2D.Float(VLEVO, y, w - 8, y));
                g2.setColor(Ui.muted());
                String t = kratce(horni * k / 4);
                g2.drawString(t, VLEVO - 8 - fm.stringWidth(t), y + fm.getAscent() / 2 - 1);
            }
            if (n == 0) {
                g2.dispose();
                return;
            }

            double krok = (w - VLEVO - 8) / (double) n;
            int sirka = (int) Math.max(6, Math.min(34, krok * 0.58));
            for (int i = 0; i < n; i++) {
                int x = (int) (VLEVO + krok * i + (krok - sirka) / 2);
                double y = NAHORE + vyska;
                if (i == hover) {
                    g2.setColor(Ui.soft(Ui.accent(), 0.06f));
                    g2.fillRect((int) (VLEVO + krok * i), NAHORE, (int) krok, vyska);
                }
                for (int r = 0; r < rady.length; r++) {
                    double v = rady[r][i];
                    if (v <= 0) continue;
                    double vy = vyska * v / horni;
                    g2.setColor(hover >= 0 && hover != i ? Ui.soft(barvy[r], 0.55f) : barvy[r]);
                    boolean posledni = true;
                    for (int r2 = r + 1; r2 < rady.length; r2++) if (rady[r2][i] > 0) posledni = false;
                    if (posledni) {
                        g2.fill(new RoundRectangle2D.Double(x, y - vy, sirka, vy, 6, 6));
                        g2.fill(new Rectangle.Double(x, y - Math.min(vy, 3), sirka, Math.min(vy, 3)));
                    } else {
                        g2.fill(new Rectangle.Double(x, y - vy, sirka, vy));
                    }
                    y -= vy;
                }
                g2.setColor(i == hover ? Ui.text() : Ui.muted());
                String p = popisky[i];
                g2.drawString(p, (int) (VLEVO + krok * i + (krok - fm.stringWidth(p)) / 2), h - 8);
            }

            // bublina s hodnotami
            if (hover >= 0) {
                g2.setFont(Ui.font(Font.PLAIN, 12));
                fm = g2.getFontMetrics();
                String[] radky = new String[rady.length + 1];
                radky[0] = popisky[hover] + ": " + Ui.tis(soucet(hover)) + " " + jednotka;
                for (int r = 0; r < rady.length; r++) {
                    radky[r + 1] = nazvyRad[r] + " " + Ui.tis(rady[r][hover]) + " " + jednotka;
                }
                int bw = 0;
                for (String s : radky) bw = Math.max(bw, fm.stringWidth(s));
                bw += 34;
                int bh = radky.length * 18 + 12;
                int bx = (int) (VLEVO + krok * hover + krok / 2 + 10);
                if (bx + bw > w - 4) bx = (int) (VLEVO + krok * hover + krok / 2 - bw - 10);
                int by = NAHORE + 4;
                g2.setColor(Ui.surface());
                g2.fill(new RoundRectangle2D.Float(bx, by, bw, bh, 10, 10));
                g2.setColor(Ui.lineStrong());
                g2.draw(new RoundRectangle2D.Float(bx + 0.5f, by + 0.5f, bw - 1, bh - 1, 10, 10));
                for (int k = 0; k < radky.length; k++) {
                    int ty = by + 8 + k * 18 + fm.getAscent();
                    if (k > 0) {
                        g2.setColor(barvy[k - 1]);
                        g2.fill(new Ellipse2D.Float(bx + 12, ty - 9, 8, 8));
                    }
                    g2.setColor(k == 0 ? Ui.text() : Ui.text2());
                    g2.setFont(Ui.font(k == 0 ? Font.BOLD : Font.PLAIN, 12));
                    g2.drawString(radky[k], bx + (k == 0 ? 12 : 26), ty);
                }
            }
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // vodorovné pruhy s popiskem (žebříček)
    // ------------------------------------------------------------------

    static class Pruhy extends JComponent {
        private final String[] popisky;
        private final double[] hodnoty;
        private final String[] texty;
        private final Color[] barvy;
        private static final int RADEK = 30;

        Pruhy(String[] popisky, double[] hodnoty, String[] texty, Color[] barvy) {
            this.popisky = popisky;
            this.hodnoty = hodnoty;
            this.texty = texty;
            this.barvy = barvy;
            setPreferredSize(new Dimension(400, Math.max(1, popisky.length) * RADEK + 4));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Ui.aa(g2);
            g2.setFont(Ui.font(Font.PLAIN, 13));
            FontMetrics fm = g2.getFontMetrics();
            int w = getWidth();
            int popisekW = Math.min(170, w / 3), textW = 96;
            double max = 0;
            for (double v : hodnoty) max = Math.max(max, v);
            if (max <= 0) max = 1;
            int sirka = Math.max(40, w - popisekW - textW - 16);
            for (int i = 0; i < popisky.length; i++) {
                int y = i * RADEK;
                g2.setColor(Ui.text());
                g2.drawString(Ui.zkrat(popisky[i], fm, popisekW - 10), 0, y + 19);
                g2.setColor(Ui.sunken());
                g2.fill(new RoundRectangle2D.Float(popisekW, y + 9, sirka, 12, 12, 12));
                g2.setColor(barvy[Math.min(i, barvy.length - 1)]);
                g2.fill(new RoundRectangle2D.Float(popisekW, y + 9, Math.max(12, (float) (sirka * hodnoty[i] / max)), 12, 12, 12));
                g2.setColor(Ui.text2());
                String t = texty[i];
                g2.drawString(t, w - fm.stringWidth(t), y + 19);
            }
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // malá křivka (trend)
    // ------------------------------------------------------------------

    static class Krivka extends JComponent {
        private final double[] body;
        private final Color barva;

        Krivka(double[] body, Color barva, int w, int h) {
            this.body = body;
            this.barva = barva;
            setPreferredSize(new Dimension(w, h));
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (body.length < 2) return;
            Graphics2D g2 = (Graphics2D) g.create();
            Ui.aa(g2);
            int w = getWidth(), h = getHeight();
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            for (double b : body) {
                min = Math.min(min, b);
                max = Math.max(max, b);
            }
            if (max - min < 1e-9) {
                max += 1;
                min -= 1;
            }
            GeneralPath p = new GeneralPath();
            GeneralPath plocha = new GeneralPath();
            for (int i = 0; i < body.length; i++) {
                double x = 2 + (w - 4) * i / (double) (body.length - 1);
                double y = 3 + (h - 6) * (1 - (body[i] - min) / (max - min));
                if (i == 0) {
                    p.moveTo(x, y);
                    plocha.moveTo(x, h);
                    plocha.lineTo(x, y);
                } else {
                    p.lineTo(x, y);
                    plocha.lineTo(x, y);
                }
            }
            plocha.lineTo(w - 2, h);
            plocha.closePath();
            g2.setColor(Ui.soft(barva, 0.12f));
            g2.fill(plocha);
            g2.setColor(barva);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(p);
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // rozklad skóre na faktory (jeden vodorovný pruh složený z dílů)
    // ------------------------------------------------------------------

    static class Rozklad extends JComponent {
        private final double[] dily;
        private final Color[] barvy;

        Rozklad(double[] dily, Color[] barvy) {
            this.dily = dily;
            this.barvy = barvy;
            setPreferredSize(new Dimension(200, 12));
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 12);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Ui.aa(g2);
            int w = getWidth(), h = 12, y = (getHeight() - h) / 2;
            g2.setColor(Ui.sunken());
            g2.fill(new RoundRectangle2D.Float(0, y, w, h, h, h));
            g2.setClip(new RoundRectangle2D.Float(0, y, w, h, h, h));
            double x = 0;
            for (int i = 0; i < dily.length; i++) {
                double d = w * dily[i] / 100.0;
                g2.setColor(barvy[i % barvy.length]);
                g2.fill(new Rectangle.Double(x, y, d, h));
                x += d;
                g2.setColor(Ui.surface());
                g2.fill(new Rectangle.Double(x - 1, y, 1, h));
            }
            g2.dispose();
        }
    }

    /** Paleta pro faktory skóre (rozlišitelné v obou režimech). */
    static Color[] paletaFaktoru() {
        if (Ui.tmavy) {
            return new Color[]{new Color(0x2bb3b1), new Color(0x60a5fa), new Color(0xc084fc),
                    new Color(0xfbbf24), new Color(0xf97316), new Color(0xf87171)};
        }
        return new Color[]{new Color(0x0e7c7b), new Color(0x2563eb), new Color(0x7c3aed),
                new Color(0xca8a04), new Color(0xc2410c), new Color(0xb91c1c)};
    }
}
