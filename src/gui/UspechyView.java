package gui;

import data.Garaz;

import javax.swing.*;
import java.awt.*;

/** Úspěchy: odemčené i zamčené odznaky s postupem k cíli a souhrnná statistika jízd. */
class UspechyView extends Pohled {

    UspechyView(MainFrame app) {
        super(app);
    }

    @Override
    String nazev() {
        return "Úspěchy";
    }

    @Override
    String ikona() {
        return "trophy";
    }

    @Override
    String podtitul() {
        int n = 0;
        for (Garaz.Uspech u : Garaz.Uspech.values()) if (g.jeOdemcen(u)) n++;
        return "Odemčeno " + n + " z " + Garaz.Uspech.values().length + ". Odznaky se odemykají samy, jak garáž používáš.";
    }

    @Override
    void postav() {
        blok(vedle(14, null,
                Ui.kpi("Najeto celkem", Ui.tis(g.getCelkemKm()) + " km", "všechna auta dohromady", null, "route"),
                Ui.kpi("Výletů", String.valueOf(g.getPocetVyletu()), "nejdelší " + Ui.tis(g.getNejdelsiVylet()) + " km", null, "map"),
                Ui.kpi("Tankování", String.valueOf(g.getPocetTankovani()), "zapsaných v aplikaci", null, "fuel"),
                Ui.kpi("Servisů", String.valueOf(g.getServisy().size()), "v servisní knize", null, "wrench")));

        JPanel mrizka = Ui.pruhledny(new GridLayout(0, 3, 14, 14));
        for (Garaz.Uspech u : Garaz.Uspech.values()) mrizka.add(odznak(u));
        blok(mrizka);
    }

    private JComponent odznak(Garaz.Uspech u) {
        boolean ano = g.jeOdemcen(u);
        float postup = Math.min(1f, g.getProgress(u) / u.getCil());
        Ui.Karta k = new Ui.Karta(new BorderLayout(14, 0), 16, 18);
        if (ano) k.prouzek(Ui.warn());
        JComponent medaile = new JComponent() {
            @Override
            protected void paintComponent(Graphics gr) {
                Graphics2D g2 = (Graphics2D) gr.create();
                Ui.aa(g2);
                Color b = ano ? Ui.warn() : Ui.muted();
                g2.setColor(Ui.soft(b, ano ? 0.18f : 0.10f));
                g2.fillOval(0, 0, 52, 52);
                g2.setColor(Ui.soft(b, 0.5f));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(4, 4, 44, 44);
                Ui.Ikona.kresli(g2, ano ? "trophy" : "pin", 14, 14, 24, b);
                g2.dispose();
            }
        };
        medaile.setPreferredSize(new Dimension(52, 52));
        JPanel mObal = Ui.pruhledny(new GridBagLayout());
        mObal.add(medaile);
        k.add(mObal, BorderLayout.WEST);

        JPanel t = Ui.pruhledny(new GridLayout(0, 1, 0, 4));
        JPanel h = Ui.pruhledny(new BorderLayout());
        h.add(Ui.label(u.getNazev(), Font.BOLD, 14.5f, ano ? Ui.text() : Ui.text2()), BorderLayout.WEST);
        if (ano) h.add(new JLabel(new Ui.Ikona("check", 16, Ui.ok())), BorderLayout.EAST);
        t.add(h);
        t.add(Ui.muted(u.getPopis(), 12.5f));
        t.add(new Ui.Ukazatel(ano ? 1f : postup, ano ? Ui.warn() : Ui.accent(), 6));
        String p = u == Garaz.Uspech.POKLADNIK ? (ano ? "splněno" : "plán se zatím do rozpočtu nevejde")
                : Ui.tis(Math.min(g.getProgress(u), u.getCil())) + " / " + Ui.tis(u.getCil());
        t.add(Ui.muted(p, 11.5f));
        k.add(t, BorderLayout.CENTER);
        return k;
    }
}
