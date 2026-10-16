package gui;

import data.Auto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.text.Collator;
import java.text.Normalizer;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Mapa světa: výběr dvou měst, nejkratší cesta po zeměkouli, místní časy, den a noc, odhad cesty autem. */
final class SvetMapaDialog extends JDialog {

    private static final Color MODRA = new Color(59, 130, 246);
    private static final Color ZELENA = new Color(34, 165, 89);
    private static final Color CERVENA = new Color(229, 72, 77);
    private static final Color ORANZOVA = new Color(245, 158, 11);

    private static final WorldData.Mesto NIC =
            new WorldData.Mesto("— vyber město —", "", "UTC", "", 0, 0, 0);

    private static final DateTimeFormatter CAS_KRATKY = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter CAS_DLOUHY =
            DateTimeFormatter.ofPattern("EEEE d. M. HH:mm", Locale.forLanguageTag("cs-CZ"));

    private final Auto auto;
    private final float cena;
    private WorldData.Mesto odkud, kam;
    private boolean ruseni;

    private final JComboBox<WorldData.Mesto> odkudBox, kamBox;
    private final JTextField hledani = new JTextField(14);
    private final JCheckBox nocBox = new JCheckBox("Den a noc", true);
    private final JCheckBox mrizkaBox = new JCheckBox("Souřadnicová síť", true);
    private final JLabel info = new JLabel();
    private final MapPanel mapa = new MapPanel();
    private final JButton prehratBtn;
    private Timer anim;
    private double progress = -1;

    SvetMapaDialog(Frame owner, Auto auto, float cena) {
        super(owner, "Mapa světa", true);
        this.auto = auto;
        this.cena = cena;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        Collator col = Collator.getInstance(Locale.forLanguageTag("cs-CZ"));
        List<WorldData.Mesto> abeceda = new ArrayList<>(WorldData.MESTA);
        abeceda.sort((x, y) -> col.compare(x.nazev, y.nazev));
        WorldData.Mesto[] pole = new WorldData.Mesto[abeceda.size() + 1];
        pole[0] = NIC;
        for (int i = 0; i < abeceda.size(); i++) pole[i + 1] = abeceda.get(i);

        odkudBox = new JComboBox<>(pole);
        kamBox = new JComboBox<>(pole);
        DefaultListCellRenderer renderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                WorldData.Mesto m = (WorldData.Mesto) v;
                String t = m == null ? "" : (m.zeme.isEmpty() ? m.nazev : m.nazev + " (" + m.zeme + ")");
                return super.getListCellRendererComponent(l, t, i, s, f);
            }
        };
        for (JComboBox<WorldData.Mesto> b : java.util.Arrays.asList(odkudBox, kamBox)) {
            b.setRenderer(renderer);
            b.setMaximumRowCount(16);
            b.setBackground(MainFrame.cPole());
            b.setForeground(MainFrame.cText());
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        }
        MainFrame.role(hledani, "field");
        hledani.setFont(f(Font.PLAIN, 13));

        JButton prohoditBtn = mala("Prohodit", new Color(100, 116, 139));
        JButton trasaBtn = mala("Na trasu", MODRA);
        JButton svetBtn = mala("Celý svět", new Color(100, 116, 139));
        prehratBtn = new MainFrame.StyledButton("▶ Přehrát cestu", MODRA);
        prehratBtn.setFont(f(Font.BOLD, 13));

        for (AbstractButton b : new AbstractButton[]{nocBox, mrizkaBox}) {
            b.setOpaque(false);
            b.setFont(f(Font.PLAIN, 13));
            MainFrame.role((JComponent) b, "text");
            b.addActionListener(e -> mapa.repaint());
        }

        // ---- pravý panel ----
        JPanel ovladani = new JPanel();
        ovladani.setOpaque(false);
        ovladani.setLayout(new BoxLayout(ovladani, BoxLayout.Y_AXIS));
        ovladani.add(lev(lbl("Mapa světa", Font.BOLD, 22, "text")));
        ovladani.add(lev(lbl(auto == null ? "Bez vybraného auta" : "Auto: " + auto.getJmenoAuta(),
                Font.PLAIN, 12, "muted")));
        ovladani.add(Box.createVerticalStrut(10));
        ovladani.add(lev(lbl("Odkud", Font.BOLD, 13, "text")));
        ovladani.add(lev(odkudBox));
        ovladani.add(Box.createVerticalStrut(6));
        ovladani.add(lev(lbl("Kam", Font.BOLD, 13, "text")));
        ovladani.add(lev(kamBox));
        ovladani.add(Box.createVerticalStrut(6));

        JPanel tlac = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tlac.setOpaque(false);
        tlac.add(prohoditBtn);
        tlac.add(trasaBtn);
        tlac.add(svetBtn);
        tlac.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        ovladani.add(lev(tlac));
        ovladani.add(Box.createVerticalStrut(8));

        JPanel najit = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        najit.setOpaque(false);
        najit.add(lbl("Najít:", Font.PLAIN, 13, "text"));
        najit.add(hledani);
        najit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        ovladani.add(lev(najit));
        ovladani.add(Box.createVerticalStrut(6));
        ovladani.add(lev(nocBox));
        ovladani.add(lev(mrizkaBox));

        JPanel infoKarta = MainFrame.role(new JPanel(new BorderLayout()), "cardSmall");
        infoKarta.add(info, BorderLayout.NORTH);
        JScrollPane infoScroll = new JScrollPane(infoKarta);
        infoScroll.setBorder(null);
        infoScroll.setOpaque(false);
        infoScroll.getViewport().setOpaque(false);

        JPanel pravy = new JPanel(new BorderLayout(0, 8));
        pravy.setOpaque(false);
        pravy.setPreferredSize(new Dimension(310, 100));
        pravy.add(ovladani, BorderLayout.NORTH);
        pravy.add(infoScroll, BorderLayout.CENTER);
        pravy.add(prehratBtn, BorderLayout.SOUTH);

        // ---- spodní lišta ----
        JButton zavrit = new MainFrame.StyledButton("Zavřít", new Color(71, 85, 105));
        zavrit.addActionListener(e -> dispose());
        JPanel dole = new JPanel(new BorderLayout());
        dole.setOpaque(false);
        dole.add(lbl("Kolečko = přiblížení, tažení = posun, klik na město = výběr (1. klik odkud, 2. klik kam). "
                + "Obrysy jsou schematické.", Font.PLAIN, 12, "muted"), BorderLayout.WEST);
        dole.add(zavrit, BorderLayout.EAST);

        mapa.setPreferredSize(new Dimension(800, 540));
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
        odkudBox.addActionListener(e -> {
            if (ruseni) return;
            WorldData.Mesto m = (WorldData.Mesto) odkudBox.getSelectedItem();
            odkud = m == NIC ? null : m;
            zmena();
        });
        kamBox.addActionListener(e -> {
            if (ruseni) return;
            WorldData.Mesto m = (WorldData.Mesto) kamBox.getSelectedItem();
            kam = m == NIC ? null : m;
            zmena();
        });
        prohoditBtn.addActionListener(e -> {
            WorldData.Mesto t = odkud;
            odkud = kam;
            kam = t;
            synchronizuj();
            zmena();
        });
        trasaBtn.addActionListener(e -> mapa.zoomTrasa());
        svetBtn.addActionListener(e -> mapa.fit());
        hledani.addActionListener(e -> najdiMesto(hledani.getText()));
        prehratBtn.addActionListener(e -> prehraj());

        aktualizujInfo();
        pack();
        setMinimumSize(new Dimension(960, 600));
        setLocationRelativeTo(owner);
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
        b.setPreferredSize(new Dimension(92, 28));
        b.setFont(f(Font.BOLD, 12));
        return b;
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String bez(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static String cislo(double v, int dec) {
        return String.format(Locale.ROOT, "%." + dec + "f", v).replace('.', ',');
    }

    /** Celé číslo s mezerami po tisících. */
    private static String tis(long v) {
        String s = Long.toString(Math.abs(v));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) sb.append('\u00a0');
            sb.append(s.charAt(i));
        }
        return (v < 0 ? "-" : "") + sb;
    }

    private static String delka(double hodiny) {
        int min = (int) Math.round(hodiny * 60);
        return min < 60 ? min + " min" : (min / 60) + " h " + (min % 60) + " min";
    }

    private void synchronizuj() {
        ruseni = true;
        odkudBox.setSelectedItem(odkud == null ? NIC : odkud);
        kamBox.setSelectedItem(kam == null ? NIC : kam);
        ruseni = false;
    }

    private void zmena() {
        if (anim != null) anim.stop();
        progress = -1;
        aktualizujInfo();
        mapa.repaint();
    }

    private void vyberZMapy(WorldData.Mesto m) {
        if (odkud == null || kam != null) {
            odkud = m;
            kam = null;
        } else {
            kam = m;
        }
        synchronizuj();
        zmena();
    }

    private void najdiMesto(String text) {
        String q = bez(text.trim());
        if (q.isEmpty()) return;
        for (WorldData.Mesto m : WorldData.MESTA) {
            if (bez(m.nazev).contains(q)) {
                mapa.cx = m.lon;
                mapa.cy = m.lat;
                mapa.scale = Math.max(mapa.scale, 8);
                mapa.hover = m;
                mapa.repaint();
                return;
            }
        }
        hledani.setText("");
        JOptionPane.showMessageDialog(this, "Takové město v mapě není.", "Najít město",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void prehraj() {
        if (odkud == null || kam == null || odkud == kam) return;
        if (anim != null) anim.stop();
        progress = 0;
        anim = new Timer(20, e -> {
            progress += 0.006;
            if (progress >= 1) {
                progress = 1;
                anim.stop();
            }
            mapa.repaint();
        });
        anim.start();
    }

    // ------------------------------------------------------------------
    // informace o trase
    // ------------------------------------------------------------------

    private void radek(StringBuilder sb, String seda, String k, String v) {
        sb.append("<span style='color:").append(seda).append("'>").append(k).append(":</span> <b>")
                .append(v).append("</b><br>");
    }

    private void aktualizujInfo() {
        String txt = hex(MainFrame.cText()), seda = hex(MainFrame.cSeda());
        String oran = "#d97706", cerv = "#dc2626";
        StringBuilder sb = new StringBuilder("<html><div style='width:255px;color:" + txt + "'>");

        if (odkud == null && kam == null) {
            sb.append("<span style='color:").append(seda).append("'>Vyber dvě města: klikni na ně v mapě, ")
                    .append("nebo je vyber v nabídce. Uvidíš nejkratší cestu po zeměkouli, místní časy, ")
                    .append("délku letu a odhad cesty autem.</span>");
        } else if (odkud == null || kam == null) {
            WorldData.Mesto m = odkud != null ? odkud : kam;
            ZonedDateTime t = ZonedDateTime.now(m.zonaId());
            sb.append("<b>").append(m.nazev).append("</b> (").append(m.zeme).append(")<br><br>");
            radek(sb, seda, "Místní čas", t.format(CAS_DLOUHY));
            radek(sb, seda, "Zeměpisná šířka", cislo(m.lat, 2) + "°");
            radek(sb, seda, "Zeměpisná délka", cislo(m.lon, 2) + "°");
            sb.append("<br><span style='color:").append(seda).append("'>Teď vyber druhé město.</span>");
        } else {
            double km = WorldData.vzdalenostKm(odkud, kam);
            double az = WorldData.azimut(odkud, kam);
            ZonedDateTime za = ZonedDateTime.now(odkud.zonaId()), zb = ZonedDateTime.now(kam.zonaId());
            double rozdil = (zb.getOffset().getTotalSeconds() - za.getOffset().getTotalSeconds()) / 3600.0;

            sb.append("<b>").append(odkud.nazev).append(" → ").append(kam.nazev).append("</b><br>");
            sb.append("<span style='color:").append(seda).append("'>").append(odkud.zeme)
                    .append(odkud.zeme.equals(kam.zeme) ? "" : " → " + kam.zeme).append("</span><br><br>");

            radek(sb, seda, "Vzdušnou čarou", tis(Math.round(km)) + " km");
            radek(sb, seda, "Směr", Math.round(az) + "° (" + WorldData.svetovaStrana(az) + ")");
            radek(sb, seda, "Let (odhad)", delka(km / 800.0 + 0.75));
            radek(sb, seda, "Čas " + odkud.nazev, za.format(CAS_KRATKY));
            radek(sb, seda, "Čas " + kam.nazev, zb.format(CAS_KRATKY));
            String rozdilTxt = rozdil == Math.rint(rozdil)
                    ? String.format(Locale.ROOT, "%+d h", (int) rozdil)
                    : String.format(Locale.ROOT, "%+.1f h", rozdil).replace('.', ',');
            radek(sb, seda, "Časový rozdíl", rozdilTxt);

            WorldData.Cesta c = WorldData.cesta(odkud, kam);
            sb.append("<br>");
            if (!c.autem) {
                sb.append("<span style='color:").append(cerv).append("'>").append(c.poznamka).append("</span>");
            } else if (km < 1) {
                sb.append("<span style='color:").append(seda).append("'>Obě místa jsou stejné město.</span>");
            } else {
                radek(sb, seda, "Po silnici (odhad)", tis(Math.round(c.km)) + " km");
                radek(sb, seda, "Čistý čas jízdy", delka(c.km / 75.0));
                radek(sb, seda, "Na dny (600 km/den)", ((int) Math.ceil(c.km / 600.0)) + " dní");
                if (auto != null) {
                    double litry = auto.getSpotreba() * c.km / 100.0;
                    int tank = auto.getMaxPalivo() <= 0 ? 0
                            : Math.max(0, (int) Math.ceil((litry - auto.getPalivo()) / auto.getMaxPalivo()));
                    radek(sb, seda, "Spotřeba", cislo(litry, 0) + " l");
                    radek(sb, seda, "Cena paliva", tis(Math.round(litry * cena)) + " Kč");
                    radek(sb, seda, "Tankování", tank + "×");
                    radek(sb, seda, "Emise CO₂ (odhad)", tis(Math.round(litry * 2.3)) + " kg");
                }
                if (!c.poznamka.isEmpty()) {
                    sb.append("<br><span style='color:").append(oran).append("'>").append(c.poznamka)
                            .append("</span>");
                }
            }
        }
        sb.append("</div></html>");
        info.setText(sb.toString());
        prehratBtn.setEnabled(odkud != null && kam != null && odkud != kam);
    }

    // ------------------------------------------------------------------
    // mapa
    // ------------------------------------------------------------------

    /** Poloha slunce nad Zemí: {délka podsluneční polohy ve stupních, deklinace v radiánech}. */
    private static double[] slunce() {
        ZonedDateTime u = ZonedDateTime.now(ZoneOffset.UTC);
        int den = u.getDayOfYear();
        double dec = Math.toRadians(-23.44) * Math.cos(Math.toRadians(360.0 / 365.0 * (den + 10)));
        double hodiny = u.getHour() + u.getMinute() / 60.0 + u.getSecond() / 3600.0;
        return new double[]{(12.0 - hodiny) * 15.0, dec};
    }

    private final class MapPanel extends JPanel {
        double cx = 0, cy = 10, scale = 2, fitScale = 2;
        boolean nafitovano;
        WorldData.Mesto hover;
        double kurzorLon, kurzorLat;

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
                    cy += dy / scale;
                    omez();
                    last = e.getPoint();
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (last == null || ujeto > 4) return;
                    WorldData.Mesto m = najdi(e.getPoint());
                    if (m != null) vyberZMapy(m);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    kurzorLon = cx + (e.getX() - getWidth() / 2.0) / scale;
                    kurzorLat = cy - (e.getY() - getHeight() / 2.0) / scale;
                    WorldData.Mesto m = najdi(e.getPoint());
                    hover = m;
                    setCursor(m != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                    repaint();
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    double faktor = Math.pow(1.18, -e.getPreciseWheelRotation());
                    double ns = Math.max(fitScale * 0.9, Math.min(90, scale * faktor));
                    double lon = cx + (e.getX() - getWidth() / 2.0) / scale;
                    double lat = cy - (e.getY() - getHeight() / 2.0) / scale;
                    scale = ns;
                    cx = lon - (e.getX() - getWidth() / 2.0) / scale;
                    cy = lat + (e.getY() - getHeight() / 2.0) / scale;
                    omez();
                    repaint();
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
            addMouseWheelListener(ma);
        }

        private void omez() {
            cx = Math.max(-180, Math.min(180, cx));
            cy = Math.max(-85, Math.min(85, cy));
        }

        double sx(double lon) {
            return getWidth() / 2.0 + (lon - cx) * scale;
        }

        double sy(double lat) {
            return getHeight() / 2.0 - (lat - cy) * scale;
        }

        WorldData.Mesto najdi(Point p) {
            WorldData.Mesto best = null;
            double bd = 12 * 12;
            for (WorldData.Mesto m : WorldData.MESTA) {
                double dx = sx(m.lon) - p.x, dy = sy(m.lat) - p.y;
                double d = dx * dx + dy * dy;
                if (d < bd) {
                    bd = d;
                    best = m;
                }
            }
            return best;
        }

        void fit() {
            scale = fitScale;
            cx = 0;
            cy = 10;
            repaint();
        }

        void zoomTrasa() {
            if (odkud == null || kam == null) return;
            double dLon = Math.abs(odkud.lon - kam.lon), dLat = Math.abs(odkud.lat - kam.lat);
            cx = (odkud.lon + kam.lon) / 2;
            cy = (odkud.lat + kam.lat) / 2;
            double s = Math.min((getWidth() - 200) / (dLon + 8), (getHeight() - 200) / (dLat + 8));
            scale = Math.max(fitScale, Math.min(14, s));
            omez();
            repaint();
        }

        private Path2D.Double cesta(double[][] body) {
            Path2D.Double p = new Path2D.Double();
            for (int i = 0; i < body.length; i++) {
                double x = sx(body[i][0]), y = sy(body[i][1]);
                if (i == 0) p.moveTo(x, y);
                else p.lineTo(x, y);
            }
            p.closePath();
            return p;
        }

        private void popisek(Graphics2D g, String text, double x, double y, boolean tm) {
            g.setFont(f(Font.BOLD, 11));
            g.setColor(tm ? new Color(17, 24, 39, 220) : new Color(255, 255, 255, 220));
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
            int w = getWidth(), h = getHeight();
            fitScale = Math.max(0.5, Math.min(w / 360.0, h / 170.0));
            if (!nafitovano && w > 0) {
                scale = fitScale;
                nafitovano = true;
            }
            boolean tm = MainFrame.jeTmavy();
            Color oceanColor = tm ? new Color(13, 20, 33) : new Color(186, 212, 236);

            g.setColor(tm ? new Color(8, 12, 20) : new Color(150, 166, 184));
            g.fillRect(0, 0, w, h);
            Rectangle2D.Double svet = new Rectangle2D.Double(sx(-180), sy(90), 360 * scale, 180 * scale);
            g.setColor(oceanColor);
            g.fill(svet);

            Shape puvodniClip = g.getClip();
            g.clip(svet);

            // pevniny a vnitrozemská moře
            for (double[][] poly : WorldData.SUSHI) {
                Path2D.Double p = cesta(poly);
                g.setColor(tm ? new Color(44, 55, 72) : new Color(238, 234, 222));
                g.fill(p);
                g.setColor(tm ? new Color(104, 118, 140) : new Color(130, 140, 152));
                g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(p);
            }
            for (double[][] poly : WorldData.MORE) {
                Path2D.Double p = cesta(poly);
                g.setColor(oceanColor);
                g.fill(p);
                g.setColor(tm ? new Color(104, 118, 140) : new Color(130, 140, 152));
                g.draw(p);
            }

            // souřadnicová síť
            if (mrizkaBox.isSelected()) {
                g.setColor(tm ? new Color(255, 255, 255, 28) : new Color(0, 0, 0, 28));
                g.setStroke(new BasicStroke(1f));
                g.setFont(f(Font.PLAIN, 10));
                for (int lon = -150; lon <= 150; lon += 30) {
                    g.draw(new java.awt.geom.Line2D.Double(sx(lon), sy(90), sx(lon), sy(-90)));
                }
                for (int lat = -60; lat <= 60; lat += 30) {
                    g.draw(new java.awt.geom.Line2D.Double(sx(-180), sy(lat), sx(180), sy(lat)));
                }
                g.setColor(tm ? new Color(255, 255, 255, 90) : new Color(0, 0, 0, 90));
                g.drawString("rovník", (float) (sx(-180) + 4), (float) (sy(0) - 3));
            }

            // den a noc
            if (nocBox.isSelected()) {
                double[] s = slunce();
                double dec = s[1];
                if (Math.abs(dec) < 0.02) dec = dec < 0 ? -0.02 : 0.02;
                Path2D.Double noc = new Path2D.Double();
                boolean prvni = true;
                for (int lon = -180; lon <= 180; lon += 2) {
                    double dl = Math.toRadians(lon - s[0]);
                    double lat = Math.toDegrees(Math.atan(-Math.cos(dl) / Math.tan(dec)));
                    if (prvni) {
                        noc.moveTo(sx(lon), sy(lat));
                        prvni = false;
                    } else {
                        noc.lineTo(sx(lon), sy(lat));
                    }
                }
                double pol = dec > 0 ? -90 : 90;
                noc.lineTo(sx(180), sy(pol));
                noc.lineTo(sx(-180), sy(pol));
                noc.closePath();
                g.setColor(new Color(0, 0, 40, tm ? 120 : 85));
                g.fill(noc);
                // podsluneční bod
                double sxp = sx(s[0] > 180 ? s[0] - 360 : (s[0] < -180 ? s[0] + 360 : s[0]));
                double syp = sy(Math.toDegrees(s[1]));
                g.setColor(new Color(255, 214, 10));
                g.fill(new Ellipse2D.Double(sxp - 6, syp - 6, 12, 12));
                g.setColor(new Color(255, 255, 255, 200));
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new Ellipse2D.Double(sxp - 6, syp - 6, 12, 12));
            }

            // názvy světadílů
            if (scale < 5) {
                g.setFont(f(Font.BOLD, scale < 3 ? 11 : 13));
                g.setColor(tm ? new Color(255, 255, 255, 70) : new Color(30, 41, 59, 80));
                for (Object[] sv : WorldData.SVETADILY) {
                    String t = (String) sv[0];
                    double x = sx((Double) sv[1]) - g.getFontMetrics().stringWidth(t) / 2.0;
                    g.drawString(t, (float) x, (float) sy((Double) sv[2]));
                }
            }

            // trasa po velkém kruhu
            if (odkud != null && kam != null && odkud != kam) {
                double[][] body = WorldData.oblouk(odkud, kam, 160);
                Path2D.Double dr = new Path2D.Double();
                for (int i = 0; i < body.length; i++) {
                    double x = sx(body[i][0]), y = sy(body[i][1]);
                    if (i == 0 || Math.abs(body[i][0] - body[i - 1][0]) > 180) dr.moveTo(x, y);
                    else dr.lineTo(x, y);
                }
                g.setColor(new Color(255, 255, 255, tm ? 110 : 220));
                g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(dr);
                g.setColor(MODRA);
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(dr);

                if (progress >= 0) {
                    int idx = (int) Math.min(body.length - 1, Math.round(progress * (body.length - 1)));
                    double x = sx(body[idx][0]), y = sy(body[idx][1]);
                    g.setColor(Color.WHITE);
                    g.fill(new Ellipse2D.Double(x - 9, y - 9, 18, 18));
                    g.setColor(auto != null ? new Color(CarModel.barvaAuta(auto)) : ORANZOVA);
                    g.fill(new Ellipse2D.Double(x - 7, y - 7, 14, 14));
                }
            }

            // města
            for (WorldData.Mesto m : WorldData.MESTA) {
                double x = sx(m.lon), y = sy(m.lat);
                if (x < -30 || y < -30 || x > w + 30 || y > h + 30) continue;
                double r = m.velikost == 2 ? 4.8 : (m.velikost == 1 ? 3.6 : 2.8);
                g.setColor(tm ? new Color(226, 232, 240) : Color.WHITE);
                g.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
                g.setColor(tm ? new Color(15, 23, 42) : new Color(51, 65, 85));
                g.setStroke(new BasicStroke(1.4f));
                g.draw(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
                boolean vybrane = m == odkud || m == kam;
                boolean ukaz = m.velikost == 2 || (m.velikost == 1 && scale >= 3.2) || scale >= 6
                        || vybrane || m == hover;
                if (ukaz) popisek(g, m.nazev, x + r + 3, y + 4, tm);
            }

            // značky vybraných měst
            for (int k = 0; k < 2; k++) {
                WorldData.Mesto m = k == 0 ? odkud : kam;
                if (m == null) continue;
                double x = sx(m.lon), y = sy(m.lat);
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(x - 12, y - 12, 24, 24));
                g.setColor(k == 0 ? ZELENA : CERVENA);
                g.fill(new Ellipse2D.Double(x - 10, y - 10, 20, 20));
                g.setColor(Color.WHITE);
                g.setFont(f(Font.BOLD, 12));
                String t = k == 0 ? "A" : "B";
                g.drawString(t, (float) (x - g.getFontMetrics().stringWidth(t) / 2.0), (float) (y + 4.5));
            }
            g.setClip(puvodniClip);

            // informace pod kurzorem
            if (hover != null) {
                ZonedDateTime t = ZonedDateTime.now(hover.zonaId());
                String txt = hover.nazev + ", " + hover.zeme + "   •   místní čas " + t.format(CAS_KRATKY);
                g.setFont(f(Font.BOLD, 12));
                int tw = g.getFontMetrics().stringWidth(txt);
                g.setColor(tm ? new Color(31, 41, 55, 235) : new Color(255, 255, 255, 235));
                g.fillRoundRect(10, h - 34, tw + 20, 24, 12, 12);
                g.setColor(tm ? new Color(226, 232, 240) : new Color(30, 41, 59));
                g.drawString(txt, 20, h - 17);
            }
            g.setFont(f(Font.PLAIN, 11));
            g.setColor(tm ? new Color(156, 163, 175) : new Color(51, 65, 85));
            String souradnice = cislo(kurzorLat, 1) + "°, " + cislo(kurzorLon, 1) + "°";
            g.drawString(souradnice, w - g.getFontMetrics().stringWidth(souradnice) - 10, h - 10);
            g.dispose();
        }
    }
}