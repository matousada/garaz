package gui;

import data.Auto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.Year;
import java.util.regex.Pattern;

public class AutoDialog extends JDialog {

    // ---- REGEX (do dokumentace) ----
    // CISLO_VSTUP: při psaní do číselných polí povolí jen rozepsané číslo (omezVstup)
    // ROK_VSTUP  : při psaní roku povolí max. 4 číslice (omezVstup)
    // CELE_VSTUP : při psaní servisního intervalu povolí jen číslice (omezVstup)
    // DATUM_VSTUP: při psaní data STK povolí jen číslice, tečky a mezery (omezVstup)
    // ROK        : kontrola hotového roku - přesně 4 číslice (tryClose)
    // SPZ        : kontrola SPZ - 5 až 8 velkých písmen bez diakritiky nebo číslic (tryClose)
    // (kontrola hotového čísla a data je v MainFrame.parseCislo / parseDatum)
    private static final Pattern CISLO_VSTUP = Pattern.compile("\\d*([.,]\\d*)?");
    private static final Pattern ROK_VSTUP = Pattern.compile("\\d{0,4}");
    private static final Pattern CELE_VSTUP = Pattern.compile("\\d{0,6}");
    private static final Pattern DATUM_VSTUP = Pattern.compile("[\\d. ]{0,12}");
    private static final Pattern ROK = Pattern.compile("\\d{4}");
    private static final Pattern SPZ = Pattern.compile("[A-Z0-9]{5,8}");

    private JPanel managePanel;   // panel se všemi prvky dialogu (GroupLayout)

    private JTextField idText, jmenoText, znackaText, spzText, rokText,
            spotrebaText, maxPalivoText, palivoText,
            intervalText, odServisuText, stkText, poznamkaText;
    private JComboBox<Auto.Palivo> palivoVyber;
    private JLabel chybaLabel;

    private Auto data;       // výsledek dialogu (null = zrušeno)
    private Auto editovane;  // auto, které se právě upravuje (null = nové auto)

    private DialogResult result = DialogResult.Cancel;

    public AutoDialog(Frame owner, String name) {
        super(owner, name, true);
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        initComponents(name);
        idText.setText("přidělí se automaticky");
        intervalText.setText("15000");
        odServisuText.setText("0");
        stkText.setText(LocalDate.now().plusYears(2).format(MainFrame.DATUM_CZ));
        this.pack();
        this.setMinimumSize(new Dimension(640, getHeight()));
        this.setLocationRelativeTo(owner);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                result = DialogResult.Cancel;
                tryClose();
            }
        });
    }

    /** Pole pustí jen text, který odpovídá vzoru (regex) - ostatní znaky se nenapíšou. */
    private void omezVstup(JTextField field, Pattern vzor) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                    throws BadLocationException {
                replace(fb, offset, 0, text, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                String aktualni = fb.getDocument().getText(0, fb.getDocument().getLength());
                String novy = aktualni.substring(0, offset)
                        + (text == null ? "" : text)
                        + aktualni.substring(offset + length);
                if (vzor.matcher(novy).matches()) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
    }

    private static JLabel popisek(String text) {
        return Ui.label(text, Font.BOLD, 12.5f, Ui.text2());
    }

    private void initComponents(String nazev) {
        // ---- labely (levý sloupec obou bloků) ----
        JLabel idLabel = popisek("ID");
        JLabel jmenoLabel = popisek("Jméno");
        JLabel znackaLabel = popisek("Značka");
        JLabel spzLabel = popisek("SPZ");
        JLabel rokLabel = popisek("Rok výroby");
        JLabel druhLabel = popisek("Palivo");
        JLabel spotrebaLabel = popisek("Spotřeba (l/100 km)");
        JLabel maxPalivoLabel = popisek("Kapacita nádrže (l)");
        JLabel palivoLabel = popisek("V nádrži teď (l)");
        JLabel intervalLabel = popisek("Servisní interval (km)");
        JLabel odServisuLabel = popisek("Km od servisu");
        JLabel stkLabel = popisek("STK platí do");
        JLabel poznamkaLabel = popisek("Poznámka");

        // ---- textfieldy (pravý sloupec) ----
        idText = Ui.pole(16);
        jmenoText = Ui.pole(16);
        znackaText = Ui.pole(16);
        spzText = Ui.pole(16);
        rokText = Ui.pole(16);
        spotrebaText = Ui.pole(16);
        maxPalivoText = Ui.pole(16);
        palivoText = Ui.pole(16);
        intervalText = Ui.pole(16);
        odServisuText = Ui.pole(16);
        stkText = Ui.pole(16);
        poznamkaText = Ui.pole(16);
        palivoVyber = Ui.vyber(Auto.Palivo.values());

        idText.setEditable(false);
        idText.setForeground(Ui.muted());

        omezVstup(rokText, ROK_VSTUP);
        omezVstup(spotrebaText, CISLO_VSTUP);
        omezVstup(maxPalivoText, CISLO_VSTUP);
        omezVstup(palivoText, CISLO_VSTUP);
        omezVstup(intervalText, CELE_VSTUP);
        omezVstup(odServisuText, CISLO_VSTUP);
        omezVstup(stkText, DATUM_VSTUP);

        // ---- hlavička a tlačítka ----
        JLabel titulek = Ui.titulek(nazev, 22);
        JLabel podtitul = Ui.muted("Povinné jsou údaje vlevo. Servis a STK zpřesní prioritu a plán rozpočtu.", 13);
        JLabel blok1 = Ui.eyebrow("Vozidlo");
        JLabel blok2 = Ui.eyebrow("Provoz a servis");
        chybaLabel = Ui.label(" ", Font.BOLD, 12.5f, Ui.danger());

        Ui.Tlacitko cancelButton = Ui.sekundarni("Zrušit", null, () -> {
            result = DialogResult.Cancel;
            tryClose();
        });
        Ui.Tlacitko okButton = Ui.primarni("Uložit", "check", () -> {
            result = DialogResult.OK;
            tryClose();
        });

        // ================================================================
        // managePanel - JPanel, ve kterém jsou uspořádané všechny prvky
        // ================================================================
        managePanel = new JPanel();
        managePanel.setBackground(Ui.surface());
        managePanel.setBorder(new EmptyBorder(14, 18, 14, 18));

        GroupLayout layout = new GroupLayout(managePanel);
        managePanel.setLayout(layout);
        layout.setAutoCreateGaps(true);            // mezery mezi komponentami
        layout.setAutoCreateContainerGaps(true);   // okraj kolem celého panelu

        // Každá komponenta se do GroupLayoutu přidává DVAKRÁT:
        //   1) do horizontální skupiny (kde je zleva doprava - sloupec)
        //   2) do vertikální skupiny (kde je shora dolů - řádek)
        // Formulář má dva bloky vedle sebe, každý se dvěma sloupci (popisek | pole).

        int sirka = 190;
        GroupLayout.SequentialGroup bloky = layout.createSequentialGroup()
                // blok 1, sloupec popisků
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(blok1)
                        .addComponent(idLabel)
                        .addComponent(jmenoLabel)
                        .addComponent(znackaLabel)
                        .addComponent(spzLabel)
                        .addComponent(rokLabel)
                        .addComponent(spotrebaLabel)
                        .addComponent(maxPalivoLabel))
                // blok 1, sloupec polí
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(idText, sirka, sirka, Short.MAX_VALUE)
                        .addComponent(jmenoText, sirka, sirka, Short.MAX_VALUE)
                        .addComponent(znackaText, sirka, sirka, Short.MAX_VALUE)
                        .addComponent(spzText, sirka, sirka, Short.MAX_VALUE)
                        .addComponent(rokText, sirka, sirka, Short.MAX_VALUE)
                        .addComponent(spotrebaText, sirka, sirka, Short.MAX_VALUE)
                        .addComponent(maxPalivoText, sirka, sirka, Short.MAX_VALUE))
                .addGap(26)
                // blok 2, sloupec popisků
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(blok2)
                        .addComponent(druhLabel)
                        .addComponent(palivoLabel)
                        .addComponent(intervalLabel)
                        .addComponent(odServisuLabel)
                        .addComponent(stkLabel)
                        .addComponent(poznamkaLabel))
                // blok 2, sloupec polí
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(palivoVyber, sirka, sirka, Short.MAX_VALUE)
                        .addComponent(palivoText, sirka, sirka, Short.MAX_VALUE)
                        .addComponent(intervalText, sirka, sirka, Short.MAX_VALUE)
                        .addComponent(odServisuText, sirka, sirka, Short.MAX_VALUE)
                        .addComponent(stkText, sirka, sirka, Short.MAX_VALUE)
                        .addComponent(poznamkaText, sirka, sirka, Short.MAX_VALUE));

        // ---- HORIZONTÁLNÍ skupina ----
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(titulek)
                        .addComponent(podtitul)
                        .addGroup(bloky)
                        .addComponent(chybaLabel)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(cancelButton)
                                .addComponent(okButton))
        );

        // ---- VERTIKÁLNÍ skupina: řádky pod sebou (label + jeho pole v jednom řádku) ----
        int v = 36;   // výška pole
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(titulek)
                        .addComponent(podtitul)
                        .addGap(10)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(blok1)
                                .addComponent(blok2))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(idLabel).addComponent(idText, v, v, v)
                                .addComponent(druhLabel).addComponent(palivoVyber, v, v, v))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(jmenoLabel).addComponent(jmenoText, v, v, v)
                                .addComponent(palivoLabel).addComponent(palivoText, v, v, v))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(znackaLabel).addComponent(znackaText, v, v, v)
                                .addComponent(intervalLabel).addComponent(intervalText, v, v, v))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(spzLabel).addComponent(spzText, v, v, v)
                                .addComponent(odServisuLabel).addComponent(odServisuText, v, v, v))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(rokLabel).addComponent(rokText, v, v, v)
                                .addComponent(stkLabel).addComponent(stkText, v, v, v))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(spotrebaLabel).addComponent(spotrebaText, v, v, v)
                                .addComponent(poznamkaLabel).addComponent(poznamkaText, v, v, v))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(maxPalivoLabel).addComponent(maxPalivoText, v, v, v))
                        .addGap(6)
                        .addComponent(chybaLabel)
                        .addGap(6)
                        // poslední řádek: obě tlačítka
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(cancelButton)
                                .addComponent(okButton))
        );

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(managePanel, BorderLayout.CENTER);

        getRootPane().setDefaultButton(okButton);
        getRootPane().registerKeyboardAction(
                e -> {
                    result = DialogResult.Cancel;
                    tryClose();
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    /** Výsledek dialogu: nové / upravené auto, nebo null při zrušení. */
    public Auto getData() {
        return data;
    }

    /** Naplní formulář údaji existujícího auta (úprava). Auto se změní až po stisku Uložit. */
    public void setData(Auto auto) {
        this.editovane = auto;
        idText.setText(String.valueOf(auto.getCarId()));
        jmenoText.setText(auto.getJmenoAuta());
        znackaText.setText(auto.getZnacka());
        spzText.setText(auto.getSpz());
        rokText.setText(String.valueOf(auto.getRokVyroby()));
        spotrebaText.setText(MainFrame.format(auto.getSpotreba()));
        maxPalivoText.setText(MainFrame.format(auto.getMaxPalivo()));
        palivoText.setText(MainFrame.format(auto.getPalivo()));
        palivoVyber.setSelectedItem(auto.getDruhPaliva());
        intervalText.setText(String.valueOf(auto.getServisniInterval()));
        odServisuText.setText(MainFrame.format(Math.round(auto.getKmOdServisu())));
        stkText.setText(auto.getStkDo() == null ? "" : auto.getStkDo().format(MainFrame.DATUM_CZ));
        poznamkaText.setText(auto.getPoznamka());
    }

    private void chyba(String zprava, JComponent field) {
        chybaLabel.setText(zprava);
        field.requestFocus();
        result = DialogResult.Cancel; // dialog zůstává otevřený, křížek ho pak zase zruší
    }

    private void tryClose() {
        if (result == DialogResult.Cancel) {
            this.data = null;
            this.dispose();
            return;
        }

        if (jmenoText.getText().trim().isBlank()) {
            chyba("Jméno není vyplněné", jmenoText);
            return;
        }
        if (znackaText.getText().trim().isBlank()) {
            chyba("Značka není vyplněná", znackaText);
            return;
        }

        String spz = spzText.getText().replace(" ", "").toUpperCase();
        if (!SPZ.matcher(spz).matches()) {
            chyba("SPZ musí mít 5 až 8 znaků (písmena bez diakritiky a číslice)", spzText);
            return;
        }

        String rokTxt = rokText.getText().trim();
        if (!ROK.matcher(rokTxt).matches()) {
            chyba("Rok výroby musí mít 4 číslice", rokText);
            return;
        }
        int rok = Integer.parseInt(rokTxt);
        int maxRok = Year.now().getValue() + 1;
        if (rok < 1886 || rok > maxRok) {
            chyba("Rok výroby musí být mezi 1886 a " + maxRok, rokText);
            return;
        }

        Float spotreba = MainFrame.parseCislo(spotrebaText.getText());
        if (spotreba == null || spotreba <= 0) {
            chyba("Spotřeba musí být kladné číslo", spotrebaText);
            return;
        }
        Float maxPalivo = MainFrame.parseCislo(maxPalivoText.getText());
        if (maxPalivo == null || maxPalivo <= 0) {
            chyba("Kapacita nádrže musí být kladné číslo", maxPalivoText);
            return;
        }
        Float palivo = MainFrame.parseCislo(palivoText.getText());
        if (palivo == null) {
            chyba("Palivo musí být číslo", palivoText);
            return;
        }
        if (palivo > maxPalivo) {
            chyba("Palivo nemůže být větší než kapacita nádrže", palivoText);
            return;
        }
        String intervalTxt = intervalText.getText().trim();
        if (intervalTxt.isEmpty() || Integer.parseInt(intervalTxt) < 1000) {
            chyba("Servisní interval musí být aspoň 1000 km", intervalText);
            return;
        }
        Float odServisu = MainFrame.parseCislo(odServisuText.getText());
        if (odServisu == null) {
            chyba("Km od servisu musí být číslo", odServisuText);
            return;
        }
        LocalDate stk = null;
        if (!stkText.getText().trim().isEmpty()) {
            stk = MainFrame.parseDatum(stkText.getText());
            if (stk == null) {
                chyba("Datum STK zadej ve tvaru d. m. rrrr (např. 16. 10. 2027), nebo nech prázdné", stkText);
                return;
            }
        }

        // při úpravě měníme původní auto (zůstane mu ID), jinak vznikne nové
        Auto a = (editovane != null) ? editovane : new Auto();
        a.setJmenoAuta(jmenoText.getText().trim());
        a.setZnacka(znackaText.getText().trim());
        a.setSpz(spz);
        a.setRokVyroby(rok);
        a.setSpotreba(spotreba);
        a.setMaxPalivo(maxPalivo);
        a.setPalivo(palivo);
        a.setDruhPaliva((Auto.Palivo) palivoVyber.getSelectedItem());
        a.setServisniInterval(Integer.parseInt(intervalTxt));
        a.setKmOdServisu(odServisu);
        a.setStkDo(stk);
        a.setPoznamka(poznamkaText.getText());
        this.data = a;
        this.dispose();
    }
}
