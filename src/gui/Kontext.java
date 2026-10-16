package gui;

import data.Auto;

import java.awt.Frame;
import java.util.List;

/** Společné rozhraní, přes které nové moduly mluví s hlavním oknem (napojí se v MainFrame). */
interface Kontext {

    /** Hlavní okno (rodič dialogů). */
    Frame okno();

    /** Všechna auta v garáži. */
    List<Auto> auta();

    /** Aktuálně zobrazené auto, nebo null když je garáž prázdná. */
    Auto aktualni();

    /** Cena paliva v Kč za litr. */
    float cenaPaliva();

    /** Uloží celou garáž i nastavení do souboru. */
    void ulozit();

    /** Překreslí hlavní okno podle aktuálních dat. */
    void obnovit();

    /** Krátká zpráva ve stavovém řádku. */
    void status(String text);
}