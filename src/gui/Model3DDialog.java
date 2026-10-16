package gui;

import data.Auto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

/** Otáčivé 3D auto. Typ karoserie se pozná z názvu, barvu laku lze měnit (a uloží se k autu). */
final class Model3DDialog extends JDialog {

    // světlo v souřadnicích kamery (zleva shora od diváka) a polovinový vektor pro odlesk
    private static final double LX = -0.4534, LY = 0.655, LZ = -0.6045;
    private static final double HX = -0.2531, HY = 0.3657, HZ = -0.8957;

    private final Auto auto;
    private final CarModel.Detekce detekce;
    private CarModel.Typ typ;
    private CarModel model;
    private CarModel.Sestava sestava;
    private int rgb;
    private boolean zmeneno;

    private final View3D view = new View3D();
    private final List<JButton> vzorky = new ArrayList<>();
    private final JComboBox<CarModel.Typ> typBox = new JComboBox<>(CarModel.Typ.values());

    Model3DDialog(Frame owner, Auto auto) {
        super(owner, "3D model auta", true);
        this.auto = auto;
        this.detekce = CarModel.detekuj(auto);
        this.typ = detekce.typ;
        this.rgb = CarModel.barvaAuta(auto);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        typBox.setSelectedItem(typ);
        typBox.setBackground(MainFrame.cPole());
        typBox.setForeground(MainFrame.cText());
        typBox.addActionListener(e -> {
            typ = (CarModel.Typ) typBox.getSelectedItem();
            postav();
        });

        JPanel pravy = new JPanel();
        pravy.setOpaque(false);
        pravy.setLayout(new BoxLayout(pravy, BoxLayout.Y_AXIS));
        pravy.setPreferredSize(new Dimension(270, 100));
        pravy.add(lev(lbl(auto.getJmenoAuta(), Font.BOLD, 20, "text")));
        String spz = auto.getSpz().isEmpty() ? "bez SPZ" : auto.getSpz();
        String rok = auto.getRokVyroby() > 0 ? ", rok " + auto.getRokVyroby() : "";
        pravy.add(lev(lbl(spz + rok, Font.PLAIN, 12, "muted")));
        pravy.add(Box.createVerticalStrut(12));

        pravy.add(lev(lbl("Typ karoserie", Font.BOLD, 13, "text")));
        typBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        pravy.add(lev(typBox));
        String zdroj = detekce.podleNazvu
                ? "Typ rozpoznán z názvu (" + detekce.klic + ")."
                : "Typ se z názvu nepoznal, je to jen odhad podle spotřeby a nádrže. Vyber správný typ ručně.";
        pravy.add(Box.createVerticalStrut(4));
        pravy.add(lev(lbl("<html><div style='width:240px'>" + zdroj + " Typ se neukládá.</div></html>",
                Font.PLAIN, 12, "muted")));
        pravy.add(Box.createVerticalStrut(12));

        pravy.add(lev(lbl("Barva laku", Font.BOLD, 13, "text")));
        pravy.add(Box.createVerticalStrut(4));
        JPanel mrizka = new JPanel(new GridLayout(2, 7, 5, 5));
        mrizka.setOpaque(false);
        for (int i = 0; i < CarModel.BARVY.length; i++) {
            final int c = CarModel.BARVY[i];
            JButton b = new JButton();
            b.setToolTipText(CarModel.BARVY_JMENA[i]);
            b.setBackground(new Color(c));
            b.setOpaque(true);
            b.setContentAreaFilled(true);
            b.setFocusPainted(false);
            b.setPreferredSize(new Dimension(30, 30));
            b.addActionListener(e -> vyberBarvu(c));
            vzorky.add(b);
            mrizka.add(b);
        }
        mrizka.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        pravy.add(lev(mrizka));
        pravy.add(Box.createVerticalStrut(6));

        JButton vlastni = mala("Vlastní…", new Color(59, 130, 246));
        JButton automaticka = mala("Automatická", new Color(100, 116, 139));
        vlastni.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Barva laku", new Color(rgb));
            if (c != null) vyberBarvu(c.getRGB() & 0xFFFFFF);
        });
        automaticka.addActionListener(e -> {
            auto.setBarva(-1);
            zmeneno = true;
            rgb = CarModel.vychoziBarva(auto);
            postav();
        });
        JPanel barvyTlac = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        barvyTlac.setOpaque(false);
        barvyTlac.add(vlastni);
        barvyTlac.add(automaticka);
        barvyTlac.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        pravy.add(lev(barvyTlac));
        pravy.add(Box.createVerticalStrut(12));

        JCheckBox otacet = new JCheckBox("Automatické otáčení", true);
        otacet.setOpaque(false);
        otacet.setFont(f(Font.PLAIN, 13));
        MainFrame.role(otacet, "text");
        otacet.addActionListener(e -> {
            if (otacet.isSelected()) view.otaceni.start();
            else view.otaceni.stop();
        });
        pravy.add(lev(otacet));
        pravy.add(Box.createVerticalStrut(8));

        pravy.add(lev(lbl("Pohled", Font.BOLD, 13, "text")));
        pravy.add(Box.createVerticalStrut(4));
        JPanel pohledy = new JPanel(new GridLayout(2, 2, 6, 6));
        pohledy.setOpaque(false);
        pohledy.add(pohled("Zepředu", -Math.PI / 2, 0.15));
        pohledy.add(pohled("Z boku", 0, 0.12));
        pohledy.add(pohled("Zezadu", Math.PI / 2, 0.15));
        pohledy.add(pohled("Shora", -0.5, 1.35));
        pohledy.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        pravy.add(lev(pohledy));
        pravy.add(Box.createVerticalGlue());
        pravy.add(lev(lbl("<html><div style='width:240px'>Tažení myší otáčí autem, kolečko přibližuje. "
                + "Model je zjednodušený podle typu karoserie, nejde o přesný model vašeho auta.</div></html>",
                Font.PLAIN, 12, "muted")));

        JButton zavrit = new MainFrame.StyledButton("Zavřít", new Color(71, 85, 105));
        zavrit.addActionListener(e -> dispose());
        JPanel dole = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        dole.setOpaque(false);
        dole.add(zavrit);

        JPanel obal = new JPanel(new BorderLayout());
        obal.setBorder(new LineBorder(MainFrame.cObrys()));
        obal.add(view, BorderLayout.CENTER);

        JPanel root = MainFrame.role(new JPanel(new BorderLayout(14, 10)), "bg");
        root.setBorder(new EmptyBorder(14, 16, 14, 16));
        root.add(obal, BorderLayout.CENTER);
        root.add(pravy, BorderLayout.EAST);
        root.add(dole, BorderLayout.SOUTH);
        setContentPane(root);
        MainFrame.tema(root);

        postav();
        view.otaceni.start();
        pack();
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(owner);
    }

    /** True, když se změnila barva auta (hlavní okno pak uloží data). */
    boolean isZmeneno() {
        return zmeneno;
    }

    @Override
    public void dispose() {
        view.otaceni.stop();
        super.dispose();
    }

    private void vyberBarvu(int c) {
        rgb = c & 0xFFFFFF;
        auto.setBarva(rgb);
        zmeneno = true;
        postav();
    }

    private void postav() {
        model = CarModel.proTyp(typ);
        sestava = model.postav(rgb);
        for (int i = 0; i < vzorky.size(); i++) {
            boolean vybrana = CarModel.BARVY[i] == rgb;
            vzorky.get(i).setBorder(vybrana ? new LineBorder(MainFrame.cText(), 3)
                    : new LineBorder(MainFrame.cObrys(), 1));
        }
        view.repaint();
    }

    private JButton pohled(String text, double yaw, double pitch) {
        JButton b = mala(text, new Color(100, 116, 139));
        b.addActionListener(e -> {
            view.yaw = yaw;
            view.pitch = pitch;
            view.repaint();
        });
        return b;
    }

    private static Font f(int styl, int vel) {
        return new Font(Font.SANS_SERIF, styl, vel);
    }

    private static JLabel lbl(String t, int styl, int vel, String role) {
        JLabel l = new JLabel(t);
        l.setFont(f(styl, vel));
        if (role != null) MainFrame.role(l, role);
        return l;
    }

    private static <T extends JComponent> T lev(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static JButton mala(String text, Color c) {
        JButton b = new MainFrame.StyledButton(text, c);
        b.setPreferredSize(new Dimension(100, 28));
        b.setFont(f(Font.BOLD, 12));
        return b;
    }

    private static int sc(int c, float k) {
        int r = Math.max(0, Math.min(255, Math.round(((c >> 16) & 255) * k)));
        int g = Math.max(0, Math.min(255, Math.round(((c >> 8) & 255) * k)));
        int b = Math.max(0, Math.min(255, Math.round((c & 255) * k)));
        return (r << 16) | (g << 8) | b;
    }

    // ------------------------------------------------------------------
    // 3D zobrazení (vlastní jednoduchý renderer)
    // ------------------------------------------------------------------

    private final class View3D extends JPanel {
        double yaw = -0.62, pitch = 0.26, zoom = 1.0;
        boolean tahne;
        final Timer otaceni = new Timer(25, e -> {
            if (!tahne) {
                yaw += 0.012;
                repaint();
            }
        });

        private int W, H;
        private double cyw, syw, cp, sp, zc, D, F;
        private final double[][] pt = new double[3][5];
        private final double[] pa = new double[5], pb = new double[5], pc = new double[5];

        View3D() {
            setPreferredSize(new Dimension(640, 480));
            setOpaque(true);
            MouseAdapter ma = new MouseAdapter() {
                Point p0;

                @Override
                public void mousePressed(MouseEvent e) {
                    p0 = e.getPoint();
                    tahne = true;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (p0 == null) return;
                    yaw += (e.getX() - p0.x) * 0.012;
                    pitch = Math.max(0.02, Math.min(1.45, pitch + (e.getY() - p0.y) * 0.008));
                    p0 = e.getPoint();
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    tahne = false;
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    zoom = Math.max(0.5, Math.min(3.5, zoom * Math.pow(1.1, -e.getPreciseWheelRotation())));
                    repaint();
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
            addMouseWheelListener(ma);
        }

        /** Souřadnice modelu -> obrazovka. o = {X, Y, hloubka, x v kameře, y v kameře}. */
        private void proj(double x, double y, double z, double[] o) {
            double xr = x * cyw - y * syw;
            double yr = x * syw + y * cyw;
            double zr = z - zc;
            double su = yr * sp + zr * cp;
            double dep = Math.max(0.05, yr * cp - zr * sp + D);
            o[0] = W / 2.0 + F * xr / dep;
            o[1] = H * 0.52 - F * su / dep;
            o[2] = dep;
            o[3] = xr;
            o[4] = su;
        }

        private Polygon elipsa(double rxx, double ryy, double z, int n) {
            Polygon p = new Polygon();
            for (int i = 0; i < n; i++) {
                double a = 2 * Math.PI * i / n;
                proj(rxx * Math.cos(a), ryy * Math.sin(a), z, pa);
                p.addPoint((int) Math.round(pa[0]), (int) Math.round(pa[1]));
            }
            return p;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            W = getWidth();
            H = getHeight();
            g.setPaint(new GradientPaint(0, 0, MainFrame.cKarta(), 0, H, MainFrame.cPozadi()));
            g.fillRect(0, 0, W, H);
            if (sestava == null) {
                g.dispose();
                return;
            }
            cyw = Math.cos(yaw);
            syw = Math.sin(yaw);
            cp = Math.cos(pitch);
            sp = Math.sin(pitch);
            zc = model.height() * 0.4;
            D = model.len() * 1.05 / zoom + 1.5;
            F = Math.min(W * 0.95, H * 1.5);
            boolean tm = MainFrame.jeTmavy();

            // podlaha a stín
            Polygon podlaha = elipsa(model.len() * 0.85, model.len() * 0.85, 0, 56);
            g.setColor(tm ? new Color(24, 32, 46) : new Color(222, 229, 238));
            g.fillPolygon(podlaha);
            g.setColor(MainFrame.cObrys());
            g.drawPolygon(podlaha);
            g.setColor(new Color(0, 0, 0, tm ? 120 : 70));
            g.fillPolygon(elipsa(model.len() * 0.5, model.halfW() * 1.1, 0.005, 36));

            // trojúhelníky: nejdřív karoserie, potom nálepky (okna, kola, světla) v pořadí vzniku
            for (int pass = 0; pass < 2; pass++) {
                for (CarModel.Tri tr : sestava.tri) {
                    if ((tr.bias > 0) != (pass == 1)) continue;
                    double mx = 0, mu = 0, md = 0;
                    for (int k = 0; k < 3; k++) {
                        proj(tr.v[3 * k], tr.v[3 * k + 1], tr.v[3 * k + 2], pt[k]);
                        mx += pt[k][3];
                        mu += pt[k][4];
                        md += pt[k][2];
                    }
                    mx /= 3;
                    mu /= 3;
                    md /= 3;
                    double nxr = tr.nx * cyw - tr.ny * syw, nyr = tr.nx * syw + tr.ny * cyw;
                    double ncx = nxr, ncy = nyr * sp + tr.nz * cp, ncz = nyr * cp - tr.nz * sp;
                    if (ncx * mx + ncy * mu + ncz * md >= 0) continue;   // odvrácená strana
                    if (tr.side != 0) {                                   // kolo na odvrácené straně auta
                        double oy = tr.side * cyw, ox = -tr.side * syw;
                        if (ox * mx + oy * sp * mu + oy * cp * md >= 0) continue;
                    }
                    double diff = Math.max(0, ncx * LX + ncy * LY + ncz * LZ);
                    double spec = Math.pow(Math.max(0, ncx * HX + ncy * HY + ncz * HZ), 30);
                    int c;
                    switch (tr.mode) {
                        case 1:
                            c = CarModel.mixRgb(sc(tr.rgb, 0.55f + 0.45f * (float) diff), 0x9DB8D6,
                                    (float) Math.min(0.85, 0.12 + 0.30 * diff + 0.6 * spec));
                            break;
                        case 2:
                            c = CarModel.mixRgb(tr.rgb, 0xFFFFFF, 0.12f);
                            break;
                        case 3:
                            c = sc(tr.rgb, 0.55f + 0.45f * (float) diff);
                            break;
                        default:
                            c = CarModel.mixRgb(sc(tr.rgb, 0.36f + 0.64f * (float) diff), 0xFFFFFF,
                                    (float) Math.min(0.6, spec * 0.5));
                    }
                    int[] xs = {(int) Math.round(pt[0][0]), (int) Math.round(pt[1][0]), (int) Math.round(pt[2][0])};
                    int[] ys = {(int) Math.round(pt[0][1]), (int) Math.round(pt[1][1]), (int) Math.round(pt[2][1])};
                    g.setColor(new Color(c));
                    g.fillPolygon(xs, ys, 3);
                    g.drawPolygon(xs, ys, 3);
                }
            }

            // text SPZ na tabulkách
            String text = auto.getSpz().isEmpty() ? "—" : auto.getSpz().toUpperCase();
            Font pismo = new Font(Font.SANS_SERIF, Font.BOLD, 100);
            double sirka = pismo.getStringBounds(text, new FontRenderContext(null, true, true)).getWidth();
            double meritko = Math.min(0.00085, 0.40 / Math.max(1, sirka));
            for (CarModel.Znacka z : sestava.znacky) {
                proj(z.x, 0, z.z, pa);
                double nxr = z.smer * cyw, nyr = z.smer * syw;
                double ncx = nxr, ncy = nyr * sp, ncz = nyr * cp;
                if (ncx * pa[3] + ncy * pa[4] + ncz * pa[2] >= 0) continue;   // tabulka je otočená pryč
                double doprava = z.smer > 0 ? 1 : -1;                          // pravá ruka diváka
                proj(z.x, 0.1 * doprava, z.z, pb);
                proj(z.x, 0, z.z + 0.1, pc);
                double rxx = (pb[0] - pa[0]) * 10, ryy = (pb[1] - pa[1]) * 10;
                double uxx = (pc[0] - pa[0]) * 10, uyy = (pc[1] - pa[1]) * 10;
                Graphics2D g2 = (Graphics2D) g.create();
                AffineTransform at = new AffineTransform(rxx, ryy, -uxx, -uyy, pa[0], pa[1]);
                at.translate(0.025, 0);
                at.scale(meritko, meritko);
                g2.transform(at);
                g2.setFont(pismo);
                g2.setColor(new Color(0x14141A));
                g2.drawString(text, (float) (-sirka / 2), 36f);
                g2.dispose();
            }
            g.dispose();
        }
    }
}