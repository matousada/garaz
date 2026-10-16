package gui;

import data.Auto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Mapa ČR: výběr zastávek, nejrychlejší / nejkratší trasa, náklady a animace jízdy. */
final class MapaDialog extends JDialog {

    private static final Color MODRA = new Color(59, 130, 246);
    private static final Color ZELENA = new Color(34, 165, 89);
    private static final Color ORANZOVA = new Color(245, 158, 11);
    private static final Color CERVENA = new Color(229, 72, 77);

    private final Auto auto;
    private final float cena;
    private boolean potvrzeno;
    private int kmVysledek;
    private boolean tamZpetVysledek;
    private String popisVysledek = "";

    private final List<Integer> zastavky = new ArrayList<>();
    private final DefaultListModel<MapaData.Mesto> model = new DefaultListModel<>();
    private final JList<MapaData.Mesto> seznam = new JList<>(model);
    private final JComboBox<MapaData.Mesto> vyber;
    private final JRadioButton rychla = new JRadioButton("Nejrychlejší", true);
    private final JRadioButton kratka = new JRadioButton("Nejkratší");
    private final JCheckBox tamZpet = new JCheckBox("Tam a zpět", true);
    private final JLabel info = new JLabel();
    private final MapPanel mapa = new MapPanel();
    private final JButton prehratBtn, odjetBtn;

    private MapaData.Trasa trasa;
    private double[] rx = new double[0], ry = new double[0], rc = new double[0];
    private double progress = -1;
    private Timer anim;

    MapaDialog(Frame owner, Auto auto, float cena) {
        super(owner, "Mapa a plánování trasy", true);
        this.auto = auto;
        this.cena = cena;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        Collator col = Collator.getInstance(Locale.forLanguageTag("cs-CZ"));
        List<MapaData.Mesto> abeceda = new ArrayList<>(MapaData.MESTA);
        abeceda.sort((x, y) -> col.compare(x.nazev, y.nazev));
        vyber = new JComboBox<>(abeceda.toArray(new MapaData.Mesto[0]));
        vyber.setMaximumRowCount(14);
        vyber.setBackground(MainFrame.cPole());
        vyber.setForeground(MainFrame.cText());

        seznam.setBackground(MainFrame.cPole());
        seznam.setForeground(MainFrame.cText());
        seznam.setFont(f(Font.PLAIN, 13));
        seznam.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean fo) {
                String pref = i == 0 ? "Start: " : (i == model.size() - 1 ? "Cíl: " : (i + 1) + ". ");
                return super.getListCellRendererComponent(l, pref + v, i, s, fo);
            }
        });

        // ---- pravý panel ----
        JButton pridejBtn = mala("Přidat", ZELENA);
        JButton odeberBtn = mala("Odebrat", CERVENA);
        JButton otocBtn = mala("Otočit", new Color(100, 116, 139));
        JButton smazBtn = mala("Vymazat", new Color(100, 116, 139));
        prehratBtn = new MainFrame.StyledButton("▶ Přehrát jízdu", MODRA);
        odjetBtn = new MainFrame.StyledButton("Odjet na cestu", ZELENA);
        prehratBtn.setFont(f(Font.BOLD, 13));
        odjetBtn.setFont(f(Font.BOLD, 13));

        JPanel ovladani = new JPanel();
        ovladani.setOpaque(false);
        ovladani.setLayout(new BoxLayout(ovladani, BoxLayout.Y_AXIS));
        ovladani.add(lev(lbl("Plánování trasy", Font.BOLD, 22, "text")));
        ovladani.add(lev(lbl("Auto: " + auto.getJmenoAuta(), Font.PLAIN, 12, "muted")));
        ovladani.add(Box.createVerticalStrut(10));

        JPanel radekVyber = new JPanel(new BorderLayout(6, 0));
        radekVyber.setOpaque(false);
        radekVyber.add(vyber, BorderLayout.CENTER);
        radekVyber.add(pridejBtn, BorderLayout.EAST);
        radekVyber.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        ovladani.add(lev(radekVyber));
        ovladani.add(Box.createVerticalStrut(8));
        ovladani.add(lev(lbl("Zastávky (první = start, poslední = cíl):", Font.PLAIN, 12, "muted")));

        JScrollPane sp = new JScrollPane(seznam);
        sp.setBorder(new LineBorder(MainFrame.cObrys()));
        sp.setPreferredSize(new Dimension(280, 120));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        ovladani.add(lev(sp));
        ovladani.add(Box.createVerticalStrut(6));

        JPanel tlac = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tlac.setOpaque(false);
        tlac.add(odeberBtn);
        tlac.add(otocBtn);
        tlac.add(smazBtn);
        tlac.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        ovladani.add(lev(tlac));
        ovladani.add(Box.createVerticalStrut(8));

        ButtonGroup skupina = new ButtonGroup();
        skupina.add(rychla);
        skupina.add(kratka);
        for (AbstractButton b : new AbstractButton[]{rychla, kratka, tamZpet}) {
            b.setOpaque(false);
            b.setFont(f(Font.PLAIN, 13));
            MainFrame.role((JComponent) b, "text");
        }
        JPanel radio = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        radio.setOpaque(false);
        radio.add(rychla);
        radio.add(kratka);
        radio.add(tamZpet);
        radio.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        ovladani.add(lev(radio));

        JPanel infoKarta = MainFrame.role(new JPanel(new BorderLayout()), "cardSmall");
        infoKarta.add(info, BorderLayout.NORTH);
        JScrollPane infoScroll = new JScrollPane(infoKarta);
        infoScroll.setBorder(null);
        infoScroll.setOpaque(false);
        infoScroll.getViewport().setOpaque(false);

        JPanel akce = new JPanel(new GridLayout(1, 2, 8, 0));
        akce.setOpaque(false);
        akce.add(prehratBtn);
        akce.add(odjetBtn);

        JPanel pravy = new JPanel(new BorderLayout(0, 8));
        pravy.setOpaque(false);
        pravy.setPreferredSize(new Dimension(300, 100));
        pravy.add(ovladani, BorderLayout.NORTH);
        pravy.add(infoScroll, BorderLayout.CENTER);
        pravy.add(akce, BorderLayout.SOUTH);

        // ---- spodní lišta ----
        JButton zavrit = new MainFrame.StyledButton("Zavřít", new Color(71, 85, 105));
        zavrit.addActionListener(e -> dispose());
        JPanel dole = new JPanel(new BorderLayout());
        dole.setOpaque(false);
        dole.add(lbl("Kolečko = přiblížení, tažení = posun mapy, klik na město = přidat zastávku, "
                + "pravé tlačítko = odebrat. Mapa je schematická.", Font.PLAIN, 12, "muted"), BorderLayout.WEST);
        dole.add(zavrit, BorderLayout.EAST);

        mapa.setPreferredSize(new Dimension(780, 540));
        JPanel mapaObal = new JPanel(new BorderLayout());
        mapaObal.setBorder(new LineBorder(MainFrame.cObrys()));
        mapaObal.add(mapa, BorderLayout.CENTER);

        JPanel root = MainFrame.role(new JPanel(new BorderLayout(14, 10)), "bg");
        root.setBorder(new EmptyBorder(14, 16, 14, 16));
        root.add(mapaObal, BorderLayout.CENTER);
        root.add(pravy, BorderLayout.EAST);
        root.add(dole, BorderLayout.SOUTH);
        setContentPane(root);
        MainFrame.tema(root);

        // ---- akce ----
        pridejBtn.addActionListener(e -> {
            MapaData.Mesto m = (MapaData.Mesto) vyber.getSelectedItem();
            if (m != null) pridej(m.id);
        });
        odeberBtn.addActionListener(e -> {
            int i = seznam.getSelectedIndex();
            if (i < 0 && !zastavky.isEmpty()) i = zastavky.size() - 1;
            if (i >= 0) {
                zastavky.remove(i);
                zmena();
            }
        });
        otocBtn.addActionListener(e -> {
            Collections.reverse(zastavky);
            zmena();
        });
        smazBtn.addActionListener(e -> {
            zastavky.clear();
            zmena();
        });
        rychla.addActionListener(e -> prepocitej());
        kratka.addActionListener(e -> prepocitej());
        tamZpet.addActionListener(e -> aktualizujInfo());
        prehratBtn.addActionListener(e -> prehraj());
        odjetBtn.addActionListener(e -> {
            if (trasa == null) return;
            int km = (int) Math.max(1, Math.round(trasa.km));
            boolean tz = tamZpet.isSelected();
            int v = JOptionPane.showConfirmDialog(this,
                    "Vyrazit na cestu " + km + " km" + (tz ? " (tam a zpět " + (2 * km) + " km)" : "")
                            + " s autem " + auto.getJmenoAuta() + "?",
                    "Odjet na cestu", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (v == JOptionPane.YES_OPTION) {
                potvrzeno = true;
                kmVysledek = km;
                tamZpetVysledek = tz;
                popisVysledek = popisTrasy();
                dispose();
            }
        });

        aktualizujInfo();
        pack();
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(owner);
    }

    boolean isPotvrzeno() {
        return potvrzeno;
    }

    /** Vzdálenost jedním směrem v km (zaokrouhlená). */
    int getKm() {
        return kmVysledek;
    }

    boolean isTamZpet() {
        return tamZpetVysledek;
    }

    String getPopis() {
        return popisVysledek;
    }

    @Override
    public void dispose() {
        if (anim != null) anim.stop();
        super.dispose();
    }

    // ------------------------------------------------------------------
    // pomocné
    // ------------------------------------------------------------------

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
        b.setPreferredSize(new Dimension(86, 28));
        b.setFont(f(Font.BOLD, 12));
        return b;
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String fmt(double v, int dec) {
        return String.format(Locale.ROOT, "%." + dec + "f", v).replace('.', ',');
    }

    private static String cas(double h) {
        int min = (int) Math.round(h * 60);
        return min < 60 ? min + " min" : (min / 60) + " h " + (min % 60) + " min";
    }

    private String popisTrasy() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < zastavky.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(MapaData.MESTA.get(zastavky.get(i)).nazev);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // trasa
    // ------------------------------------------------------------------

    private void pridej(int id) {
        if (!zastavky.isEmpty() && zastavky.get(zastavky.size() - 1) == id) return;
        zastavky.add(id);
        zmena();
    }

    private void odeber(int id) {
        int i = zastavky.lastIndexOf(id);
        if (i >= 0) {
            zastavky.remove(i);
            zmena();
        }
    }

    private void zmena() {
        model.clear();
        for (int id : zastavky) model.addElement(MapaData.MESTA.get(id));
        prepocitej();
    }

    private void prepocitej() {
        if (anim != null) anim.stop();
        progress = -1;
        trasa = zastavky.size() >= 2 ? MapaData.najdi(zastavky, rychla.isSelected()) : null;
        int n = trasa == null ? 0 : trasa.uzly.size();
        rx = new double[n];
        ry = new double[n];
        rc = new double[n];
        for (int i = 0; i < n; i++) {
            MapaData.Mesto m = MapaData.MESTA.get(trasa.uzly.get(i));
            rx[i] = MapaData.mx(m.lon);
            ry[i] = MapaData.my(m.lat);
            if (i > 0) rc[i] = rc[i - 1] + Math.hypot(rx[i] - rx[i - 1], ry[i] - ry[i - 1]);
        }
        aktualizujInfo();
        if (trasa != null) mapa.zoomTrasa();
        mapa.repaint();
    }

    private void prehraj() {
        if (trasa == null || rc.length < 2 || rc[rc.length - 1] <= 0) return;
        if (anim != null) anim.stop();
        progress = 0;
        final double krok = Math.max(0.0015, 2.6 / rc[rc.length - 1]);
        anim = new Timer(16, e -> {
            progress += krok;
            if (progress >= 1) {
                progress = 1;
                anim.stop();
            }
            mapa.repaint();
        });
        anim.start();
    }

    private void radek(StringBuilder sb, String seda, String k, String v) {
        sb.append("<span style='color:").append(seda).append("'>").append(k).append(":</span> <b>")
                .append(v).append("</b><br>");
    }

    private void aktualizujInfo() {
        String txt = hex(MainFrame.cText()), seda = hex(MainFrame.cSeda());
        String oran = "#d97706", cerv = "#dc2626";
        StringBuilder sb = new StringBuilder("<html><div style='width:245px;color:" + txt + "'>");
        boolean ok = false;
        if (zastavky.size() < 2) {
            sb.append("<span style='color:").append(seda).append("'>Vyber start a cíl: klikni na města v mapě, ")
                    .append("nebo je přidej z nabídky nahoře.</span>");
        } else if (trasa == null) {
            sb.append("<span style='color:").append(cerv)
                    .append("'>Mezi vybranými místy se nepodařilo najít silniční spojení.</span>");
        } else {
            ok = true;
            boolean tz = tamZpet.isSelected();
            int nas = tz ? 2 : 1;
            double celkem = trasa.km * nas;
            double spotrebaL = auto.getSpotreba() * celkem / 100.0;
            double maxP = auto.getMaxPalivo();
            int tank = maxP <= 0 ? 0 : Math.max(0, (int) Math.ceil((spotrebaL - auto.getPalivo()) / maxP));
            double zbude = auto.getPalivo() + tank * maxP - spotrebaL;
            double hod = trasa.hodiny * nas + tank * 0.25;

            sb.append("<b>").append(esc(popisTrasy())).append("</b><br>");
            List<String> pres = new ArrayList<>();
            for (int id : trasa.uzly) {
                if (!zastavky.contains(id)) pres.add(MapaData.MESTA.get(id).nazev);
            }
            if (!pres.isEmpty()) {
                String p = String.join(", ", pres.subList(0, Math.min(8, pres.size())));
                sb.append("<span style='color:").append(seda).append("'>přes ").append(esc(p))
                        .append(pres.size() > 8 ? "…" : "").append("</span><br>");
            }
            sb.append("<br>");
            radek(sb, seda, "Vzdálenost", fmt(trasa.km, 0) + " km" + (tz ? " (celkem " + fmt(celkem, 0) + " km)" : ""));
            radek(sb, seda, "Z toho dálnice", fmt(trasa.kmDalnice, 0) + " km");
            radek(sb, seda, "Čas jízdy", cas(hod));
            radek(sb, seda, "Spotřeba", fmt(spotrebaL, 1) + " l");
            radek(sb, seda, "Cena paliva", fmt(spotrebaL * cena, 0) + " Kč");
            radek(sb, seda, "Tankování po cestě", tank + "×");
            radek(sb, seda, "Po cestě zbude", fmt(Math.max(0, zbude), 1) + " l");
            if (tank > 0) {
                sb.append("<br><span style='color:").append(oran).append("'>Na cestu je potřeba natankovat.</span>");
            }
            if (auto.getKmOdServisu() + celkem > auto.getServisniInterval()) {
                sb.append("<br><span style='color:").append(cerv).append("'>Po cestě bude potřeba servis.</span>");
            }
        }
        sb.append("</div></html>");
        info.setText(sb.toString());
        prehratBtn.setEnabled(ok);
        odjetBtn.setEnabled(ok);
    }

    // ------------------------------------------------------------------
    // mapa
    // ------------------------------------------------------------------

    private final class MapPanel extends JPanel {
        double cx, cy, scale = 1.4;
        boolean nafitovano;
        int hover = -1;

        MapPanel() {
            setOpaque(true);
            MouseAdapter ma = new MouseAdapter() {
                Point last;
                double ujeto;

                @Override
                public void mousePressed(MouseEvent e) {
                    last = e.getPoint();
                    ujeto = 0;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (last == null) return;
                    int dx = e.getX() - last.x, dy = e.getY() - last.y;
                    ujeto += Math.abs(dx) + Math.abs(dy);
                    cx -= dx / scale;
                    cy -= dy / scale;
                    last = e.getPoint();
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (last == null || ujeto > 4) return;
                    int id = najdi(e.getPoint());
                    if (id < 0) return;
                    if (SwingUtilities.isRightMouseButton(e)) odeber(id);
                    else pridej(id);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    int id = najdi(e.getPoint());
                    if (id != hover) {
                        hover = id;
                        setCursor(id >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                        repaint();
                    }
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    double faktor = Math.pow(1.18, -e.getPreciseWheelRotation());
                    double ns = Math.max(0.6, Math.min(16, scale * faktor));
                    double kx = cx + (e.getX() - getWidth() / 2.0) / scale;
                    double ky = cy + (e.getY() - getHeight() / 2.0) / scale;
                    scale = ns;
                    cx = kx - (e.getX() - getWidth() / 2.0) / scale;
                    cy = ky - (e.getY() - getHeight() / 2.0) / scale;
                    repaint();
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
            addMouseWheelListener(ma);
        }

        double sx(double kx) {
            return getWidth() / 2.0 + (kx - cx) * scale;
        }

        double sy(double ky) {
            return getHeight() / 2.0 + (ky - cy) * scale;
        }

        int najdi(Point p) {
            int best = -1;
            double bd = 13 * 13;
            for (MapaData.Mesto m : MapaData.MESTA) {
                double dx = sx(MapaData.mx(m.lon)) - p.x, dy = sy(MapaData.my(m.lat)) - p.y;
                double d = dx * dx + dy * dy;
                if (d < bd) {
                    bd = d;
                    best = m.id;
                }
            }
            return best;
        }

        void fit() {
            double x0 = 1e9, x1 = -1e9, y0 = 1e9, y1 = -1e9;
            for (double[] h : MapaData.HRANICE) {
                double x = MapaData.mx(h[0]), y = MapaData.my(h[1]);
                x0 = Math.min(x0, x);
                x1 = Math.max(x1, x);
                y0 = Math.min(y0, y);
                y1 = Math.max(y1, y);
            }
            cx = (x0 + x1) / 2;
            cy = (y0 + y1) / 2;
            scale = Math.max(0.6, Math.min((getWidth() - 30) / (x1 - x0), (getHeight() - 30) / (y1 - y0)));
            nafitovano = true;
        }

        void zoomTrasa() {
            if (rx.length < 2 || getWidth() <= 0) return;
            double x0 = 1e9, x1 = -1e9, y0 = 1e9, y1 = -1e9;
            for (int i = 0; i < rx.length; i++) {
                x0 = Math.min(x0, rx[i]);
                x1 = Math.max(x1, rx[i]);
                y0 = Math.min(y0, ry[i]);
                y1 = Math.max(y1, ry[i]);
            }
            cx = (x0 + x1) / 2;
            cy = (y0 + y1) / 2;
            double s = Math.min((getWidth() - 160) / (x1 - x0 + 1), (getHeight() - 160) / (y1 - y0 + 1));
            scale = Math.max(0.8, Math.min(9, s));
        }

        private void popisek(Graphics2D g, String text, double x, double y, boolean tm) {
            g.setFont(f(Font.BOLD, 11));
            g.setColor(tm ? new Color(31, 41, 55, 225) : new Color(255, 255, 255, 225));
            for (int dx = -1; dx <= 1; dx += 2) {
                for (int dy = -1; dy <= 1; dy += 2) g.drawString(text, (float) (x + dx), (float) (y + dy));
            }
            g.setColor(tm ? new Color(226, 232, 240) : new Color(30, 41, 59));
            g.drawString(text, (float) x, (float) y);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (!nafitovano && getWidth() > 0) fit();
            boolean tm = MainFrame.jeTmavy();

            g.setColor(tm ? new Color(13, 20, 33) : new Color(198, 222, 240));
            g.fillRect(0, 0, getWidth(), getHeight());

            Path2D.Double zeme = new Path2D.Double();
            boolean prvni = true;
            for (double[] h : MapaData.HRANICE) {
                double x = sx(MapaData.mx(h[0])), y = sy(MapaData.my(h[1]));
                if (prvni) {
                    zeme.moveTo(x, y);
                    prvni = false;
                } else {
                    zeme.lineTo(x, y);
                }
            }
            zeme.closePath();
            g.setColor(tm ? new Color(31, 41, 55) : new Color(244, 241, 230));
            g.fill(zeme);
            g.setColor(tm ? new Color(120, 134, 156) : new Color(120, 130, 142));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(zeme);

            // silnice (nejdřív běžné, nad nimi dálnice)
            for (int pass = 0; pass < 2; pass++) {
                boolean dal = pass == 1;
                g.setColor(dal ? (tm ? new Color(240, 130, 70) : new Color(232, 106, 60))
                        : (tm ? new Color(92, 102, 120) : new Color(176, 178, 186)));
                float w = dal ? (float) Math.max(2.4, Math.min(5.0, scale * 1.7))
                        : (float) Math.max(1.4, Math.min(3.0, scale * 0.9));
                g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (MapaData.Hrana h : MapaData.HRANY) {
                    if (h.dalnice != dal) continue;
                    MapaData.Mesto a = MapaData.MESTA.get(h.a), b = MapaData.MESTA.get(h.b);
                    g.draw(new Line2D.Double(sx(MapaData.mx(a.lon)), sy(MapaData.my(a.lat)),
                            sx(MapaData.mx(b.lon)), sy(MapaData.my(b.lat))));
                }
            }

            // vybraná trasa
            if (rx.length >= 2) {
                Path2D.Double cesta = new Path2D.Double();
                for (int i = 0; i < rx.length; i++) {
                    if (i == 0) cesta.moveTo(sx(rx[i]), sy(ry[i]));
                    else cesta.lineTo(sx(rx[i]), sy(ry[i]));
                }
                g.setColor(new Color(255, 255, 255, tm ? 120 : 230));
                g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(cesta);
                g.setColor(MODRA);
                g.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(cesta);
            }

            // města
            for (MapaData.Mesto m : MapaData.MESTA) {
                double x = sx(MapaData.mx(m.lon)), y = sy(MapaData.my(m.lat));
                if (x < -30 || y < -30 || x > getWidth() + 30 || y > getHeight() + 30) continue;
                double r = m.velikost == 2 ? 5.5 : (m.velikost == 1 ? 4.2 : 3.2);
                Ellipse2D.Double kolo = new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r);
                g.setColor(tm ? new Color(226, 232, 240) : Color.WHITE);
                g.fill(kolo);
                g.setColor(tm ? new Color(15, 23, 42) : new Color(51, 65, 85));
                g.setStroke(new BasicStroke(1.5f));
                g.draw(kolo);
                boolean zast = zastavky.contains(m.id);
                if (m.velikost == 2 || (m.velikost == 1 && scale >= 1.15) || scale >= 2.6 || zast || m.id == hover) {
                    popisek(g, m.nazev, x + r + 3, y + 4, tm);
                }
            }

            // značky zastávek
            for (int i = 0; i < zastavky.size(); i++) {
                MapaData.Mesto m = MapaData.MESTA.get(zastavky.get(i));
                double x = sx(MapaData.mx(m.lon)), y = sy(MapaData.my(m.lat));
                Color c = i == 0 ? ZELENA : (i == zastavky.size() - 1 ? CERVENA : ORANZOVA);
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(x - 12, y - 12, 24, 24));
                g.setColor(c);
                g.fill(new Ellipse2D.Double(x - 10, y - 10, 20, 20));
                g.setColor(Color.WHITE);
                g.setFont(f(Font.BOLD, 12));
                String t = String.valueOf(i + 1);
                g.drawString(t, (float) (x - g.getFontMetrics().stringWidth(t) / 2.0), (float) (y + 4.5));
            }

            // jedoucí auto
            if (progress >= 0 && rc.length >= 2 && rc[rc.length - 1] > 0) {
                double target = progress * rc[rc.length - 1];
                int i = 1;
                while (i < rc.length - 1 && rc[i] < target) i++;
                double seg = rc[i] - rc[i - 1];
                double t = seg <= 0 ? 0 : Math.max(0, Math.min(1, (target - rc[i - 1]) / seg));
                double px = rx[i - 1] + (rx[i] - rx[i - 1]) * t, py = ry[i - 1] + (ry[i] - ry[i - 1]) * t;
                double ang = Math.atan2(ry[i] - ry[i - 1], rx[i] - rx[i - 1]);
                AffineTransform old = g.getTransform();
                g.translate(sx(px), sy(py));
                g.rotate(ang);
                g.setColor(Color.WHITE);
                g.fillRoundRect(-12, -7, 24, 14, 8, 8);
                g.setColor(new Color(CarModel.barvaAuta(auto)));
                g.fillRoundRect(-11, -6, 22, 12, 7, 7);
                g.setColor(new Color(0, 0, 0, 120));
                g.fillRoundRect(0, -4, 6, 8, 2, 2);
                g.setTransform(old);
            }

            // měřítko
            int d100 = (int) Math.round(100 * scale);
            int bx = getWidth() - 20 - d100, by = getHeight() - 18;
            g.setColor(tm ? new Color(226, 232, 240) : new Color(51, 65, 85));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(bx, by, bx + d100, by);
            g.drawLine(bx, by - 4, bx, by + 4);
            g.drawLine(bx + d100, by - 4, bx + d100, by + 4);
            g.setFont(f(Font.PLAIN, 11));
            g.drawString("100 km", bx + d100 / 2 - 18, by - 6);
            g.dispose();
        }
    }
}