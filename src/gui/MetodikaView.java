package gui;

import data.Auto;
import data.Garaz;

import javax.swing.*;
import java.awt.*;

/**
 * Jak to počítáme: vzorec skóre, aktuální váhy, prahy tříd, odhady a simulace, kde jsou data.
 * Po vzoru stránky „Zdroje a metodika“ v Cesty KHK – každé číslo v aplikaci má jít vysvětlit.
 */
class MetodikaView extends Pohled {

    MetodikaView(MainFrame app) {
        super(app);
    }

    @Override
    String nazev() {
        return "Jak to počítáme";
    }

    @Override
    String ikona() {
        return "book";
    }

    @Override
    String podtitul() {
        return "Metodika, předpoklady a limity. Co je výpočet, co odhad a co simulace.";
    }

    @Override
    void postav() {
        blok(vedle(18, new double[]{1.2, 1}, vzorec(), tridy()));
        blok(vedle(18, new double[]{1, 1}, odhady(), data()));
        blok(zkratky());
    }

    private JComponent vzorec() {
        JPanel o = Ui.pruhledny(new BorderLayout(0, 14));
        Ui.Karta f = new Ui.Karta(new BorderLayout(), 12, 16).vypln(Ui.sunken());
        JLabel v = Ui.label("skóre = 100 × Σ ( váha·hodnota ) / Σ váha", Font.BOLD, 15, Ui.text());
        v.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        f.add(v);
        o.add(f, BorderLayout.NORTH);
        JPanel tab = Ui.pruhledny(new GridLayout(0, 3, 12, 8));
        tab.add(Ui.label("FAKTOR", Font.BOLD, 11, Ui.muted()));
        tab.add(Ui.label("JAK SE POČÍTÁ HODNOTA 0–1", Font.BOLD, 11, Ui.muted()));
        tab.add(Ui.label("VÁHA TEĎ", Font.BOLD, 11, Ui.muted()));
        Color[] pal = Grafy.paletaFaktoru();
        for (Garaz.Faktor fk : Garaz.Faktor.values()) {
            tab.add(Ui.label(fk.getNazev(), Font.BOLD, 13, pal[fk.ordinal()]));
            tab.add(Ui.muted(fk.getPopis(), 12));
            tab.add(Ui.silny(String.valueOf(g.getVaha(fk)), 13));
        }
        o.add(tab, BorderLayout.CENTER);
        o.add(Ui.odstavec("Hodnota každého faktoru je oříznutá na 0 až 1, takže jeden extrém nepřeválcuje ostatní."
                + " Váhy se nemusí sčítat do 100, výsledek se normalizuje. Předvolby jen nastaví váhy.", 12.5f, Ui.muted()),
                BorderLayout.SOUTH);
        return Ui.sekce("Vzorec skóre priority", null, o);
    }

    private JComponent tridy() {
        JPanel o = Ui.pruhledny(new GridLayout(0, 1, 0, 10));
        int k = Math.round(Garaz.PRAH_KRITICKA), v = Math.round(Garaz.PRAH_VYSOKA), s0 = Math.round(Garaz.PRAH_STREDNI);
        String[] prahy = {"skóre " + k + " a víc", v + " až " + (k - 1), s0 + " až " + (v - 1), "pod " + s0};
        String[] co = {"řešit hned, auto může být nebezpečné nebo nelegální",
                "naplánovat v nejbližších týdnech", "hlídat, zatím bez spěchu", "v pořádku"};
        for (Auto.Priorita p : Auto.Priorita.values()) {
            JPanel r = Ui.pruhledny(new BorderLayout(12, 0));
            JPanel s = Ui.pruhledny(new FlowLayout(FlowLayout.LEFT, 0, 0));
            s.add(Ui.stitekPriority(p));
            s.setPreferredSize(new Dimension(92, 24));
            r.add(s, BorderLayout.WEST);
            JPanel t = Ui.pruhledny(new GridLayout(2, 1));
            t.add(Ui.silny(prahy[p.ordinal()], 13));
            t.add(Ui.muted(co[p.ordinal()], 12));
            r.add(t, BorderLayout.CENTER);
            o.add(r);
        }
        o.add(Ui.odstavec("Ručně připnutá třída přebije výpočet. Důvod se zapíše do historie a model ji nepřepíše,"
                + " dokud ji neodepneš – člověk má poslední slovo.", 12.5f, Ui.text2()));
        return Ui.sekce("Třídy priority", null, o);
    }

    private JComponent odhady() {
        JPanel o = Ui.pruhledny(null);
        o.setLayout(new BoxLayout(o, BoxLayout.Y_AXIS));
        o.add(polozka(Ui.odhad("odhad"), "Cena servisu",
                "2 800 Kč + 190 Kč za rok stáří + 0,18 Kč za km od servisu; +1 600 Kč, když do 60 dní vyprší STK;"
                        + " ×1,15 při přetaženém intervalu. Zaokrouhleno na stovky."));
        o.add(polozka(Ui.odhad("odhad"), "Plán do rozpočtu",
                "Auta se berou od nejvyšší priority. Když se další nevejde, přeskočí se a zkusí se levnější."));
        o.add(polozka(Ui.odhad("simulace"), "Odstávka auta",
                "Jízdy převezme auto s nejnižší cenou za km, které samo nemá kritickou prioritu."
                        + " Počítá se jen palivo, ne opotřebení."));
        o.add(polozka(new Ui.Stitek("výpočet", Ui.accent()), "Cena paliva",
                "Benzín " + MainFrame.format(g.getCenaPaliva()) + " Kč/l; nafta ×0,96, LPG ×0,48, hybrid ×1,0."));
        return Ui.sekce("Odhady a simulace", null, o);
    }

    private static JComponent polozka(JComponent stitek, String nadpis, String text) {
        JPanel r = Ui.pruhledny(new BorderLayout(0, 4));
        r.setBorder(new javax.swing.border.EmptyBorder(0, 0, 12, 0));
        r.setAlignmentX(LEFT_ALIGNMENT);
        JPanel h = Ui.pruhledny(new FlowLayout(FlowLayout.LEFT, 8, 0));
        h.add(Ui.silny(nadpis, 13.5f));
        h.add(stitek);
        r.add(h, BorderLayout.NORTH);
        r.add(Ui.odstavec(text, 12.5f, Ui.muted()), BorderLayout.CENTER);
        return r;
    }

    private JComponent data() {
        JPanel o = Ui.pruhledny(null);
        o.setLayout(new BoxLayout(o, BoxLayout.Y_AXIS));
        o.add(polozka(new Ui.Stitek("soubor", Ui.text2()), "Kde jsou data", app.soubor().toString()
                + "\nTextový soubor UTF-8, jeden záznam na řádek. Ukládá se po každé změně a při zavření okna."));
        o.add(polozka(new Ui.Stitek("upozornění", Ui.crit()), "Kdy se ozve zvonek",
                "STK prošlá nebo do 30 dní, servis po intervalu nebo nad 90 %, palivo pod 15 %."));
        o.add(polozka(new Ui.Stitek("export", Ui.ok()), "CSV pro Excel",
                "UTF-8 s BOM, oddělovač středník. Obsahuje skóre, třídu, důvod připnutí, STK a odhad servisu."));
        o.add(polozka(new Ui.Stitek("limity", Ui.muted()), "Co aplikace neví",
                "Skutečné ceny servisu, stav pneumatik ani historii mimo aplikaci. Čísla jsou vodítko, ne posudek."));
        return Ui.sekce("Data a limity", null, o);
    }

    private JComponent zkratky() {
        JPanel o = Ui.pruhledny(new GridLayout(0, 4, 14, 10));
        String[][] z = {{"Ctrl+K", "hledání a příkazy"}, {"Ctrl+N", "přidat auto"}, {"Ctrl+S", "uložit"},
                {"Ctrl+D", "světlý / tmavý režim"}, {"Ctrl+1 – 8", "přepnout pohled"}, {"Enter", "potvrdit dialog"},
                {"Esc", "zavřít dialog"}, {"↑ ↓", "výběr v hledání"}};
        for (String[] x : z) {
            JPanel r = Ui.pruhledny(new FlowLayout(FlowLayout.LEFT, 8, 0));
            r.add(new Ui.Stitek(x[0], Ui.text2()));
            r.add(Ui.muted(x[1], 12.5f));
            o.add(r);
        }
        return Ui.sekce("Klávesové zkratky", null, o);
    }
}
