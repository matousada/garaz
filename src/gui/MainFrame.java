package gui;

import data.Auto;
import data.Garaz;
import data.Uloziste;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Hlavní okno: horní lišta (hledání, upozornění, režim), levé menu s pohledy, obsah a stavový řádek.
 * Akce nad autem (tankování, výlet, servis, připnutí...) jsou tady, aby je mohly volat všechny pohledy.
 */
public class MainFrame extends JFrame implements Kontext {

    // REGEX (do dokumentace):
    // CISLO       - kontrola hotového čísla (litry, spotřeba, cena...), použito v parseCislo()
    // CELE_CISLO  - kontrola kilometrů u výletu a dní u simulace, použito ve vylet() a simulace()
    // DATUM       - datum ve tvaru d.M.rrrr (servis, STK), použito v parseDatum()
    private static final Pattern CISLO = Pattern.compile("\\d+([.,]\\d+)?");
    static final Pattern CELE_CISLO = Pattern.compile("\\d{1,5}");
    private static final Pattern DATUM = Pattern.compile("\\d{1,2}\\.\\s?\\d{1,2}\\.\\s?\\d{4}");

    static final DateTimeFormatter DATUM_CZ = DateTimeFormatter.ofPattern("d. M. yyyy");

    // ------------------------------------------------------------------
    // TÉMA (kvůli starším dialogům: mapa, svět, 3D model používají tyhle metody)
    // ------------------------------------------------------------------

    static boolean jeTmavy() { return Ui.tmavy; }
    static Color cPozadi() { return Ui.bg(); }
    static Color cKarta() { return Ui.surface(); }
    static Color cObrys() { return Ui.line(); }
    static Color cText() { return Ui.text(); }
    static Color cSeda() { return Ui.muted(); }
    static Color cPole() { return Ui.surface(); }
    static Color cStopa() { return Ui.sunken(); }

    private static Border okraj(Color barva, int v, int h) {
        return new CompoundBorder(new LineBorder(barva, 1, true), new EmptyBorder(v, h, v, h));
    }

    /** Nastaví barvy standardních prvků Swingu (JOptionPane, JFileChooser...) podle aktuálního tématu. */
    static void nastavUIManager() {
        UIManager.put("Panel.background", Ui.bg());
        UIManager.put("OptionPane.background", Ui.bg());
        UIManager.put("OptionPane.messageForeground", Ui.text());
        UIManager.put("Label.foreground", Ui.text());
        UIManager.put("TextField.background", Ui.surface());
        UIManager.put("TextField.foreground", Ui.text());
        UIManager.put("TextField.caretForeground", Ui.text());
        UIManager.put("Button.background", Ui.surface2());
        UIManager.put("Button.foreground", Ui.text());
        UIManager.put("ComboBox.background", Ui.surface());
        UIManager.put("ComboBox.foreground", Ui.text());
        UIManager.put("ComboBox.selectionBackground", Ui.accent());
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("Slider.background", Ui.bg());
        UIManager.put("TextArea.background", Ui.surface());
        UIManager.put("TextArea.foreground", Ui.text());
        UIManager.put("List.background", Ui.surface());
        UIManager.put("List.foreground", Ui.text());
        UIManager.put("ToolTip.background", Ui.text());
        UIManager.put("ToolTip.foreground", Ui.bg());
        UIManager.put("ToolTip.border", new EmptyBorder(6, 9, 6, 9));
        UIManager.put("ToolTip.font", Ui.font(Font.PLAIN, 12));
        UIManager.put("PopupMenu.background", Ui.surface());
        UIManager.put("PopupMenu.border", new LineBorder(Ui.lineStrong()));
    }

    /**
     * Přebarví strom komponent podle "role" (client property "role"):
     * bg, card, cardSmall, cardGold, text, muted, field. Používají to starší dialogy.
     */
    static void tema(Component c) {
        if (c instanceof JComponent) {
            JComponent jc = (JComponent) c;
            Object role = jc.getClientProperty("role");
            if ("bg".equals(role)) {
                jc.setBackground(cPozadi());
            } else if ("card".equals(role)) {
                jc.setBackground(cKarta());
                jc.setBorder(okraj(cObrys(), 18, 20));
            } else if ("cardSmall".equals(role)) {
                jc.setBackground(cKarta());
                jc.setBorder(okraj(cObrys(), 10, 12));
            } else if ("cardGold".equals(role)) {
                jc.setBackground(cKarta());
                jc.setBorder(okraj(Ui.warn(), 10, 12));
            } else if ("text".equals(role)) {
                jc.setForeground(cText());
            } else if ("muted".equals(role)) {
                jc.setForeground(cSeda());
            } else if ("field".equals(role)) {
                jc.setBackground(cPole());
                jc.setForeground(cText());
                if (jc instanceof JTextField) {
                    ((JTextField) jc).setCaretColor(cText());
                }
            }
        }
        if (c instanceof Container) {
            for (Component dite : ((Container) c).getComponents()) {
                tema(dite);
            }
        }
        c.repaint();
    }

    static <T extends JComponent> T role(T c, String role) {
        c.putClientProperty("role", role);
        return c;
    }

    // ------------------------------------------------------------------
    // pomocné metody (používá je i AutoDialog)
    // ------------------------------------------------------------------

    /** Převede text na číslo (tečka i čárka). Neplatný text -> null. */
    static Float parseCislo(String text) {
        if (text == null) return null;
        String t = text.trim().replace(" ", "");
        if (!CISLO.matcher(t).matches()) return null;
        return Float.parseFloat(t.replace(',', '.'));
    }

    /** Převede datum d.M.rrrr na LocalDate. Neplatné datum -> null. */
    static LocalDate parseDatum(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (!DATUM.matcher(t).matches()) return null;
        String[] p = t.replace(" ", "").split("\\.");
        try {
            return LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    static String datum(LocalDate d) {
        return d == null ? "—" : d.format(DATUM_CZ);
    }

    /** Hezké číslo pro zobrazení: 7.50 -> "7,5", 40.0 -> "40". */
    static String format(float hodnota) {
        String s = String.format(Locale.ROOT, "%.2f", hodnota);
        s = s.replaceAll("\\.?0+$", "");
        return s.replace('.', ',');
    }

    /** Číslo na jedno desetinné místo s čárkou: 12.34 -> "12,3". */
    static String cislo1(float x) {
        return String.format(Locale.forLanguageTag("cs-CZ"), "%.1f", x);
    }

    /** Zaoblené barevné tlačítko (používají ho starší dialogy mapy a 3D modelu). */
    static class StyledButton extends JButton {
        private final Color barva;
        private boolean hover = false;

        StyledButton(String text, Color barva) {
            super(text);
            this.barva = barva;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(Ui.font(Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(120, 38));
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

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Ui.aa(g2);
            Color pozadi;
            if (!isEnabled()) pozadi = Ui.sunken();
            else if (getModel().isPressed()) pozadi = barva.darker();
            else if (hover) pozadi = barva.brighter();
            else pozadi = barva;
            g2.setColor(pozadi);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(isEnabled() ? getForeground() : Ui.muted());
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // stav aplikace
    // ------------------------------------------------------------------

    private final Garaz garaz = new Garaz(100);
    private Auto vybrane;
    private Path soubor = Uloziste.vychoziSoubor();
    private String posledniUlozeni = "";

    private final Map<String, Pohled> pohledy = new LinkedHashMap<>();
    private final Map<String, NavPolozka> navigace = new LinkedHashMap<>();
    private String aktivni = "prehled";

    private JPanel karty;
    private JLabel statusLabel, ulozenoLabel;
    private JPanel navPanel;
    private Ui.Tlacitko zvonek;

    public MainFrame() {
        super("Garáž – správa vozového parku");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ulozit();
                dispose();
                System.exit(0);
            }
        });
        nactiNeboUkazka();

        pohledy.put("prehled", new PrehledView(this));
        pohledy.put("vozidla", new VozidlaView(this));
        pohledy.put("priorita", new PrioritaView(this));
        pohledy.put("servis", new ServisView(this));
        pohledy.put("naklady", new NakladyView(this));
        pohledy.put("historie", new HistorieView(this));
        pohledy.put("uspechy", new UspechyView(this));
        pohledy.put("metodika", new MetodikaView(this));

        postavOkno();
        klavesoveZkratky();
        setMinimumSize(new Dimension(1120, 720));
        setSize(1400, 880);
        setLocationRelativeTo(null);
        prepni("prehled");
    }

    private void nactiNeboUkazka() {
        try {
            if (Files.exists(soubor)) {
                Uloziste.nacti(garaz, soubor);
                posledniUlozeni = "načteno " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                if (!garaz.jePrazdna()) vybrane = garaz.getZebricek().get(0);
                return;
            }
        } catch (IOException ex) {
            garaz.vymazVse();
        }
        Uloziste.ukazkovaData(garaz);
        if (!garaz.jePrazdna()) vybrane = garaz.getZebricek().get(0);
    }

    Garaz garaz() {
        return garaz;
    }

    Path soubor() {
        return soubor;
    }

    // ------------------------------------------------------------------
    // Kontext
    // ------------------------------------------------------------------

    @Override
    public Frame okno() {
        return this;
    }

    @Override
    public List<Auto> auta() {
        return garaz.getAuta();
    }

    @Override
    public Auto aktualni() {
        if (vybrane != null && garaz.najdiAuto(vybrane.getCarId()) == null) vybrane = null;
        if (vybrane == null && !garaz.jePrazdna()) vybrane = garaz.getZebricek().get(0);
        return vybrane;
    }

    @Override
    public float cenaPaliva() {
        return garaz.getCenaPaliva();
    }

    @Override
    public void ulozit() {
        try {
            Uloziste.uloz(garaz, soubor);
            posledniUlozeni = "uloženo " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        } catch (IOException ex) {
            posledniUlozeni = "uložení selhalo";
            status("Nepodařilo se uložit: " + ex.getMessage());
        }
        if (ulozenoLabel != null) ulozenoLabel.setText(posledniUlozeni + "  ·  " + soubor.getFileName());
    }

    @Override
    public void obnovit() {
        Pohled p = pohledy.get(aktivni);
        if (p != null) p.obnov();
        obnovNavigaci();
        if (zvonek != null) zvonek.repaint();
    }

    @Override
    public void status(String text) {
        if (statusLabel != null) statusLabel.setText(text);
    }

    /** Vybere auto (ostatní pohledy ho pak ukazují jako aktuální). */
    void vyber(Auto a) {
        vybrane = a;
    }

    /** Vybere auto a přepne na jeho detail. */
    void otevriAuto(Auto a) {
        vybrane = a;
        prepni("vozidla");
    }

    /** Po každé změně dat: kontrola úspěchů, uložení, překreslení a krátké oznámení. */
    void zmena(String zprava) {
        List<Garaz.Uspech> nove = garaz.zkontrolujUspechy();
        ulozit();
        obnovit();
        if (zprava != null) {
            status(zprava);
            Ui.toast(this, zprava, Ui.ok());
        }
        for (Garaz.Uspech u : nove) {
            garaz.zapis("Odemčen úspěch " + u.getNazev());
            Ui.toast(this, "Nový úspěch: " + u.getNazev() + " – " + u.getPopis(), Ui.warn());
        }
        if (!nove.isEmpty()) ulozit();
    }

    // ------------------------------------------------------------------
    // stavba okna
    // ------------------------------------------------------------------

    private void postavOkno() {
        nastavUIManager();
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Ui.bg());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        karty = Ui.pruhledny(new CardLayout());
        for (Map.Entry<String, Pohled> e : pohledy.entrySet()) {
            karty.add(e.getValue(), e.getKey());
        }

        root.add(horniLista(), BorderLayout.NORTH);
        root.add(levaNavigace(), BorderLayout.WEST);
        root.add(karty, BorderLayout.CENTER);
        root.add(stavovyRadek(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JComponent horniLista() {
        JPanel lista = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Ui.surface());
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Ui.line());
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        lista.setPreferredSize(new Dimension(10, 60));
        lista.setBorder(new EmptyBorder(0, 18, 0, 16));

        // logo
        JComponent znak = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Ui.aa(g2);
                g2.setColor(Ui.accent());
                g2.fill(new RoundRectangle2D.Float(0, 0, 32, 32, 10, 10));
                Ui.Ikona.kresli(g2, "car", 6, 6, 20, Ui.onAccent());
                g2.dispose();
            }
        };
        znak.setPreferredSize(new Dimension(32, 32));
        JLabel nazev = Ui.titulek("Garáž", 21);
        JLabel podnazev = Ui.muted("správa vozového parku", 12);
        JPanel texty = Ui.pruhledny(new GridLayout(2, 1, 0, -2));
        texty.add(nazev);
        texty.add(podnazev);
        JPanel logo = Ui.pruhledny(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logo.add(znak);
        logo.add(texty);
        JPanel logoObal = Ui.pruhledny(new GridBagLayout());
        logoObal.add(logo);
        logoObal.setPreferredSize(new Dimension(212, 60));

        // hledání (otevře příkazovou paletu)
        JTextField hledej = Ui.hledani("Hledat auto, SPZ nebo akci…   Ctrl+K");
        hledej.setEditable(false);
        hledej.setFocusable(false);
        hledej.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        hledej.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                SwingUtilities.invokeLater(() -> paleta());
            }
        });
        hledej.setPreferredSize(new Dimension(420, 38));
        JPanel stred = Ui.pruhledny(new GridBagLayout());
        stred.add(hledej);

        // vpravo
        zvonek = new Ui.Tlacitko("", Ui.Styl.DUCH, "bell") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int n = garaz.getUpozorneni().size();
                if (n == 0) return;
                Graphics2D g2 = (Graphics2D) g.create();
                Ui.aa(g2);
                String t = n > 9 ? "9+" : String.valueOf(n);
                g2.setFont(Ui.font(Font.BOLD, 10));
                FontMetrics fm = g2.getFontMetrics();
                int w = Math.max(16, fm.stringWidth(t) + 8);
                g2.setColor(Ui.danger());
                g2.fill(new RoundRectangle2D.Float(getWidth() - w - 1, 2, w, 16, 16, 16));
                g2.setColor(Color.WHITE);
                g2.drawString(t, getWidth() - w - 1 + (w - fm.stringWidth(t)) / 2, 2 + (16 - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        zvonek.setToolTipText("Upozornění");
        zvonek.addActionListener(e -> upozorneniPopup(zvonek));
        Ui.Tlacitko tema = new Ui.Tlacitko("", Ui.Styl.DUCH, Ui.tmavy ? "sun" : "moon");
        tema.setToolTipText(Ui.tmavy ? "Světlý režim" : "Tmavý režim");
        tema.addActionListener(e -> prepniTema());
        Ui.Tlacitko nastaveni = new Ui.Tlacitko("", Ui.Styl.DUCH, "settings");
        nastaveni.setToolTipText("Nastavení a data");
        nastaveni.addActionListener(e -> nastaveni());
        Ui.Tlacitko pridat = Ui.primarni("Přidat auto", "plus", this::pridatAuto);
        pridat.setToolTipText("Ctrl+N");

        JPanel vpravo = Ui.pruhledny(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        vpravo.add(zvonek);
        vpravo.add(tema);
        vpravo.add(nastaveni);
        vpravo.add(Ui.mezera(6, 1));
        vpravo.add(pridat);
        JPanel vpravoObal = Ui.pruhledny(new GridBagLayout());
        vpravoObal.add(vpravo);

        lista.add(logoObal, BorderLayout.WEST);
        lista.add(stred, BorderLayout.CENTER);
        lista.add(vpravoObal, BorderLayout.EAST);
        return lista;
    }

    /** Položka levého menu: ikona, název a volitelně počet vpravo. */
    private final class NavPolozka extends JComponent {
        private final String id, text, ikona;
        private boolean hover;
        private String pocet = "";
        private Color barvaPoctu;

        NavPolozka(String id, String text, String ikona, Runnable akce) {
            this.id = id;
            this.text = text;
            this.ikona = ikona;
            setPreferredSize(new Dimension(200, 38));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            setAlignmentX(LEFT_ALIGNMENT);
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

                @Override
                public void mouseClicked(MouseEvent e) {
                    akce.run();
                }
            });
        }

        void pocet(String p, Color barva) {
            this.pocet = p;
            this.barvaPoctu = barva;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Ui.aa(g2);
            boolean vybrana = id.equals(aktivni);
            int w = getWidth(), h = getHeight();
            if (vybrana || hover) {
                g2.setColor(vybrana ? Ui.accentSoft() : Ui.sunken());
                g2.fill(new RoundRectangle2D.Float(0, 2, w, h - 4, 10, 10));
            }
            if (vybrana) {
                g2.setColor(Ui.accent());
                g2.fill(new RoundRectangle2D.Float(0, 10, 3, h - 20, 3, 3));
            }
            Color barva = vybrana ? Ui.accent() : Ui.text2();
            Ui.Ikona.kresli(g2, ikona, 14, (h - 18) / 2, 18, vybrana ? Ui.accent() : Ui.muted());
            g2.setFont(Ui.font(vybrana ? Font.BOLD : Font.PLAIN, 14));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(barva);
            g2.drawString(text, 44, (h - fm.getHeight()) / 2 + fm.getAscent());
            if (!pocet.isEmpty()) {
                g2.setFont(Ui.font(Font.BOLD, 11));
                fm = g2.getFontMetrics();
                int pw = Math.max(20, fm.stringWidth(pocet) + 12);
                Color b = barvaPoctu == null ? Ui.muted() : barvaPoctu;
                g2.setColor(Ui.soft(b, 0.14f));
                g2.fill(new RoundRectangle2D.Float(w - pw - 8, (h - 20) / 2f, pw, 20, 20, 20));
                g2.setColor(b);
                g2.drawString(pocet, w - pw - 8 + (pw - fm.stringWidth(pocet)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }

    private JComponent levaNavigace() {
        navPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Ui.surface());
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Ui.line());
                g.fillRect(getWidth() - 1, 0, 1, getHeight());
            }
        };
        navPanel.setPreferredSize(new Dimension(236, 10));
        navPanel.setBorder(new EmptyBorder(16, 12, 14, 14));

        JPanel seznam = Ui.pruhledny(null);
        seznam.setLayout(new BoxLayout(seznam, BoxLayout.Y_AXIS));

        skupina(seznam, "Přehled");
        polozka(seznam, "prehled", "Přehled", "dashboard");
        skupina(seznam, "Vozový park");
        polozka(seznam, "vozidla", "Vozidla", "car");
        polozka(seznam, "priorita", "Priorita údržby", "priority");
        polozka(seznam, "servis", "Servis a rozpočet", "wrench");
        polozka(seznam, "naklady", "Náklady", "coins");
        skupina(seznam, "Cesty");
        NavPolozka trasy = new NavPolozka("_trasy", "Plánovač tras", "route", () -> mapaCR(aktualni()));
        NavPolozka svet = new NavPolozka("_svet", "Mapa světa", "globe", () -> mapaSvet(aktualni()));
        seznam.add(trasy);
        seznam.add(svet);
        skupina(seznam, "Záznamy");
        polozka(seznam, "historie", "Historie změn", "history");
        polozka(seznam, "uspechy", "Úspěchy", "trophy");
        polozka(seznam, "metodika", "Jak to počítáme", "book");

        navPanel.add(seznam, BorderLayout.NORTH);
        navPanel.add(kapacita(), BorderLayout.SOUTH);
        return navPanel;
    }

    private void skupina(JPanel p, String nazev) {
        if (p.getComponentCount() > 0) p.add(Box.createVerticalStrut(14));
        JLabel l = Ui.eyebrow(nazev);
        l.setBorder(new EmptyBorder(0, 14, 6, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l);
    }

    private void polozka(JPanel p, String id, String text, String ikona) {
        NavPolozka n = new NavPolozka(id, text, ikona, () -> prepni(id));
        navigace.put(id, n);
        p.add(n);
    }

    /** Spodní karta v menu: obsazenost garáže a kam se ukládá. */
    private JComponent kapacita() {
        JComponent k = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Ui.aa(g2);
                int w = getWidth(), h = getHeight();
                g2.setColor(Ui.surface2());
                g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 12, 12));
                g2.setColor(Ui.line());
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2, h - 2, 12, 12));
                g2.setFont(Ui.font(Font.BOLD, 11));
                g2.setColor(Ui.muted());
                g2.drawString("OBSAZENOST GARÁŽE", 14, 22);
                g2.setFont(Ui.serif(Font.BOLD, 20));
                g2.setColor(Ui.text());
                String t = garaz.getPocetAut() + " / " + garaz.getPocetMist();
                g2.drawString(t, 14, 48);
                g2.setFont(Ui.font(Font.PLAIN, 12));
                g2.setColor(Ui.muted());
                g2.drawString("míst", 18 + g2.getFontMetrics(Ui.serif(Font.BOLD, 20)).stringWidth(t), 48);
                float v = garaz.getPocetAut() / (float) garaz.getPocetMist();
                g2.setColor(Ui.sunken());
                g2.fill(new RoundRectangle2D.Float(14, 58, w - 28, 6, 6, 6));
                g2.setColor(Ui.accent());
                g2.fill(new RoundRectangle2D.Float(14, 58, Math.max(6, (w - 28) * v), 6, 6, 6));
                g2.dispose();
            }
        };
        k.setPreferredSize(new Dimension(200, 78));
        return k;
    }

    private void obnovNavigaci() {
        int krit = 0;
        for (Auto a : garaz.getAuta()) {
            if (garaz.getPriorita(a) == Auto.Priorita.KRITICKA) krit++;
        }
        set("vozidla", String.valueOf(garaz.getPocetAut()), null);
        set("priorita", krit > 0 ? String.valueOf(krit) : "", Ui.danger());
        int uspechy = 0;
        for (Garaz.Uspech u : Garaz.Uspech.values()) if (garaz.jeOdemcen(u)) uspechy++;
        set("uspechy", uspechy + "/" + Garaz.Uspech.values().length, null);
        if (navPanel != null) navPanel.repaint();
    }

    private void set(String id, String text, Color barva) {
        NavPolozka n = navigace.get(id);
        if (n != null) n.pocet(text, barva);
    }

    private JComponent stavovyRadek() {
        JPanel r = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Ui.surface());
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Ui.line());
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        r.setPreferredSize(new Dimension(10, 28));
        r.setBorder(new EmptyBorder(0, 16, 0, 16));
        statusLabel = Ui.muted("Připraveno", 12);
        ulozenoLabel = Ui.muted((posledniUlozeni.isEmpty() ? "ukázková data" : posledniUlozeni)
                + "  ·  " + soubor.getFileName(), 12);
        JLabel klavesy = Ui.muted("Ctrl+K hledat   ·   Ctrl+N nové auto   ·   Ctrl+1–8 pohledy   ·   Ctrl+D režim", 12);
        r.add(statusLabel, BorderLayout.WEST);
        r.add(klavesy, BorderLayout.CENTER);
        klavesy.setHorizontalAlignment(SwingConstants.CENTER);
        r.add(ulozenoLabel, BorderLayout.EAST);
        return r;
    }

    private void klavesoveZkratky() {
        JRootPane rp = getRootPane();
        int ctrl = InputEvent.CTRL_DOWN_MASK;
        klavesa(rp, KeyStroke.getKeyStroke(KeyEvent.VK_K, ctrl), "paleta", this::paleta);
        klavesa(rp, KeyStroke.getKeyStroke(KeyEvent.VK_N, ctrl), "nove", this::pridatAuto);
        klavesa(rp, KeyStroke.getKeyStroke(KeyEvent.VK_S, ctrl), "ulozit", () -> {
            ulozit();
            Ui.toast(this, "Uloženo do " + soubor, Ui.ok());
        });
        klavesa(rp, KeyStroke.getKeyStroke(KeyEvent.VK_D, ctrl), "tema", this::prepniTema);
        String[] ids = pohledy.keySet().toArray(new String[0]);
        for (int i = 0; i < ids.length && i < 9; i++) {
            String id = ids[i];
            klavesa(rp, KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, ctrl), "pohled" + i, () -> prepni(id));
        }
    }

    private static void klavesa(JRootPane rp, KeyStroke ks, String nazev, Runnable akce) {
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, nazev);
        rp.getActionMap().put(nazev, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                akce.run();
            }
        });
    }

    void prepni(String id) {
        if (!pohledy.containsKey(id)) return;
        aktivni = id;
        pohledy.get(id).obnov();
        ((CardLayout) karty.getLayout()).show(karty, id);
        obnovNavigaci();
        status(pohledy.get(id).nazev());
    }

    String aktivniPohled() {
        return aktivni;
    }

    private void prepniTema() {
        Ui.tmavy = !Ui.tmavy;
        nastavUIManager();
        Container stary = getContentPane();
        String puvodni = aktivni;
        postavOkno();
        stary.removeAll();
        prepni(puvodni);
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------------
    // upozornění a příkazová paleta
    // ------------------------------------------------------------------

    private void upozorneniPopup(JComponent kotva) {
        List<Garaz.Upozorneni> seznam = garaz.getUpozorneni();
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(new LineBorder(Ui.lineStrong()));
        JPanel obsah = new JPanel();
        obsah.setBackground(Ui.surface());
        obsah.setLayout(new BoxLayout(obsah, BoxLayout.Y_AXIS));
        obsah.setBorder(new EmptyBorder(14, 14, 10, 14));
        JPanel hlava = Ui.pruhledny(new BorderLayout());
        hlava.add(Ui.silny("Upozornění", 15), BorderLayout.WEST);
        hlava.add(Ui.muted(seznam.size() + " k řešení", 12), BorderLayout.EAST);
        hlava.setAlignmentX(Component.LEFT_ALIGNMENT);
        hlava.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        obsah.add(hlava);
        obsah.add(Box.createVerticalStrut(8));
        if (seznam.isEmpty()) {
            JLabel l = Ui.muted("Všechno v pořádku. Žádné auto teď nic nepotřebuje.");
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            obsah.add(l);
        }
        int n = 0;
        for (Garaz.Upozorneni u : seznam) {
            if (n++ >= 9) break;
            JPanel radek = radekUpozorneni(u, () -> {
                menu.setVisible(false);
                otevriAuto(u.auto);
            });
            obsah.add(radek);
        }
        if (seznam.size() > 9) {
            JLabel dalsi = Ui.muted("a dalších " + (seznam.size() - 9) + "…", 12);
            dalsi.setAlignmentX(Component.LEFT_ALIGNMENT);
            obsah.add(dalsi);
        }
        obsah.setPreferredSize(new Dimension(380, obsah.getPreferredSize().height));
        menu.add(obsah);
        menu.show(kotva, kotva.getWidth() - 380, kotva.getHeight() + 6);
    }

    /** Jeden řádek upozornění: tečka podle závažnosti, auto, text. Používá ho i přehled. */
    static JPanel radekUpozorneni(Garaz.Upozorneni u, Runnable klik) {
        JPanel r = new JPanel(new BorderLayout(10, 0)) {
            boolean hover;

            {
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

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        klik.run();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                if (!hover) return;
                Graphics2D g2 = (Graphics2D) g.create();
                Ui.aa(g2);
                g2.setColor(Ui.sunken());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
        };
        r.setOpaque(false);
        r.setBorder(new EmptyBorder(8, 8, 8, 8));
        r.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Color barva = Ui.zavaznost(u.zavaznost);
        JComponent tecka = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Ui.aa(g2);
                g2.setColor(Ui.soft(barva, 0.15f));
                g2.fill(new Ellipse2D.Float(0, 4, 28, 28));
                Ui.Ikona.kresli(g2, u.zavaznost == Garaz.Zavaznost.INFO ? "fuel" : "alert", 6, 10, 16, barva);
                g2.dispose();
            }
        };
        tecka.setPreferredSize(new Dimension(28, 36));
        JPanel texty = Ui.pruhledny(new GridLayout(2, 1));
        texty.add(Ui.label(u.auto.getJmenoAuta() + "  ·  " + u.auto.getSpz(), Font.BOLD, 13, Ui.text()));
        texty.add(Ui.label(u.text, Font.PLAIN, 12.5f, barva));
        r.add(tecka, BorderLayout.WEST);
        r.add(texty, BorderLayout.CENTER);
        r.add(new JLabel(new Ui.Ikona("chevron-right", 14, Ui.muted())), BorderLayout.EAST);
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        return r;
    }

    /** Příkazová paleta (Ctrl+K): hledá auta podle jména, značky a SPZ a nabízí akce. */
    void paleta() {
        JDialog d = new JDialog(this, "Hledat", Dialog.ModalityType.APPLICATION_MODAL);
        d.setUndecorated(true);
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(Ui.surface());
        root.setBorder(new CompoundBorder(new LineBorder(Ui.lineStrong()), new EmptyBorder(12, 12, 12, 12)));

        JTextField pole = Ui.hledani("Napiš jméno auta, SPZ nebo akci…");
        pole.setPreferredSize(new Dimension(560, 42));
        pole.setFont(Ui.font(Font.PLAIN, 15));

        class Polozka {
            final String text, pod, ikona;
            final Runnable akce;

            Polozka(String text, String pod, String ikona, Runnable akce) {
                this.text = text;
                this.pod = pod;
                this.ikona = ikona;
                this.akce = akce;
            }
        }
        List<Polozka> vse = new ArrayList<>();
        for (Auto a : garaz.getZebricek()) {
            vse.add(new Polozka(a.getJmenoAuta(), a.getZnacka() + " · " + a.getSpz() + " · " + garaz.getPriorita(a),
                    "car", () -> otevriAuto(a)));
        }
        vse.add(new Polozka("Přidat auto", "Ctrl+N", "plus", this::pridatAuto));
        for (Map.Entry<String, Pohled> e : pohledy.entrySet()) {
            vse.add(new Polozka("Přejít: " + e.getValue().nazev(), "pohled", e.getValue().ikona(), () -> prepni(e.getKey())));
        }
        vse.add(new Polozka("Plánovač tras po ČR", "mapa", "route", () -> mapaCR(aktualni())));
        vse.add(new Polozka("Mapa světa", "mapa", "globe", () -> mapaSvet(aktualni())));
        vse.add(new Polozka("Export do CSV", "soubor", "download", this::exportovatCsv));
        vse.add(new Polozka("Přepnout světlý / tmavý režim", "Ctrl+D", Ui.tmavy ? "sun" : "moon", this::prepniTema));
        vse.add(new Polozka("Nastavení a data", "cena paliva, rozpočet, záloha", "settings", this::nastaveni));

        DefaultListModel<Polozka> model = new DefaultListModel<>();
        JList<Polozka> seznam = new JList<>(model);
        seznam.setBackground(Ui.surface());
        seznam.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        seznam.setCellRenderer((list, p, i, vybrana, fokus) -> {
            JPanel r = new JPanel(new BorderLayout(12, 0));
            r.setBackground(vybrana ? Ui.accentSoft() : Ui.surface());
            r.setBorder(new EmptyBorder(8, 10, 8, 10));
            r.add(new JLabel(new Ui.Ikona(p.ikona, 18, vybrana ? Ui.accent() : Ui.muted())), BorderLayout.WEST);
            r.add(Ui.label(p.text, Font.BOLD, 14, vybrana ? Ui.accent() : Ui.text()), BorderLayout.CENTER);
            r.add(Ui.muted(p.pod, 12), BorderLayout.EAST);
            return r;
        });
        Runnable filtr = () -> {
            String q = pole.getText().trim().toLowerCase(Locale.ROOT);
            model.clear();
            for (Polozka p : vse) {
                String h = (p.text + " " + p.pod).toLowerCase(Locale.ROOT);
                if (q.isEmpty() || bezDiakritiky(h).contains(bezDiakritiky(q))) model.addElement(p);
            }
            if (!model.isEmpty()) seznam.setSelectedIndex(0);
        };
        pole.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filtr.run();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filtr.run();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filtr.run();
            }
        });
        Runnable spust = () -> {
            Polozka p = seznam.getSelectedValue();
            d.dispose();
            if (p != null) SwingUtilities.invokeLater(p.akce);
        };
        pole.addActionListener(e -> spust.run());
        pole.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int i = seznam.getSelectedIndex();
                if (e.getKeyCode() == KeyEvent.VK_DOWN && i < model.size() - 1) {
                    seznam.setSelectedIndex(i + 1);
                    seznam.ensureIndexIsVisible(i + 1);
                } else if (e.getKeyCode() == KeyEvent.VK_UP && i > 0) {
                    seznam.setSelectedIndex(i - 1);
                    seznam.ensureIndexIsVisible(i - 1);
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    d.dispose();
                }
            }
        });
        seznam.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                spust.run();
            }
        });
        d.addWindowFocusListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowLostFocus(java.awt.event.WindowEvent e) {
                d.dispose();
            }
        });
        filtr.run();
        JScrollPane sp = Ui.scroll(seznam);
        sp.setPreferredSize(new Dimension(560, 340));
        root.add(pole, BorderLayout.NORTH);
        root.add(sp, BorderLayout.CENTER);
        root.add(Ui.muted("↑ ↓ vybrat   ·   Enter otevřít   ·   Esc zavřít", 12), BorderLayout.SOUTH);
        d.setContentPane(root);
        d.pack();
        Point p = getLocationOnScreen();
        d.setLocation(p.x + (getWidth() - d.getWidth()) / 2, p.y + 90);
        d.setVisible(true);
    }

    static String bezDiakritiky(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    // ------------------------------------------------------------------
    // akce nad autem
    // ------------------------------------------------------------------

    void pridatAuto() {
        if (garaz.jePlna()) {
            Okno.info(this, "Garáž je plná", "Všech " + garaz.getPocetMist() + " míst je obsazeno. Nejdřív nějaké auto odeber.");
            return;
        }
        AutoDialog dialog = new AutoDialog(this, "Nové auto");
        dialog.setVisible(true);
        Auto nove = dialog.getData();
        if (nove != null && garaz.addCar(nove)) {
            vybrane = nove;
            garaz.zapis("Přidáno auto " + nove);
            if (!aktivni.equals("vozidla")) prepni("vozidla");
            zmena("Přidáno auto " + nove.getJmenoAuta());
        }
    }

    void upravitAuto(Auto a) {
        if (a == null) return;
        AutoDialog dialog = new AutoDialog(this, "Upravit auto");
        dialog.setData(a);
        dialog.setVisible(true);
        if (dialog.getData() != null) {
            garaz.zapis("Upraveno auto " + a);
            zmena("Uloženy změny auta " + a.getJmenoAuta());
        }
    }

    void odebratAuto(Auto a) {
        if (a == null) return;
        if (Okno.potvrd(this, "Odebrat auto", "Opravdu odebrat " + a + "? Kniha tankování a servisů zůstane"
                + " v historii, ale auto zmizí ze žebříčku i z plánu.", "Odebrat", true)) {
            garaz.removeCar(a);
            garaz.zapis("Odebráno auto " + a);
            vybrane = null;
            zmena("Odebráno auto " + a.getJmenoAuta());
        }
    }

    void natankovat(Auto a) {
        if (a == null) return;
        float volne = a.getMaxPalivo() - a.getPalivo();
        if (volne <= 0.001f) {
            Okno.info(this, "Tankování", "Nádrž auta " + a.getJmenoAuta() + " je už plná.");
            return;
        }
        float cena = garaz.getCenaPaliva(a);
        JTextField litry = Ui.pole(10);
        litry.setText(format(volne));
        JLabel souhrn = Ui.muted("");
        Runnable prepocet = () -> {
            Float l = parseCislo(litry.getText());
            if (l == null) souhrn.setText("Zadej počet litrů.");
            else souhrn.setText("≈ " + Ui.kc(Math.min(l, volne) * cena) + " při ceně " + format(cena) + " Kč/l ("
                    + a.getDruhPaliva() + ")");
        };
        litry.getDocument().addDocumentListener(zmenaTextu(prepocet));
        prepocet.run();

        Okno o = new Okno(this, "Natankovat", a.getJmenoAuta() + " · v nádrži " + format(a.getPalivo()) + " z "
                + format(a.getMaxPalivo()) + " l, volné místo " + format(volne) + " l.");
        o.pole("Litrů", litry, null);
        o.prvek(souhrn);
        o.akce(Ui.sekundarni("Do plna", "fuel", () -> litry.setText(format(volne))));
        o.zavrit("Zrušit");
        o.potvrdit("Natankovat", () -> {
            Float l = parseCislo(litry.getText());
            if (l == null || l <= 0) {
                o.chyba("Zadej kladné číslo.");
                return false;
            }
            if (l > volne + 0.001f) {
                o.chyba("Tolik se do nádrže nevejde (volné místo " + format(volne) + " l).");
                return false;
            }
            return true;
        });
        if (o.ukaz()) {
            float l = garaz.natankuj(a, parseCislo(litry.getText()));
            garaz.zapis("Natankováno: " + a + ", " + format(l) + " l za " + Ui.kc(l * cena));
            zmena("Natankováno " + format(l) + " l za " + Ui.kc(l * cena));
        }
    }

    /** Výlet tam a zpět - využívá metody vylet(), vyletTest() a fuelingCount() z třídy Auto. */
    void vylet(Auto a) {
        if (a == null) return;
        JTextField km = Ui.pole(8);
        JTextArea souhrn = Ui.odstavec("", 13, Ui.text2());
        Runnable prepocet = () -> {
            String t = km.getText().trim();
            if (!CELE_CISLO.matcher(t).matches() || Integer.parseInt(t) <= 0) {
                souhrn.setText("Zadej vzdálenost cíle v celých km.");
                return;
            }
            int k = Integer.parseInt(t);
            float spotrebovano = a.getSpotreba() * k * 2 / 100;
            int tank = Math.max(0, a.fuelingCount(spotrebovano));
            float zbude = Math.max(0, a.vyletTest(k, true));
            souhrn.setText("Celkem " + (k * 2) + " km  ·  spotřeba " + format(spotrebovano) + " l ≈ "
                    + Ui.kc(spotrebovano * garaz.getCenaPaliva(a)) + "\n"
                    + "Tankování po cestě: " + tank + "×  ·  po návratu zbude " + format(zbude) + " l"
                    + (a.getKmDoServisu() < k * 2 ? "\nPo cestě překročíš servisní interval." : ""));
        };
        km.getDocument().addDocumentListener(zmenaTextu(prepocet));
        prepocet.run();
        Okno o = new Okno(this, "Výlet", a.getJmenoAuta() + " · jede se tam a zpět. Na mapě si trasu"
                + " naplánuješ v Plánovači tras.");
        o.pole("Vzdálenost cíle (km jedním směrem)", km, null);
        o.prvek(souhrn);
        o.zavrit("Zrušit");
        o.potvrdit("Vyrazit", () -> {
            String t = km.getText().trim();
            if (!CELE_CISLO.matcher(t).matches() || Integer.parseInt(t) <= 0) {
                o.chyba("Zadej celé kladné číslo (km).");
                return false;
            }
            return true;
        });
        if (o.ukaz()) {
            int k = Integer.parseInt(km.getText().trim());
            float spotrebovano = a.getSpotreba() * k * 2 / 100;
            int tank = Math.max(0, a.fuelingCount(spotrebovano));
            a.vylet(k);
            a.setPalivo(Math.max(0, a.getPalivo())); // ochrana proti zaokrouhlení u floatu
            garaz.zaznamenejVylet(k, tank);
            garaz.zapis("Výlet " + a + ": " + (k * 2) + " km tam a zpět, tankování po cestě " + tank + "×");
            zmena("Výlet " + (k * 2) + " km zapsán");
        }
    }

    void servis(Auto a) {
        if (a == null) return;
        JComboBox<String> popis = Ui.vyber(new String[]{"Pravidelný servis", "Výměna oleje a filtrů",
                "Brzdy", "Pneumatiky", "STK a emise", "Oprava po poruše"});
        popis.setEditable(true);
        JTextField cena = Ui.pole(8);
        cena.setText(String.valueOf(garaz.odhadCenyServisu(a)));
        JTextField datum = Ui.pole(10);
        datum.setText(LocalDate.now().format(DATUM_CZ));
        JCheckBox stk = new JCheckBox("Při servisu proběhla i STK (platnost +2 roky)");
        stk.setOpaque(false);
        stk.setFont(Ui.font(Font.PLAIN, 13));
        stk.setForeground(Ui.text());
        stk.setSelected(a.getDniDoStk() < 60);

        Okno o = new Okno(this, "Zapsat servis", a.getJmenoAuta() + " · od posledního servisu "
                + Math.round(a.getKmOdServisu()) + " km z intervalu " + a.getServisniInterval()
                + " km. Po zápisu se km od servisu vynulují.");
        o.pole("Co se dělalo", popis, null);
        o.pole("Cena (Kč)", cena, "Předvyplněný je odhad aplikace, přepiš ho skutečnou cenou.");
        o.pole("Datum", datum, "Ve tvaru 16. 10. 2026");
        o.prvek(stk);
        o.zavrit("Zrušit");
        o.potvrdit("Zapsat servis", () -> {
            Float c = parseCislo(cena.getText());
            if (c == null) {
                o.chyba("Cena musí být číslo.");
                return false;
            }
            LocalDate d = parseDatum(datum.getText());
            if (d == null) {
                o.chyba("Datum zadej ve tvaru d. m. rrrr.");
                return false;
            }
            if (d.isAfter(LocalDate.now())) {
                o.chyba("Servis nemůže být v budoucnosti.");
                return false;
            }
            return true;
        });
        if (o.ukaz()) {
            LocalDate d = parseDatum(datum.getText());
            Object p = popis.getEditor().getItem();
            garaz.servisuj(a, p == null ? "" : p.toString(), parseCislo(cena.getText()), d);
            if (stk.isSelected()) {
                a.setStkDo(d.plusYears(2));
                garaz.zapis("STK " + a + " platí do " + datum(a.getStkDo()));
            }
            zmena("Servis zapsán: " + a.getJmenoAuta());
        }
    }

    /** Dialog pro ruční připnutí priority s nepovinným důvodem. */
    void pripnout(Auto a) {
        if (a == null) return;
        Ui.Prepinac trida = new Ui.Prepinac(new String[]{"Kritická", "Vysoká", "Střední", "Nízká"},
                (a.jePripnuta() ? a.getPripnuta() : garaz.getPriorita(a)).ordinal());
        JComboBox<String> duvod = Ui.vyber(new String[]{"", "Nařízeno vedením", "Porucha / havárie",
                "Blíží se STK", "Koordinace s jiným servisem", "Čeká na díly", "Firemní auto, nesmí vypadnout"});
        duvod.setEditable(true);
        duvod.setSelectedItem(a.getDuvodPripnuti());
        Okno o = new Okno(this, "Připnout prioritu", a + " · model říká " + garaz.getModelovaPriorita(a)
                + " (skóre " + Math.round(garaz.getSkore(a)) + "). Člověk má poslední slovo: připnutá třída"
                + " přebije výpočet, dokud ji neodepneš.");
        JPanel t = Ui.pruhledny(new FlowLayout(FlowLayout.LEFT, 0, 0));
        t.add(trida);
        int[] vybrana = {(a.jePripnuta() ? a.getPripnuta() : garaz.getPriorita(a)).ordinal()};
        trida.naZmenu(i -> vybrana[0] = i);
        o.pole("Nová třída", t, null);
        o.pole("Důvod (nepovinný)", duvod, "Vyber z nabídky nebo napiš vlastní. Zapíše se do historie.");
        o.zavrit("Zrušit");
        o.potvrdit("Připnout", null);
        if (o.ukaz()) {
            Object zadano = duvod.getEditor().getItem();
            garaz.pripniPrioritu(a, Auto.Priorita.values()[vybrana[0]], zadano == null ? "" : zadano.toString());
            zmena("Priorita připnuta: " + a.getJmenoAuta() + " → " + Auto.Priorita.values()[vybrana[0]]);
        }
    }

    void odepnout(Auto a) {
        if (a == null || !a.jePripnuta()) return;
        garaz.odepniPrioritu(a);
        zmena("Odepnuto, platí modelová priorita " + garaz.getModelovaPriorita(a));
    }

    /** „Proč A před B“: které faktory rozhodly o pořadí dvou aut. */
    void porovnat(Auto prvni, Auto druhe) {
        Auto a = prvni, b = druhe;
        if (garaz.porovnavacPriorit().compare(a, b) > 0) {   // A je vždy to, které je v žebříčku výš
            a = druhe;
            b = prvni;
        }
        JPanel tabulka = Ui.pruhledny(new GridLayout(0, 6, 14, 9));
        for (String h : new String[]{"Faktor", "A", "B", "Body A", "Body B", "A − B"}) {
            tabulka.add(Ui.label(h.toUpperCase(), Font.BOLD, 11, Ui.muted()));
        }
        Garaz.Faktor hlavni = Garaz.Faktor.values()[0];
        float nejvetsi = -Float.MAX_VALUE;
        for (Garaz.Faktor f : Garaz.Faktor.values()) {
            float pa = garaz.getPrispevek(a, f), pb = garaz.getPrispevek(b, f), d = pa - pb;
            if (d > nejvetsi) {
                nejvetsi = d;
                hlavni = f;
            }
            tabulka.add(Ui.label(f.getNazev(), Font.BOLD, 13, Ui.text()));
            tabulka.add(Ui.text(garaz.popisFaktoru(a, f)));
            tabulka.add(Ui.text(garaz.popisFaktoru(b, f)));
            tabulka.add(Ui.text(cislo1(pa)));
            tabulka.add(Ui.text(cislo1(pb)));
            tabulka.add(Ui.label((d > 0.05f ? "+" : "") + cislo1(d), Font.BOLD, 13, d > 0.05f ? Ui.accent() : Ui.muted()));
        }
        float sa = garaz.getSkore(a), sb = garaz.getSkore(b);
        tabulka.add(Ui.label("Skóre", Font.BOLD, 13, Ui.text()));
        tabulka.add(Ui.stitekPriority(garaz.getPriorita(a)));
        tabulka.add(Ui.stitekPriority(garaz.getPriorita(b)));
        tabulka.add(Ui.label(cislo1(sa), Font.BOLD, 14, Ui.text()));
        tabulka.add(Ui.label(cislo1(sb), Font.BOLD, 14, Ui.text()));
        tabulka.add(Ui.label((sa - sb > 0.05f ? "+" : "") + cislo1(sa - sb), Font.BOLD, 14, Ui.accent()));

        StringBuilder sb2 = new StringBuilder();
        float rozdil = sa - sb;
        if (rozdil > 0.05f) {
            sb2.append(a.getJmenoAuta()).append(" má vyšší skóre o ").append(cislo1(rozdil))
                    .append(" b. Nejvíc rozhodl faktor „").append(hlavni.getNazev()).append("“ (")
                    .append(cislo1(nejvetsi)).append(" b).");
        } else if (rozdil < -0.05f) {
            sb2.append("Samotné skóre by řadilo ").append(b.getJmenoAuta()).append(" výš (o ")
                    .append(cislo1(-rozdil)).append(" b), pořadí ale ovlivnila ručně připnutá priorita.");
        } else {
            sb2.append("Skóre obou aut je prakticky stejné.");
        }
        for (Auto x : new Auto[]{a, b}) {
            if (x.jePripnuta()) {
                sb2.append("\n").append(x.getJmenoAuta()).append(" má ručně připnutou prioritu ")
                        .append(x.getPripnuta()).append(x.getDuvodPripnuti().isEmpty()
                                ? " (důvod neuveden)." : ", důvod: " + x.getDuvodPripnuti() + ".");
            }
        }
        JTextArea shrnuti = Ui.odstavec(sb2.toString(), 13.5f, Ui.text());
        Ui.Karta k = new Ui.Karta(new BorderLayout(), 12, 14).vypln(Ui.accentSoft());
        k.add(shrnuti);

        Okno o = new Okno(this, "Proč „" + a.getJmenoAuta() + "“ před „" + b.getJmenoAuta() + "“",
                "A = " + a + "   ·   B = " + b + ". Kladné číslo ve sloupci A − B táhne auto A výš.");
        o.prvek(tabulka);
        o.prvek(k);
        o.potvrdit("Zavřít", null);
        o.ukaz();
    }

    /** Simulace: co když auto na pár dní odstavíme? Kdo ho zastoupí a kolik to bude stát navíc. */
    void simulace(Auto a) {
        if (a == null) return;
        JSlider dni = Ui.posuvnik(1, 30, 7);
        JSlider km = Ui.posuvnik(10, 300, 60);
        JLabel dniL = Ui.silny("7 dní", 13), kmL = Ui.silny("60 km / den", 13);
        JPanel vysledek = Ui.pruhledny(new BorderLayout());
        Runnable prepocet = () -> {
            dniL.setText(dni.getValue() + (dni.getValue() == 1 ? " den" : dni.getValue() < 5 ? " dny" : " dní"));
            kmL.setText(km.getValue() + " km / den");
            Garaz.Simulace s = garaz.simulujOdstavku(a, dni.getValue(), km.getValue());
            vysledek.removeAll();
            Ui.Karta k = new Ui.Karta(new BorderLayout(0, 10), 14, 16);
            JPanel hlava = Ui.pruhledny(new BorderLayout());
            hlava.add(Ui.eyebrow("Výsledek"), BorderLayout.WEST);
            hlava.add(Ui.odhad("simulace"), BorderLayout.EAST);
            k.add(hlava, BorderLayout.NORTH);
            JPanel radky = Ui.pruhledny(new GridLayout(0, 2, 12, 8));
            if (s.nahrada == null) {
                radky.add(Ui.label("Není čím auto nahradit (ostatní auta mají kritickou prioritu).",
                        Font.BOLD, 13, Ui.danger()));
            } else {
                radky.add(Ui.muted("Zastoupí ho"));
                radky.add(Ui.silny(s.nahrada.getJmenoAuta() + " (" + s.nahrada.getSpz() + ")", 13));
                radky.add(Ui.muted("Najede celkem"));
                radky.add(Ui.silny(Ui.tis(s.kmCelkem) + " km", 13));
                radky.add(Ui.muted("Palivo původním autem"));
                radky.add(Ui.silny(Ui.kc(s.palivoPuvodni), 13));
                radky.add(Ui.muted("Palivo náhradou"));
                radky.add(Ui.silny(Ui.kc(s.palivoNahrada), 13));
                radky.add(Ui.muted("Rozdíl"));
                float r = s.getRozdil();
                radky.add(Ui.label((r > 0 ? "+" : "") + Ui.kc(r) + (r > 0 ? " navíc" : " ušetří"), Font.BOLD, 13,
                        r > 0 ? Ui.crit() : Ui.ok()));
                radky.add(Ui.muted("Tankování náhrady"));
                radky.add(Ui.silny(s.tankovaniNahrady + "×", 13));
                if (s.servisNahradyZa >= 0) {
                    radky.add(Ui.muted("Pozor"));
                    radky.add(Ui.label("Náhrada bude potřebovat servis za " + s.servisNahradyZa + " dní",
                            Font.BOLD, 13, Ui.warn()));
                }
            }
            k.add(radky, BorderLayout.CENTER);
            vysledek.add(k);
            vysledek.revalidate();
            vysledek.repaint();
            Window w = SwingUtilities.getWindowAncestor(vysledek);
            if (w != null) w.pack();
        };
        dni.addChangeListener(e -> prepocet.run());
        km.addChangeListener(e -> prepocet.run());
        prepocet.run();
        JPanel r1 = Ui.pruhledny(new BorderLayout(10, 0));
        r1.add(dni, BorderLayout.CENTER);
        r1.add(dniL, BorderLayout.EAST);
        JPanel r2 = Ui.pruhledny(new BorderLayout(10, 0));
        r2.add(km, BorderLayout.CENTER);
        r2.add(kmL, BorderLayout.EAST);
        Okno o = new Okno(this, "Odstávka auta", "Co když " + a.getJmenoAuta() + " na pár dní odstavíme (servis,"
                + " nehoda)? Jízdy převezme auto, kterému to vyjde nejlevněji a samo nemá kritickou prioritu.");
        o.pole("Jak dlouho", r1, null);
        o.pole("Kolik auto denně najezdí", r2, null);
        o.prvek(vysledek);
        o.potvrdit("Zavřít", null);
        o.ukaz();
    }

    void mapaCR(Auto a) {
        if (a == null) {
            Okno.info(this, "Plánovač tras", "Nejdřív přidej do garáže auto, se kterým se pojede.");
            return;
        }
        MapaDialog d = new MapaDialog(this, a, garaz.getCenaPaliva(a));
        d.setVisible(true);
        if (d.isPotvrzeno()) {
            int km = d.getKm();
            float celkem = d.isTamZpet() ? km * 2f : km;
            int tank = Math.max(0, a.fuelingCount(a.getSpotreba() * celkem / 100));
            if (d.isTamZpet()) a.vylet(km);
            else a.jizda(km);
            a.setPalivo(Math.max(0, a.getPalivo()));
            garaz.zaznamenejJizdu(celkem, tank);
            garaz.zapis("Cesta " + a + ": " + d.getPopis() + ", " + Math.round(celkem) + " km");
            zmena("Cesta zapsána: " + Math.round(celkem) + " km");
        }
    }

    void mapaSvet(Auto a) {
        if (a == null) {
            Okno.info(this, "Mapa světa", "Nejdřív přidej do garáže auto.");
            return;
        }
        new SvetMapaDialog(this, a, garaz.getCenaPaliva(a)).setVisible(true);
    }

    void model3D(Auto a) {
        if (a == null) return;
        Model3DDialog d = new Model3DDialog(this, a);
        d.setVisible(true);
        if (d.isZmeneno()) {
            garaz.zapis("Změněna barva laku " + a);
            zmena("Barva laku uložena");
        }
    }

    // ------------------------------------------------------------------
    // nastavení, export, soubory
    // ------------------------------------------------------------------

    void nastaveni() {
        JTextField cena = Ui.pole(8);
        cena.setText(format(garaz.getCenaPaliva()));
        JTextField rozpocet = Ui.pole(8);
        rozpocet.setText(String.valueOf(garaz.getRocniRozpocet()));
        Okno o = new Okno(this, "Nastavení a data", "Data se ukládají automaticky po každé změně do souboru "
                + soubor + ".");
        o.pole("Cena benzínu (Kč/l)", cena, "Nafta, LPG a hybrid se přepočítají koeficientem (Metodika).");
        o.pole("Roční rozpočet na servis (Kč)", rozpocet, null);
        JPanel data = Ui.pruhledny(new GridLayout(2, 2, 8, 8));
        data.add(Ui.sekundarni("Export CSV", "download", () -> {
            o.dispose();
            exportovatCsv();
        }));
        data.add(Ui.sekundarni("Otevřít zálohu…", "upload", () -> {
            o.dispose();
            otevritSoubor();
        }));
        data.add(Ui.sekundarni("Ukázková data", "car", () -> {
            o.dispose();
            if (Okno.potvrd(this, "Načíst ukázková data", "Současná auta a historie se nahradí ukázkovou garáží"
                    + " s osmi auty a rokem tankování.", "Načíst", true)) {
                Uloziste.ukazkovaData(garaz);
                vybrane = null;
                zmena("Načtena ukázková data");
            }
        }));
        Ui.Tlacitko smazat = new Ui.Tlacitko("Vymazat vše", Ui.Styl.NEBEZPECNE, "trash");
        smazat.addActionListener(e -> {
            o.dispose();
            if (Okno.potvrd(this, "Vymazat celou garáž", "Smažou se všechna auta, kniha tankování, servisy"
                    + " i historie. Nastavení zůstane. Nejde to vrátit.", "Vymazat", true)) {
                garaz.vymazVse();
                garaz.zapis("Garáž vymazána");
                vybrane = null;
                zmena("Garáž vymazána");
            }
        });
        data.add(smazat);
        o.pole("Data", data, null);
        o.zavrit("Zrušit");
        o.potvrdit("Uložit", () -> {
            Float c = parseCislo(cena.getText());
            Float r = parseCislo(rozpocet.getText());
            if (c == null || c <= 0 || c > 500) {
                o.chyba("Cena paliva musí být kladné číslo (Kč za litr).");
                return false;
            }
            if (r == null || r < 0) {
                o.chyba("Rozpočet musí být číslo.");
                return false;
            }
            return true;
        });
        if (o.ukaz()) {
            float c = parseCislo(cena.getText());
            int r = Math.round(parseCislo(rozpocet.getText()));
            if (Math.abs(c - garaz.getCenaPaliva()) > 0.001f) garaz.zapis("Cena paliva změněna na " + format(c) + " Kč/l");
            if (r != garaz.getRocniRozpocet()) garaz.zapis("Roční rozpočet na servis změněn na " + Ui.kc(r));
            garaz.setCenaPaliva(c);
            garaz.setRocniRozpocet(r);
            zmena("Nastavení uloženo");
        }
    }

    /** Uloží garáž do CSV souboru (UTF-8 s BOM, ať si Excel poradí s diakritikou). */
    void exportovatCsv() {
        if (garaz.jePrazdna()) {
            Okno.info(this, "Export", "V garáži nejsou žádná auta.");
            return;
        }
        JFileChooser vyber = new JFileChooser();
        vyber.setDialogTitle("Export garáže do CSV");
        vyber.setSelectedFile(new File("garaz-" + LocalDate.now() + ".csv"));
        if (vyber.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File cil = vyber.getSelectedFile();
        if (!cil.getName().toLowerCase().endsWith(".csv")) {
            cil = new File(cil.getPath() + ".csv");
        }
        if (cil.exists() && !Okno.potvrd(this, "Export", "Soubor " + cil.getName() + " už existuje. Přepsat?",
                "Přepsat", true)) {
            return;
        }
        try {
            byte[] data = ("﻿" + garaz.exportCsv()).getBytes(StandardCharsets.UTF_8);
            Files.write(cil.toPath(), data);
            garaz.zapis("Exportováno do souboru " + cil.getName());
            zmena("Uloženo: " + cil.getName());
        } catch (IOException ex) {
            Okno.info(this, "Chyba exportu", "Soubor se nepodařilo uložit: " + ex.getMessage());
        }
    }

    private void otevritSoubor() {
        JFileChooser vyber = new JFileChooser(soubor.getParent().toFile());
        vyber.setDialogTitle("Otevřít uloženou garáž (.tsv)");
        if (vyber.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path p = vyber.getSelectedFile().toPath();
        try {
            int n = Uloziste.nacti(garaz, p);
            soubor = p;
            vybrane = null;
            zmena("Načteno " + n + " aut ze souboru " + p.getFileName());
        } catch (IOException | DateTimeParseException ex) {
            Okno.info(this, "Soubor nejde načíst", ex.getMessage());
        }
    }

    static javax.swing.event.DocumentListener zmenaTextu(Runnable r) {
        return new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                r.run();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                r.run();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                r.run();
            }
        };
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        nastavUIManager();
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
