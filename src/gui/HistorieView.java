package gui;

import data.Garaz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Historie změn (audit): kdo, kdy a co změnil. Hledání a filtr podle druhu záznamu. */
class HistorieView extends Pohled {

    private static final String[] DRUHY = {"Vše", "Auta", "Tankování", "Servis", "Priorita", "Cesty"};
    private static final String[][] KLICE = {
            {},
            {"přidáno", "upraveno", "odebráno", "poznámka", "barva"},
            {"natankováno"},
            {"servis", "stk", "rozpočet"},
            {"priorit", "váh", "připn", "odepn"},
            {"výlet", "cesta"},
    };

    private String hledano = "";
    private int druh = 0;
    private JPanel seznam;

    HistorieView(MainFrame app) {
        super(app);
    }

    @Override
    String nazev() {
        return "Historie změn";
    }

    @Override
    String ikona() {
        return "history";
    }

    @Override
    String podtitul() {
        return "Každá změna se zapíše s časem a jménem uživatele. Nic se nepřepisuje potichu.";
    }

    @Override
    void postav() {
        JTextField h = Ui.hledani("Hledat v historii…");
        h.setText(hledano);
        h.setPreferredSize(new Dimension(320, 38));
        h.getDocument().addDocumentListener(MainFrame.zmenaTextu(() -> {
            hledano = h.getText();
            naplnit();
        }));
        Ui.Prepinac p = new Ui.Prepinac(DRUHY, druh);
        p.naZmenu(i -> {
            druh = i;
            naplnit();
        });
        JPanel ovladani = Ui.pruhledny(new BorderLayout(14, 0));
        ovladani.add(h, BorderLayout.WEST);
        JPanel pObal = Ui.pruhledny(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        pObal.add(p);
        ovladani.add(pObal, BorderLayout.EAST);
        blok(ovladani);

        seznam = Ui.pruhledny(new GridBagLayout());
        Ui.Karta k = new Ui.Karta(new BorderLayout(), 18, 22);
        k.add(seznam);
        blok(k);
        naplnit();
    }

    private boolean odpovida(Garaz.Zaznam z) {
        String t = MainFrame.bezDiakritiky(z.getText().toLowerCase(Locale.ROOT));
        if (!hledano.trim().isEmpty()
                && !t.contains(MainFrame.bezDiakritiky(hledano.trim().toLowerCase(Locale.ROOT)))
                && !z.getKdo().toLowerCase(Locale.ROOT).contains(hledano.trim().toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (druh == 0) return true;
        for (String k : KLICE[druh]) if (t.contains(MainFrame.bezDiakritiky(k))) return true;
        return false;
    }

    private void naplnit() {
        seznam.removeAll();
        List<Garaz.Zaznam> h = g.getHistorie();
        Collections.reverse(h);
        int y = 0, zobrazeno = 0;
        LocalDate den = null;
        for (Garaz.Zaznam z : h) {
            if (!odpovida(z)) continue;
            if (zobrazeno++ >= 200) break;
            LocalDate d = z.getCas().toLocalDate();
            if (!d.equals(den)) {
                den = d;
                JLabel l = Ui.eyebrow(d.equals(LocalDate.now()) ? "Dnes" : d.equals(LocalDate.now().minusDays(1))
                        ? "Včera" : MainFrame.datum(d));
                l.setBorder(new EmptyBorder(y == 0 ? 0 : 14, 0, 6, 0));
                seznam.add(l, gc(y++));
            }
            seznam.add(radek(z), gc(y++));
        }
        if (zobrazeno == 0) {
            seznam.add(Ui.muted("Nic nenalezeno."), gc(y++));
        }
        seznam.revalidate();
        seznam.repaint();
    }

    private static GridBagConstraints gc(int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        return c;
    }

    private JComponent radek(Garaz.Zaznam z) {
        String t = z.getText().toLowerCase(Locale.ROOT);
        String ikona = "history";
        Color barva = Ui.muted();
        if (t.contains("ukázková")) {
            ikona = "car";
            barva = Ui.accent();
        } else if (t.contains("natankov")) {
            ikona = "fuel";
            barva = Ui.accent();
        } else if (t.contains("servis") || t.contains("stk")) {
            ikona = "wrench";
            barva = Ui.warn();
        } else if (t.contains("priorit") || t.contains("připn") || t.contains("odepn") || t.contains("váh")) {
            ikona = "pin";
            barva = Ui.crit();
        } else if (t.contains("výlet") || t.contains("cesta")) {
            ikona = "route";
            barva = Ui.ok();
        } else if (t.contains("úspěch")) {
            ikona = "trophy";
            barva = Ui.yellow();
        } else if (t.contains("auto")) {
            ikona = "car";
            barva = Ui.text2();
        }
        Color b = barva;
        String ik = ikona;
        JComponent znak = new JComponent() {
            @Override
            protected void paintComponent(Graphics gr) {
                Graphics2D g2 = (Graphics2D) gr.create();
                Ui.aa(g2);
                g2.setColor(Ui.line());
                g2.fillRect(15, 0, 2, getHeight());
                g2.setColor(Ui.surface());
                g2.fill(new Ellipse2D.Float(2, 4, 28, 28));
                g2.setColor(Ui.soft(b, 0.16f));
                g2.fill(new Ellipse2D.Float(2, 4, 28, 28));
                Ui.Ikona.kresli(g2, ik, 9, 11, 14, b);
                g2.dispose();
            }
        };
        znak.setPreferredSize(new Dimension(32, 40));
        JPanel r = Ui.pruhledny(new BorderLayout(12, 0));
        r.add(znak, BorderLayout.WEST);
        JPanel texty = Ui.pruhledny(new GridLayout(2, 1));
        texty.add(Ui.text(z.getText()));
        texty.add(Ui.muted(z.getCasText() + "  ·  " + z.getKdo(), 12));
        r.add(texty, BorderLayout.CENTER);
        return r;
    }
}
