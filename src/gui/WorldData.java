package gui;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Data světové mapy: zjednodušené obrysy pevnin, moře a přes sto měst. Nepotřebuje internet. */
final class WorldData {

    static final class Mesto {
        final String nazev, zeme, zona, skupina;
        final double lon, lat;
        final int velikost;   // 2 = velkoměsto, 1 = střední, 0 = menší

        Mesto(String nazev, String zeme, String zona, String skupina, double lon, double lat, int velikost) {
            this.nazev = nazev;
            this.zeme = zeme;
            this.zona = zona;
            this.skupina = skupina;
            this.lon = lon;
            this.lat = lat;
            this.velikost = velikost;
        }

        ZoneId zonaId() {
            try {
                return ZoneId.of(zona);
            } catch (RuntimeException e) {
                return ZoneOffset.ofHours((int) Math.round(lon / 15.0));
            }
        }

        @Override
        public String toString() {
            return nazev;
        }
    }

    static final class Cesta {
        final boolean autem;
        final double km;
        final String poznamka;

        Cesta(boolean autem, double km, String poznamka) {
            this.autem = autem;
            this.km = km;
            this.poznamka = poznamka;
        }
    }

    // ------------------------------------------------------------------
    // obrysy pevnin a ostrovů: "délka,šířka délka,šířka ..."
    // ------------------------------------------------------------------

    private static final String[] SUSHI_DATA = {
            // Severní Amerika
            "-168,65.5 -166,68.5 -156,71.3 -141,69.7 -128,70 -115,68.5 -108,68 -96,68.5 -90,69 -82,69.5 "
                    + "-81,67 -86,66 -88,64 -93,62 -94.5,59 -92.5,57 -88,56.5 -82,55 -82,52.5 -79,51.5 -79,54.5 "
                    + "-77,57 -78,60.5 -77.5,62.4 -72,61.5 -69.5,59.5 -65,60.2 -62,57 -61.5,56 -57,53.5 -56,51.5 "
                    + "-60,50.3 -66.5,49.2 -64.5,47.8 -61.5,45.8 -66,44.5 -70,43.7 -70.5,41.8 -74,40.5 -76,37 "
                    + "-75.5,35.2 -78,33.8 -81,31.5 -80,27 -80.5,25.2 -82.5,27.5 -84,30 -88,30.4 -90,29.2 -94,29.5 "
                    + "-97.3,27.5 -97.5,24 -97.8,22 -96,19 -94.5,18.2 -91.5,18.7 -90.5,21 -87,21.5 -87.8,18 "
                    + "-88.3,16 -84,15.9 -83.2,14.2 -83.5,11 -81.5,9 -79.5,9.5 -77.5,8.5 -78,7.2 -80.5,7.3 "
                    + "-82.8,8.2 -85.7,10 -86,11.5 -88,13.2 -91.5,14 -94,16 -96,15.7 -99,16.8 -102,18 -105.5,20.2 "
                    + "-105.5,22.5 -109,25.5 -112.8,29.4 -114.8,31.8 -114.7,32.7 -117.1,32.5 -118.5,34 -120.6,34.6 "
                    + "-122.5,37.5 -124,40.5 -124.5,43 -124,46.2 -124.7,48.4 -127,50.5 -130,54 -133,57 -137,58.5 "
                    + "-141,60 -146,60.8 -150,59.5 -153,57.5 -158,56.5 -162,55 -165,54.6 -162,58.5 -165,60.5 "
                    + "-166,62.5 -161,64.5",
            // Baja California
            "-114.8,32.4 -116.9,32.5 -114.2,28 -112.2,24.8 -110,23 -109.5,23.5 -111.5,26 -113.3,29.5 -114.8,31.8",
            // Baffinův ostrov, Ellesmere, Grónsko
            "-80,73.5 -72,72 -68,70 -62,66.5 -65,63 -71,62.5 -78,64.5 -74,68 -80,70.5",
            "-90,76.5 -76,79.5 -62,82.5 -70,83 -90,81 -92,78",
            "-58,82 -30,83.5 -20,81 -18,76 -22,70.5 -27,68.5 -34,66 -40,65 -42,61 -44,60 -48,61 -52,64.5 "
                    + "-54,67.5 -55,70 -60,75.5 -68,76.5 -73,78 -66,80.5",
            // Kuba, Hispaniola
            "-85,21.9 -82,23.1 -79,22.4 -74.2,20.2 -77.7,19.9 -80.5,22",
            "-74.4,18.4 -72.8,19.9 -68.4,18.6 -70.5,18.2",
            // Jižní Amerika
            "-77.5,8.5 -76,9.5 -75.5,10.8 -72,12 -71.5,11.5 -68,10.6 -64,10.5 -61.5,10.7 -60,8.5 -57,6 -54,5.8 "
                    + "-51.5,4.2 -50,1.8 -48,-1 -44.5,-2.5 -40,-2.8 -35.2,-5.3 -34.8,-8 -37,-12 -38.8,-15 -39.2,-18 "
                    + "-41,-22 -43.2,-23 -46.5,-24 -48.7,-26.5 -48.5,-28.5 -50,-30.5 -52.5,-33.5 -54.5,-34.8 "
                    + "-56,-34.9 -58.4,-34.3 -57.2,-36.5 -62.2,-38.8 -62,-40.8 -65,-41 -64.3,-42.7 -65.5,-45 "
                    + "-67.5,-46.5 -66,-48 -68.5,-50.5 -69,-52.6 -67.5,-54.5 -65.5,-55 -68.5,-55.5 -71.5,-54 "
                    + "-74.5,-52 -75.5,-48.5 -73.8,-44 -73.5,-40 -73.7,-37 -72,-33 -71.5,-30 -70.5,-25 -70.2,-20 "
                    + "-71.5,-17.5 -75,-15.5 -78,-12 -79.5,-8 -81.2,-5.5 -80,-3 -80.8,-1 -80,0.8 -78.8,1.8 -77.5,4 "
                    + "-77.3,6.5",
            // Evropa a Asie
            "-5.6,36 -9,37 -9.5,39 -8.8,42 -9.3,43.1 -2,43.5 -1.5,46 -4.5,48.3 -1,49.4 1.5,50.5 2.5,51.1 "
                    + "4.5,52.5 7,53.5 8.6,54 8.2,56.8 10.6,57.7 10.2,55 10.5,54.3 14,54 18.5,54.7 21,55.5 21,57 "
                    + "23,57.3 24.3,58 24,59.4 28,59.6 30,60 26.5,60.5 23,60 21.5,61.5 21.5,63 25,65 25.5,65.5 "
                    + "22,65.8 21,64 18,62.5 17,61 19,59.8 16.5,57.5 14,55.4 12.8,56.2 11,59 8,58 5.5,58.8 5,61 "
                    + "5,62.3 8,63.5 11,64.8 13,66.5 15,68.5 18,69.8 21,70.2 25.8,71.1 30,70.3 33,69.4 37,68.5 "
                    + "41,67.2 44,68.2 47,68 53,68.5 60,69 66,69.5 70,73 75,72.5 80,73.5 87,75 95,76 100,77.7 "
                    + "105,77.5 112,76 113,73.7 120,73 128,72.3 135,71.5 140,72.5 150,71.5 160,70 170,70 180,68.9 "
                    + "180,65 177,62.5 170,60.5 164,59.8 163,56 162,54.5 159.5,53 156.8,51 155.7,55.5 156,57.5 "
                    + "159.5,61.5 155,59.5 150,59.5 143,59.3 137,54.5 141,53 140.3,48 135,43.5 131,42.5 129.7,41 "
                    + "129.5,37 129.2,35.3 126.5,34.5 126.2,36.7 125,38.5 124.5,39.8 121.5,40.7 121.5,39 119.5,39.2 "
                    + "118,39 117.8,38.3 119,37.2 122.5,37.2 120,35.5 120.8,33 121.9,31.5 121.5,30.5 122,29.5 "
                    + "120,27 119,25 117,23.3 114.3,22.3 110.5,21 108,21.5 106.7,20.2 105.8,19 108.7,15.5 109.3,12 "
                    + "107.5,10.5 105,8.6 105,10 103,10.7 100.5,13.3 99.2,10.5 100.3,8 101,6.5 103.4,4.5 104.2,1.4 "
                    + "103.5,1.3 101,3 100.3,5.5 100.4,6.4 98.3,8 98.5,10.5 98.6,13.5 97.7,16.5 94.4,16.2 94.2,18.5 "
                    + "92.5,20.5 91.9,22.5 90.5,22 88.5,21.7 86.5,20 84.5,19 82.2,16.5 80.2,15.5 80.2,13 79.9,10.3 "
                    + "78.2,8.7 77.5,8.1 76.2,9.5 74.8,12.5 73.4,16 72.8,19 72.7,21.5 70.5,20.8 69.5,22.3 68.7,23.1 "
                    + "67.2,24.8 66.5,25.4 62.5,25.2 58,25.5 57.3,26.5 56.5,27.2 54.5,26.5 51.5,27.9 50.2,30 "
                    + "48.5,29.9 47.8,29.2 48.4,28.5 50.5,26 51.6,25.3 54,24.2 56.3,26.3 56.6,24.5 58.7,23.5 "
                    + "59.8,22.5 58.5,20.5 55.5,17.5 52.2,16 49,14.2 45,12.8 43.4,12.7 42.7,15.5 41.3,18.8 "
                    + "39.5,21.5 38.8,24 37,26.2 35,28.2 34.9,29.5 34.2,27.7 32.6,29.9 34.2,31.3 35.1,33 35.9,35 "
                    + "36,36.6 34,36.2 32,36.2 29,36.4 27.5,37 26.3,38.5 26.8,40 26.2,40.9 24,40.8 23.7,40.2 "
                    + "22.9,40.5 22.8,39.2 24,38 23,36.5 21.7,36.8 21.2,38.3 20.3,39.5 19.4,40.4 19.5,41.8 "
                    + "18.5,42.5 16.5,43.5 15,44.5 13.7,45.2 12.3,45.3 12.5,44 14,42.2 16,41.9 18.5,40.2 16.6,39.5 "
                    + "17.1,39 15.7,38 15.8,40 14,40.8 12.5,41.5 11,42.5 10.2,43.9 8.8,44.4 7.3,43.8 5,43.3 "
                    + "3.3,43.2 3.1,42 0.9,41 -0.3,39.5 0,38.7 -0.7,37.6 -2.2,36.7 -4.5,36.7",
            // Čukotka za datovou hranicí
            "-180,68.9 -175,67.5 -170,66 -170,65.6 -172,64.5 -178,64.7 -180,65",
            // Afrika
            "-17.5,14.7 -16.5,19 -16,23 -13,27.5 -9.8,29.5 -8.5,33.5 -6,35.8 -2,35.2 3,36.8 10,37.2 11,35 "
                    + "10,33.5 15,32.3 19,30.4 20,32 23,32.8 30,31.3 32.5,31.2 32.6,29.9 33,28 35.5,23.5 37.3,19 "
                    + "39,15.5 41,13.5 43.2,12.4 44.5,10.8 48,11.2 51.2,11.8 51,10 49.5,6.5 46,2.5 43,-1 41,-2.5 "
                    + "39.5,-5 39,-8 40.5,-11 40.6,-15 36.8,-18 35.3,-22.5 35.5,-24 32.8,-26 32.5,-28.8 30,-31.5 "
                    + "27,-33.5 22.5,-34 19,-34.8 18.4,-34 17.8,-31 15.2,-27 14.5,-22.5 12,-18 11.8,-15 13.5,-11.5 "
                    + "13.2,-8.8 12.2,-6 11.8,-4.5 9.5,-1 9.5,1.5 9.8,3.9 8.5,4.5 5.8,4.3 2,6.3 -1.5,5 -4.8,5.2 "
                    + "-7.8,4.4 -11,6.7 -13.3,9 -15.5,11.5 -17.4,14.7",
            // Madagaskar
            "49.3,-12 50.5,-15.5 49.5,-18 47.5,-24.5 45,-25.5 43.5,-22.5 44.2,-19 46,-15.8 47.8,-14",
            // Británie, Irsko, Island
            "-5.7,50.1 1.3,51.1 1.7,52.7 0.2,53.5 -0.2,54.5 -1.6,55.7 -1.8,57.6 -3.2,58.6 -5,58.6 -6.2,57.5 "
                    + "-5.6,56 -4.9,55.2 -3.3,54.8 -3,53.5 -4.5,53.3 -5.2,51.7 -3.5,51.4",
            "-10,51.8 -6,52.2 -6,54.1 -5.5,54.7 -7.5,55.2 -10,54.2",
            "-24,65.5 -22,66.4 -16,66.5 -13.5,65 -15,64.2 -19,63.4 -22.7,63.8",
            // Japonsko, Sachalin, Tchaj-wan, Chaj-nan, Srí Lanka
            "130.9,34.2 132.5,35.4 135.3,35.7 136.7,37.3 139,38 140,40.5 141.5,41.4 141.9,39.5 141,37 "
                    + "140.9,35.7 139.8,35 138.5,34.6 136.9,34.7 135.3,33.6 135.1,34.3 133,34.3 131,33.9",
            "140,41.5 141.8,45.4 145.4,43.4 143,42 140.2,41.9",
            "129.8,33.5 131.8,33.8 131.5,31.4 130.2,31.2",
            "142,54 143.5,51 143.2,47 142,46 142,49",
            "121.5,25.2 121.9,24.5 120.9,22 120.2,23",
            "108.7,19.2 110.5,20.1 111,19.6 109.5,18.2",
            "79.8,9.7 81.8,7.5 80.6,6 79.8,7.5",
            // Filipíny
            "120.3,18.5 122.2,18.4 122,16.5 121.5,14.2 124,13 122,13.5 120.5,14.5",
            "122,7 124,8.5 126.5,7.5 126,6 125,5.9 123.5,7.7",
            // Indonésie: Sumatra, Jáva, Borneo, Celebes, Nová Guinea
            "95.3,5.6 98,4 100.5,2 104,-1.5 106,-3 105.8,-5.8 102,-4 99,-1.5 95.5,3",
            "105.2,-6.8 106.8,-6 110.5,-6.9 114.4,-7.8 114.5,-8.6 111,-8.3 106.5,-7.4",
            "109,1.8 111.2,1.8 113.5,3.3 115.5,5.2 117.2,6.9 119,5.3 117.8,3.5 118.2,1 117.5,-0.8 116.5,-3 "
                    + "116,-4 114,-3.6 111.8,-3.4 110,-2.5 109,-0.5",
            "119.8,0.5 120.5,1.3 124.8,1.6 122.5,0.3 121.5,-1 123,-1.2 123.2,-4 122,-4.8 121,-3 121.5,-5 "
                    + "120.3,-5.6 119.4,-5.5 119.6,-3 118.9,-2.8",
            "131,-0.8 134,-0.9 138,-1.6 141,-2.7 145.5,-5.3 147.5,-6 150.5,-10.5 147,-9.5 144,-7.8 141,-9.1 "
                    + "138,-8.3 137.5,-5 135,-4.5 133,-4 132,-2.9",
            // Austrálie, Tasmánie, Nový Zéland
            "113.5,-22 114.2,-26.5 115,-30 115,-34.2 118,-35 123,-34 126,-32.3 130,-31.5 134,-32.8 135.6,-34.8 "
                    + "138,-35.5 138,-34 140,-37.9 141,-38.3 144,-38.5 146.3,-39 148,-37.8 150,-37.2 151.3,-33.8 "
                    + "153,-31 153.6,-28.5 153,-25.5 150.8,-23 149,-21 146.3,-19 145.5,-15 143.5,-14 142.5,-10.7 "
                    + "141.5,-13 141.6,-17 139.5,-17.5 136.5,-15.5 135.5,-12 132.5,-11.5 130.8,-12.4 129.5,-14.8 "
                    + "127.5,-14 125.5,-14.5 122.2,-17.5 120,-19.8 117,-20.7",
            "144.7,-40.7 148.3,-40.9 147.9,-43 146.2,-43.5",
            "172.7,-34.5 175.5,-37 178.5,-37.7 177,-39.2 175.2,-41.5 174.8,-39.9 173.8,-39.2 174.5,-37.1",
            "172.7,-40.5 174.3,-41.7 173,-43.8 171.2,-44.5 170.5,-46.2 168.3,-46.6 166.6,-45.8 168.2,-44 170.8,-42.7",
            // Antarktida (jen pruh)
            "-180,-90 -180,-72 -150,-76 -120,-74 -90,-72 -60,-65 -50,-72 -20,-72 0,-70 30,-69 60,-67 90,-66 "
                    + "120,-66 150,-68 170,-72 180,-72 180,-90"
    };

    /** Vnitrozemská moře, která se kreslí barvou oceánu přes pevninu. */
    private static final String[] MORE_DATA = {
            "27.8,41.9 28.7,43.5 30,45.5 31.5,46.6 33,46 34.5,45.2 37,47 39,47.2 38,45 40,43.5 41.6,41.5 "
                    + "36,41.3 31,41.2 28.9,41.2",
            "47,45 49,46.5 53,46.8 53,45 51,44 53,41 54,37 51,36.7 49,37.6 49.5,40.2 47.5,42.5"
    };

    /** Názvy světadílů: text, délka, šířka. */
    static final Object[][] SVETADILY = {
            {"EVROPA", 15.0, 52.0}, {"ASIE", 90.0, 50.0}, {"AFRIKA", 20.0, 5.0},
            {"SEVERNÍ AMERIKA", -100.0, 46.0}, {"JIŽNÍ AMERIKA", -60.0, -15.0},
            {"AUSTRÁLIE", 134.0, -25.0}, {"ANTARKTIDA", 20.0, -80.0}
    };

    // ------------------------------------------------------------------
    // města: název | země | časové pásmo | skupina (kde se dá dojet autem) | délka | šířka | velikost
    // ------------------------------------------------------------------

    private static final String[] MESTA_DATA = {
            "Praha|Česko|Europe/Prague|EA|14.42|50.08|2", "Brno|Česko|Europe/Prague|EA|16.61|49.20|1",
            "Berlín|Německo|Europe/Berlin|EA|13.40|52.52|2", "Mnichov|Německo|Europe/Berlin|EA|11.58|48.14|1",
            "Vídeň|Rakousko|Europe/Vienna|EA|16.37|48.21|2", "Varšava|Polsko|Europe/Warsaw|EA|21.01|52.23|2",
            "Budapešť|Maďarsko|Europe/Budapest|EA|19.04|47.50|2",
            "Bratislava|Slovensko|Europe/Bratislava|EA|17.11|48.15|1",
            "Paříž|Francie|Europe/Paris|EA|2.35|48.86|2",
            "Londýn|Spojené království|Europe/London|EA|-0.13|51.51|2",
            "Dublin|Irsko|Europe/Dublin|EA|-6.26|53.35|1", "Madrid|Španělsko|Europe/Madrid|EA|-3.70|40.42|2",
            "Barcelona|Španělsko|Europe/Madrid|EA|2.17|41.39|1", "Lisabon|Portugalsko|Europe/Lisbon|EA|-9.14|38.72|1",
            "Řím|Itálie|Europe/Rome|EA|12.50|41.90|2", "Milán|Itálie|Europe/Rome|EA|9.19|45.46|1",
            "Atény|Řecko|Europe/Athens|EA|23.73|37.98|1", "Istanbul|Turecko|Europe/Istanbul|EA|28.98|41.01|2",
            "Moskva|Rusko|Europe/Moscow|EA|37.62|55.75|2", "Petrohrad|Rusko|Europe/Moscow|EA|30.34|59.93|1",
            "Kyjev|Ukrajina|Europe/Kiev|EA|30.52|50.45|1", "Stockholm|Švédsko|Europe/Stockholm|EA|18.07|59.33|1",
            "Oslo|Norsko|Europe/Oslo|EA|10.75|59.91|1", "Helsinky|Finsko|Europe/Helsinki|EA|24.94|60.17|1",
            "Kodaň|Dánsko|Europe/Copenhagen|EA|12.57|55.68|1", "Amsterdam|Nizozemsko|Europe/Amsterdam|EA|4.90|52.37|1",
            "Brusel|Belgie|Europe/Brussels|EA|4.35|50.85|1", "Curych|Švýcarsko|Europe/Zurich|EA|8.54|47.38|1",
            "Bukurešť|Rumunsko|Europe/Bucharest|EA|26.10|44.43|1", "Sofie|Bulharsko|Europe/Sofia|EA|23.32|42.70|0",
            "Bělehrad|Srbsko|Europe/Belgrade|EA|20.46|44.82|0", "Záhřeb|Chorvatsko|Europe/Zagreb|EA|15.98|45.81|0",
            "Reykjavik|Island|Atlantic/Reykjavik|IS|-21.94|64.15|1",
            "Ankara|Turecko|Europe/Istanbul|EA|32.85|39.93|1", "Teherán|Írán|Asia/Tehran|EA|51.39|35.69|2",
            "Dubaj|SAE|Asia/Dubai|EA|55.27|25.20|2", "Rijád|Saúdská Arábie|Asia/Riyadh|EA|46.68|24.71|1",
            "Bagdád|Irák|Asia/Baghdad|EA|44.36|33.31|1", "Jeruzalém|Izrael|Asia/Jerusalem|EA|35.22|31.77|1",
            "Taškent|Uzbekistán|Asia/Tashkent|EA|69.24|41.30|1", "Dillí|Indie|Asia/Kolkata|EA|77.21|28.61|2",
            "Bombaj|Indie|Asia/Kolkata|EA|72.88|19.08|2", "Kalkata|Indie|Asia/Kolkata|EA|88.36|22.57|1",
            "Bangalore|Indie|Asia/Kolkata|EA|77.59|12.97|1", "Dháka|Bangladéš|Asia/Dhaka|EA|90.41|23.81|1",
            "Karáčí|Pákistán|Asia/Karachi|EA|67.01|24.86|1", "Káthmándú|Nepál|Asia/Kathmandu|EA|85.32|27.72|0",
            "Bangkok|Thajsko|Asia/Bangkok|EA|100.50|13.76|2", "Hanoj|Vietnam|Asia/Bangkok|EA|105.85|21.03|1",
            "Ho Či Minovo Město|Vietnam|Asia/Ho_Chi_Minh|EA|106.63|10.82|1",
            "Kuala Lumpur|Malajsie|Asia/Kuala_Lumpur|EA|101.69|3.14|1",
            "Singapur|Singapur|Asia/Singapore|EA|103.82|1.35|1", "Peking|Čína|Asia/Shanghai|EA|116.41|39.90|2",
            "Šanghaj|Čína|Asia/Shanghai|EA|121.47|31.23|2", "Hongkong|Čína|Asia/Hong_Kong|EA|114.17|22.32|1",
            "Čcheng-tu|Čína|Asia/Shanghai|EA|104.07|30.57|1", "Ulánbátar|Mongolsko|Asia/Ulaanbaatar|EA|106.92|47.89|0",
            "Almaty|Kazachstán|Asia/Almaty|EA|76.95|43.24|1", "Novosibirsk|Rusko|Asia/Novosibirsk|EA|82.92|55.03|1",
            "Irkutsk|Rusko|Asia/Irkutsk|EA|104.28|52.29|0", "Jakutsk|Rusko|Asia/Yakutsk|EA|129.73|62.03|0",
            "Vladivostok|Rusko|Asia/Vladivostok|EA|131.89|43.12|1", "Soul|Jižní Korea|Asia/Seoul|KR|126.98|37.57|2",
            "Tokio|Japonsko|Asia/Tokyo|JP|139.69|35.69|2", "Ósaka|Japonsko|Asia/Tokyo|JP|135.50|34.69|1",
            "Taipej|Tchaj-wan|Asia/Taipei|TW|121.56|25.03|1", "Manila|Filipíny|Asia/Manila|PH|120.98|14.60|2",
            "Jakarta|Indonesie|Asia/Jakarta|ID|106.85|-6.21|2", "Kolombo|Srí Lanka|Asia/Colombo|LK|79.86|6.93|1",
            "Káhira|Egypt|Africa/Cairo|EA|31.24|30.04|2", "Lagos|Nigérie|Africa/Lagos|EA|3.38|6.52|2",
            "Nairobi|Keňa|Africa/Nairobi|EA|36.82|-1.29|1", "Johannesburg|JAR|Africa/Johannesburg|EA|28.05|-26.20|2",
            "Kapské Město|JAR|Africa/Johannesburg|EA|18.42|-33.92|1",
            "Casablanca|Maroko|Africa/Casablanca|EA|-7.59|33.57|1",
            "Addis Abeba|Etiopie|Africa/Addis_Ababa|EA|38.75|9.03|1",
            "Kinshasa|DR Kongo|Africa/Kinshasa|EA|15.27|-4.44|1",
            "Dar es Salaam|Tanzanie|Africa/Dar_es_Salaam|EA|39.27|-6.79|1",
            "Alžír|Alžírsko|Africa/Algiers|EA|3.06|36.75|1", "Tunis|Tunisko|Africa/Tunis|EA|10.18|36.81|0",
            "Accra|Ghana|Africa/Accra|EA|-0.19|5.60|1", "Dakar|Senegal|Africa/Dakar|EA|-17.47|14.69|1",
            "Luanda|Angola|Africa/Luanda|EA|13.23|-8.84|1", "Chartúm|Súdán|Africa/Khartoum|EA|32.53|15.50|1",
            "Antananarivo|Madagaskar|Indian/Antananarivo|MG|47.51|-18.88|0",
            "New York|USA|America/New_York|AM|-74.01|40.71|2", "Washington|USA|America/New_York|AM|-77.04|38.91|1",
            "Miami|USA|America/New_York|AM|-80.19|25.76|1", "Chicago|USA|America/Chicago|AM|-87.63|41.88|2",
            "Houston|USA|America/Chicago|AM|-95.37|29.76|1", "Los Angeles|USA|America/Los_Angeles|AM|-118.24|34.05|2",
            "San Francisco|USA|America/Los_Angeles|AM|-122.42|37.77|1",
            "Anchorage|USA|America/Anchorage|AM|-149.90|61.22|0", "Honolulu|USA|Pacific/Honolulu|HI|-157.86|21.31|1",
            "Toronto|Kanada|America/Toronto|AM|-79.38|43.65|2", "Montréal|Kanada|America/Toronto|AM|-73.57|45.50|1",
            "Vancouver|Kanada|America/Vancouver|AM|-123.12|49.28|1",
            "Mexiko|Mexiko|America/Mexico_City|AM|-99.13|19.43|2", "Havana|Kuba|America/Havana|CU|-82.37|23.11|1",
            "Panama|Panama|America/Panama|AM|-79.52|8.98|1", "Bogotá|Kolumbie|America/Bogota|SAM|-74.07|4.71|2",
            "Caracas|Venezuela|America/Caracas|SAM|-66.90|10.48|1", "Quito|Ekvádor|America/Guayaquil|SAM|-78.47|-0.18|1",
            "Lima|Peru|America/Lima|SAM|-77.04|-12.05|2", "La Paz|Bolívie|America/La_Paz|SAM|-68.15|-16.50|1",
            "Santiago|Chile|America/Santiago|SAM|-70.65|-33.45|1",
            "Buenos Aires|Argentina|America/Argentina/Buenos_Aires|SAM|-58.38|-34.60|2",
            "São Paulo|Brazílie|America/Sao_Paulo|SAM|-46.63|-23.55|2",
            "Rio de Janeiro|Brazílie|America/Sao_Paulo|SAM|-43.17|-22.91|1",
            "Brasília|Brazílie|America/Sao_Paulo|SAM|-47.93|-15.78|1",
            "Ushuaia|Argentina|America/Argentina/Buenos_Aires|SAM|-68.30|-54.80|0",
            "Sydney|Austrálie|Australia/Sydney|AU|151.21|-33.87|2", "Melbourne|Austrálie|Australia/Melbourne|AU|144.96|-37.81|2",
            "Brisbane|Austrálie|Australia/Brisbane|AU|153.03|-27.47|1", "Perth|Austrálie|Australia/Perth|AU|115.86|-31.95|1",
            "Adelaide|Austrálie|Australia/Adelaide|AU|138.60|-34.93|0", "Darwin|Austrálie|Australia/Darwin|AU|130.84|-12.46|0",
            "Auckland|Nový Zéland|Pacific/Auckland|NZ|174.76|-36.85|1", "Wellington|Nový Zéland|Pacific/Auckland|NZ|174.78|-41.29|0",
            "Port Moresby|Papua-Nová Guinea|Pacific/Port_Moresby|PG|147.18|-9.44|0"
    };

    static final List<double[][]> SUSHI = new ArrayList<>();
    static final List<double[][]> MORE = new ArrayList<>();
    static final List<Mesto> MESTA = new ArrayList<>();

    static {
        for (String s : SUSHI_DATA) SUSHI.add(parse(s));
        for (String s : MORE_DATA) MORE.add(parse(s));
        for (String r : MESTA_DATA) {
            String[] p = r.split("\\|");
            MESTA.add(new Mesto(p[0], p[1], p[2], p[3], Double.parseDouble(p[4]),
                    Double.parseDouble(p[5]), Integer.parseInt(p[6])));
        }
    }

    private WorldData() {
    }

    private static double[][] parse(String s) {
        String[] p = s.trim().split("\\s+");
        double[][] r = new double[p.length][2];
        for (int i = 0; i < p.length; i++) {
            String[] c = p[i].split(",");
            r[i][0] = Double.parseDouble(c[0]);
            r[i][1] = Double.parseDouble(c[1]);
        }
        return r;
    }

    // ------------------------------------------------------------------
    // zeměpis
    // ------------------------------------------------------------------

    /** Vzdálenost po povrchu zeměkoule v km (haversine). */
    static double vzdalenostKm(double lon1, double lat1, double lon2, double lat2) {
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * 6371.0 * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    static double vzdalenostKm(Mesto a, Mesto b) {
        return vzdalenostKm(a.lon, a.lat, b.lon, b.lat);
    }

    /** Počáteční azimut (0 = sever, 90 = východ) z prvního bodu k druhému. */
    static double azimut(Mesto a, Mesto b) {
        double f1 = Math.toRadians(a.lat), f2 = Math.toRadians(b.lat), dl = Math.toRadians(b.lon - a.lon);
        double y = Math.sin(dl) * Math.cos(f2);
        double x = Math.cos(f1) * Math.sin(f2) - Math.sin(f1) * Math.cos(f2) * Math.cos(dl);
        return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0;
    }

    static String svetovaStrana(double azimut) {
        String[] s = {"sever", "severovýchod", "východ", "jihovýchod", "jih", "jihozápad", "západ", "severozápad"};
        return s[(int) Math.round(azimut / 45.0) % 8];
    }

    /** Body na oblouku nejkratší cesty (velký kruh): pole {délka, šířka}, n + 1 bodů. */
    static double[][] oblouk(Mesto a, Mesto b, int n) {
        double[] va = vektor(a.lon, a.lat), vb = vektor(b.lon, b.lat);
        double dot = Math.max(-1.0, Math.min(1.0, va[0] * vb[0] + va[1] * vb[1] + va[2] * vb[2]));
        double om = Math.acos(dot);
        double[][] r = new double[n + 1][2];
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            if (om < 1e-9 || Math.sin(om) < 1e-9) {
                r[i][0] = a.lon + (b.lon - a.lon) * t;
                r[i][1] = a.lat + (b.lat - a.lat) * t;
                continue;
            }
            double s1 = Math.sin((1 - t) * om) / Math.sin(om), s2 = Math.sin(t * om) / Math.sin(om);
            double x = s1 * va[0] + s2 * vb[0], y = s1 * va[1] + s2 * vb[1], z = s1 * va[2] + s2 * vb[2];
            r[i][0] = Math.toDegrees(Math.atan2(y, x));
            r[i][1] = Math.toDegrees(Math.atan2(z, Math.hypot(x, y)));
        }
        return r;
    }

    private static double[] vektor(double lon, double lat) {
        double lo = Math.toRadians(lon), la = Math.toRadians(lat);
        return new double[]{Math.cos(la) * Math.cos(lo), Math.cos(la) * Math.sin(lo), Math.sin(la)};
    }

    private static boolean britsky(Mesto m) {
        return m.zeme.equals("Spojené království") || m.zeme.equals("Irsko");
    }

    /** Odhad, jestli a jak daleko se mezi městy dá dojet autem. */
    static Cesta cesta(Mesto a, Mesto b) {
        double gc = vzdalenostKm(a, b);
        boolean ameriky = (a.skupina.equals("AM") && b.skupina.equals("SAM"))
                || (a.skupina.equals("SAM") && b.skupina.equals("AM"));
        if (a.skupina.equals(b.skupina) || ameriky) {
            double km = gc * (gc > 3000 ? 1.22 : 1.3);
            StringBuilder p = new StringBuilder();
            if (ameriky) {
                p.append("Mezi Panamou a Kolumbií silnice nevede (Darién), je potřeba loď nebo letadlo. ");
            }
            if (britsky(a) != britsky(b)) {
                p.append("Cestou je potřeba tunel pod Lamanšem nebo trajekt. ");
            }
            if (km > 6000) {
                p.append("Skutečná trasa může být delší kvůli terénu a hranicím. ");
            }
            return new Cesta(true, km, p.toString().trim());
        }
        return new Cesta(false, gc, "Mezi místy je moře, autem se tam nedá dojet.");
    }
}