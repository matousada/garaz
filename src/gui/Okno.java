package gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.function.BooleanSupplier;

/**
 * Modální dialog v designu aplikace: nadpis, popis, pole pod sebou, chybová hláška a tlačítka.
 * Nahrazuje šedé JOptionPane, aby dialogy vypadaly stejně jako zbytek aplikace.
 */
final class Okno extends JDialog {

    private final JPanel obsah;
    private final JLabel chyba;
    private final JPanel tlacitka;
    private boolean potvrzeno;

    Okno(Window rodic, String titulek, String popis) {
        super(rodic, titulek, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Ui.surface());
        root.setBorder(new EmptyBorder(22, 24, 18, 24));

        JPanel hlava = Ui.pruhledny(new BorderLayout(0, 6));
        hlava.add(Ui.titulek(titulek, 22), BorderLayout.NORTH);
        if (popis != null && !popis.isEmpty()) {
            JTextArea p = Ui.odstavec(popis, 13.5f, Ui.muted());
            p.setColumns(34);
            hlava.add(p, BorderLayout.CENTER);
        }

        obsah = Ui.pruhledny(null);
        obsah.setLayout(new BoxLayout(obsah, BoxLayout.Y_AXIS));

        chyba = Ui.label(" ", Font.BOLD, 12.5f, Ui.danger());
        chyba.setAlignmentX(Component.LEFT_ALIGNMENT);

        tlacitka = Ui.pruhledny(new FlowLayout(FlowLayout.RIGHT, 8, 0));

        JPanel dole = Ui.pruhledny(new BorderLayout(0, 10));
        dole.add(chyba, BorderLayout.NORTH);
        dole.add(tlacitka, BorderLayout.CENTER);

        root.add(hlava, BorderLayout.NORTH);
        root.add(obsah, BorderLayout.CENTER);
        root.add(dole, BorderLayout.SOUTH);
        setContentPane(root);

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Přidá pole s popiskem nad ním a nepovinnou nápovědou pod ním. */
    Okno pole(String popisek, JComponent pole, String napoveda) {
        JPanel blok = Ui.pruhledny(new BorderLayout(0, 5));
        blok.add(Ui.label(popisek, Font.BOLD, 12.5f, Ui.text2()), BorderLayout.NORTH);
        blok.add(pole, BorderLayout.CENTER);
        if (napoveda != null) blok.add(Ui.muted(napoveda, 12), BorderLayout.SOUTH);
        blok.setAlignmentX(Component.LEFT_ALIGNMENT);
        blok.setMaximumSize(new Dimension(Integer.MAX_VALUE, blok.getPreferredSize().height));
        if (obsah.getComponentCount() > 0) obsah.add(Box.createVerticalStrut(12));
        obsah.add(blok);
        return this;
    }

    /** Přidá libovolnou komponentu (graf, tabulku, souhrn). */
    Okno prvek(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (obsah.getComponentCount() > 0) obsah.add(Box.createVerticalStrut(12));
        obsah.add(c);
        return this;
    }

    /** Tlačítko, které jen zavře okno. */
    Okno zavrit(String text) {
        Ui.Tlacitko t = Ui.sekundarni(text, null, this::dispose);
        tlacitka.add(t);
        return this;
    }

    /** Hlavní tlačítko: když kontrola vrátí true, dialog se zavře jako potvrzený. */
    Okno potvrdit(String text, BooleanSupplier kontrola) {
        Ui.Tlacitko t = Ui.primarni(text, null, () -> {
            if (kontrola == null || kontrola.getAsBoolean()) {
                potvrzeno = true;
                dispose();
            }
        });
        tlacitka.add(t);
        getRootPane().setDefaultButton(t);
        return this;
    }

    /** Červené tlačítko pro nevratné akce. */
    Okno nebezpecne(String text, BooleanSupplier kontrola) {
        Ui.Tlacitko t = new Ui.Tlacitko(text, Ui.Styl.NEBEZPECNE);
        t.addActionListener(e -> {
            if (kontrola == null || kontrola.getAsBoolean()) {
                potvrzeno = true;
                dispose();
            }
        });
        tlacitka.add(t);
        return this;
    }

    /** Vlastní tlačítko vlevo od ostatních (např. „Do plna“). */
    Okno akce(Ui.Tlacitko t) {
        tlacitka.add(t, 0);
        return this;
    }

    void chyba(String text) {
        chyba.setText(text == null || text.isEmpty() ? " " : text);
    }

    void potvrdAZavri() {
        potvrzeno = true;
        dispose();
    }

    /** Ukáže dialog a počká. Vrací true, když uživatel potvrdil. */
    boolean ukaz() {
        pack();
        setMinimumSize(new Dimension(Math.max(440, getWidth()), getHeight()));
        setLocationRelativeTo(getOwner());
        setVisible(true);
        return potvrzeno;
    }

    // ------------------------------------------------------------------
    // rychlé varianty
    // ------------------------------------------------------------------

    static boolean potvrd(Window rodic, String titulek, String text, String tlacitko, boolean nebezpecne) {
        Okno o = new Okno(rodic, titulek, text);
        o.zavrit("Zrušit");
        if (nebezpecne) o.nebezpecne(tlacitko, null);
        else o.potvrdit(tlacitko, null);
        return o.ukaz();
    }

    static void info(Window rodic, String titulek, String text) {
        new Okno(rodic, titulek, text).potvrdit("Rozumím", null).ukaz();
    }
}
