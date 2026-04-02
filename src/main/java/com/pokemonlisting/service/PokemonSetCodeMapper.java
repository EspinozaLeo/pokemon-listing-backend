package com.pokemonlisting.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class PokemonSetCodeMapper {

    private final Map<String, String> codeToTcgdexId;
    private final Map<String, String> totalCardsMapping;

    public PokemonSetCodeMapper() {
        codeToTcgdexId = new HashMap<>();
        totalCardsMapping = new HashMap<>();
        initializeSetMappings();
        initializeTotalCardsMapping();
    }

    /**
     * toTcgdexId(setCode) maps a set code from card to TCGdex API set ID.
     * Returns the TCGDex set code equivalent.
     *
     * @param setCode The code found on card (e.g., "SV1", "MEW", "PAR")
     * @return TCGdex set ID (e.g., "sv01", "sv03.5"), or null if not found
     */
    public String toTcgdexId(String setCode) {
        if (setCode == null || setCode.isEmpty()) {
            return null;
        }

        String normalized = setCode.trim().toUpperCase();
        return codeToTcgdexId.get(normalized);
    }

    /**
     * toTcgdexIdByTotal(totalCards, year) maps using denominator (total cards
     * shown on card) + copyright year. This is the fallback for ambiguous
     * or mangled set codes.
     *
     * @param totalCards The denominator from card (e.g., "091" from "074/091")
     * @param year Copyright year (e.g., "2024")
     * @return TCGdex set ID, or null if not found
     */
    public String toTcgdexIdByTotal(String totalCards, String year) {
        if (totalCards == null || year == null) {
            return null;
        }

        // Normalize: remove leading zeros from totalCards
        int total = Integer.parseInt(totalCards);
        String key = total + ":" + year;

        return totalCardsMapping.get(key);
    }

    /**
     * isAmbiguous(setCode) check if a set code is ambiguous (looks similar
     * to other codes). Ambiguous codes should use total+year fallback for safety.
     *
     * @param setCode The code to check
     * @return true if ambiguous (e.g., PAL/PAR/PAF can be confused)
     */
    public boolean isAmbiguous(String setCode) {
        if (setCode == null || setCode.isEmpty()) {
            return false;
        }

        String normalized = setCode.trim().toUpperCase();

        // PAL, PAR, PAF can all be confused with each other
        // (L↔R↔F, P↔F, A is constant)
        return normalized.matches("PA[LRFIEK]") ||  // PAL, PAR, PAF, PAI, PAE, PAK
                normalized.matches("[FRH]A[LRFP]");   // FAL, FAR, FAF, HAR, etc.
    }

    /**
     * hasMapping(setCode) checks if a set code is recognized.
     *
     * @param setCode The code to check
     * @return true if we have a mapping
     */
    public boolean hasMapping(String setCode) {
        if (setCode == null || setCode.isEmpty()) {
            return false;
        }
        return codeToTcgdexId.containsKey(setCode.trim().toUpperCase());
    }

    /**
     * initializeSetMappings() initialize the set code to TCGdex ID mappings.
     * ONLY include safe, unambiguous variations.
     */
    private void initializeSetMappings() {
        // NOTE: Only Scarlet & Violet era and later print set codes on card fronts.
        // Sword & Shield and older sets do not — use totalCardsMapping fallback instead.

        // --- SCARLET & VIOLET ERA ---
        // Scarlet & Violet base - sv01
        codeToTcgdexId.put("SVI", "sv01");
        codeToTcgdexId.put("SVL", "sv01");  // OCR variation

        // Paldea Evolved - sv02
        // ONLY exact match - too risky to add variations (conflicts with PAR/PAF)
        codeToTcgdexId.put("PAL", "sv02");

        // Obsidian Flames - sv03
        codeToTcgdexId.put("OBF", "sv03");
        codeToTcgdexId.put("OBE", "sv03");  // OCR variation
        codeToTcgdexId.put("DBF", "sv03");  // OCR variation
        codeToTcgdexId.put("ODF", "sv03");  // OCR variation
        codeToTcgdexId.put("OBP", "sv03");  // OCR variation

        // 151 - sv03.5
        codeToTcgdexId.put("MEW", "sv03.5");
        codeToTcgdexId.put("NEW", "sv03.5");  // OCR variation
        codeToTcgdexId.put("MFW", "sv03.5");  // OCR variation
        codeToTcgdexId.put("NFW", "sv03.5");  // OCR variation
        codeToTcgdexId.put("MEM", "sv03.5");  // OCR variation

        // Paradox Rift - sv04
        // ONLY exact match - too risky to add variations (conflicts with PAL/PAF)
        codeToTcgdexId.put("PAR", "sv04");

        // Paldean Fates - sv04.5
        // ONLY exact match - too risky to add variations (conflicts with PAL/PAR)
        codeToTcgdexId.put("PAF", "sv04.5");

        // Temporal Forces - sv05
        codeToTcgdexId.put("TEF", "sv05");

        // Twilight Masquerade - sv06
        codeToTcgdexId.put("TWM", "sv06");

        // Shrouded Fable - sv06.5
        codeToTcgdexId.put("SFA", "sv06.5");

        // Stellar Crown - sv07
        codeToTcgdexId.put("SCR", "sv07");

        // Surging Sparks - sv08
        codeToTcgdexId.put("SSP", "sv08");

        // Prismatic Evolutions - sv08.5
        codeToTcgdexId.put("PRE", "sv08.5");

        // Journey Together - sv09
        codeToTcgdexId.put("JTG", "sv09");

        // Destined Rivals - sv10
        codeToTcgdexId.put("DRI", "sv10");

        // Black Bolt - sv10.5b
        codeToTcgdexId.put("BLK", "sv10.5b");

        // White Flare - sv10.5w
        codeToTcgdexId.put("WHT", "sv10.5w");
    }

    /**
     * initializeTotalCardsMapping() initialize total cards (denominator) +
     * year mapping. This is the reliable fallback when set codes are
     * ambiguous or mangled.
     */
    private void initializeTotalCardsMapping() {
        //Format: "denominator:year" → tcgdexId
        // The denominator is what appears on cards (e.g., "091" in "074/091")

        // --- BASE ERA (1999-2000) ---
        // Base Set - xxx/102
        totalCardsMapping.put("102:1999", "base1");
        // Jungle - xxx/64
        totalCardsMapping.put("64:1999", "base2");
        // Fossil - xxx/62
        totalCardsMapping.put("62:1999", "base3");
        // Base Set 2 - xxx/130
        totalCardsMapping.put("130:2000", "base4");
        // Team Rocket - xxx/82
        totalCardsMapping.put("82:2000", "base5");
        // Gym Heroes - xxx/132 (NOTE: conflicts with Gym Challenge — keep Heroes, skip Challenge)
        totalCardsMapping.put("132:2000", "gym1");
        // Gym Challenge skipped — same 132:2000 as Gym Heroes

        // --- NEO ERA (2000-2002) ---
        // Neo Genesis - xxx/111
        totalCardsMapping.put("111:2000", "neo1");
        // Neo Discovery - xxx/75
        totalCardsMapping.put("75:2001", "neo2");
        // Neo Revelation - xxx/64
        totalCardsMapping.put("64:2001", "neo3");
        // Neo Destiny - xxx/105
        totalCardsMapping.put("105:2002", "neo4");
        // Legendary Collection - xxx/110
        totalCardsMapping.put("110:2002", "lc");

        // --- E-CARD ERA (2002-2003) ---
        // Expedition Base Set - xxx/165
        totalCardsMapping.put("165:2002", "ecard1");
        // Aquapolis - xxx/147
        totalCardsMapping.put("147:2003", "ecard2");
        // Skyridge - xxx/144
        totalCardsMapping.put("144:2003", "ecard3");

        // --- EX ERA (2003-2007) ---
        // Ruby & Sapphire - xxx/109
        totalCardsMapping.put("109:2003", "ex1");
        // Sandstorm - xxx/100
        totalCardsMapping.put("100:2003", "ex2");
        // Dragon - xxx/97
        totalCardsMapping.put("97:2004", "ex3");
        // Team Magma vs Team Aqua - xxx/95
        totalCardsMapping.put("95:2004", "ex4");
        // Hidden Legends - xxx/101
        totalCardsMapping.put("101:2004", "ex5");
        // FireRed & LeafGreen - xxx/112
        totalCardsMapping.put("112:2004", "ex6");
        // Team Rocket Returns - xxx/109
        totalCardsMapping.put("109:2004", "ex7");
        // Deoxys - xxx/107
        totalCardsMapping.put("107:2005", "ex8");
        // Emerald - xxx/106
        totalCardsMapping.put("106:2005", "ex9");
        // Unseen Forces - xxx/115
        totalCardsMapping.put("115:2005", "ex10");
        // Delta Species - xxx/113
        totalCardsMapping.put("113:2005", "ex11");
        // Legend Maker - xxx/92
        totalCardsMapping.put("92:2006", "ex12");
        // Holon Phantoms - xxx/110
        totalCardsMapping.put("110:2006", "ex13");
        // Crystal Guardians - xxx/100
        totalCardsMapping.put("100:2006", "ex14");
        // Dragon Frontiers - xxx/101
        totalCardsMapping.put("101:2006", "ex15");
        // Power Keepers - xxx/108
        totalCardsMapping.put("108:2007", "ex16");

        // --- DIAMOND & PEARL ERA (2007-2009) ---
        // Diamond & Pearl - xxx/130
        totalCardsMapping.put("130:2007", "dp1");
        // Mysterious Treasures - xxx/122
        totalCardsMapping.put("122:2007", "dp2");
        // Secret Wonders - xxx/132
        totalCardsMapping.put("132:2007", "dp3");
        // Great Encounters - xxx/106
        totalCardsMapping.put("106:2008", "dp4");
        // Majestic Dawn - xxx/100
        totalCardsMapping.put("100:2008", "dp5");
        // Legends Awakened - xxx/146
        totalCardsMapping.put("146:2008", "dp6");
        // Stormfront skipped — same 100:2008 as Majestic Dawn

        // --- PLATINUM ERA (2009-2010) ---
        // Platinum - xxx/127
        totalCardsMapping.put("127:2009", "pl1");
        // Rising Rivals - xxx/111
        totalCardsMapping.put("111:2009", "pl2");
        // Supreme Victors - xxx/147
        totalCardsMapping.put("147:2009", "pl3");
        // Arceus - xxx/99
        totalCardsMapping.put("99:2009", "pl4");

        // --- HEARTGOLD & SOULSILVER ERA (2010-2011) ---
        // HeartGold SoulSilver - xxx/123
        totalCardsMapping.put("123:2010", "hgss1");
        // Unleashed - xxx/95
        totalCardsMapping.put("95:2010", "hgss2");
        // Undaunted - xxx/90
        totalCardsMapping.put("90:2010", "hgss3");
        // Triumphant - xxx/102
        totalCardsMapping.put("102:2010", "hgss4");
        // Call of Legends - xxx/95
        totalCardsMapping.put("95:2011", "col1");

        // --- BLACK & WHITE ERA (2011-2013) ---
        // Black & White - xxx/114
        totalCardsMapping.put("114:2011", "bw1");
        // Emerging Powers - xxx/98
        totalCardsMapping.put("98:2011", "bw2");
        // Noble Victories - xxx/101
        totalCardsMapping.put("101:2011", "bw3");
        // Next Destinies - xxx/99
        totalCardsMapping.put("99:2012", "bw4");
        // Dark Explorers - xxx/108
        totalCardsMapping.put("108:2012", "bw5");
        // Dragons Exalted - xxx/124
        totalCardsMapping.put("124:2012", "bw6");
        // Dragon Vault - xxx/20
        totalCardsMapping.put("20:2012", "dv1");
        // Boundaries Crossed - xxx/149
        totalCardsMapping.put("149:2012", "bw7");
        // Plasma Storm - xxx/135
        totalCardsMapping.put("135:2013", "bw8");
        // Plasma Freeze - xxx/116
        totalCardsMapping.put("116:2013", "bw9");
        // Plasma Blast - xxx/101
        totalCardsMapping.put("101:2013", "bw10");
        // Legendary Treasures - xxx/113
        totalCardsMapping.put("113:2013", "bw11");

        // --- XY ERA (2014-2016) ---
        // XY - xxx/146
        totalCardsMapping.put("146:2014", "xy1");
        // Flashfire - xxx/106
        totalCardsMapping.put("106:2014", "xy2");
        // Furious Fists - xxx/111
        totalCardsMapping.put("111:2014", "xy3");
        // Phantom Forces - xxx/119
        totalCardsMapping.put("119:2014", "xy4");
        // Primal Clash - xxx/160
        totalCardsMapping.put("160:2015", "xy5");
        // Double Crisis - xxx/34
        totalCardsMapping.put("34:2015", "dc1");
        // Roaring Skies - xxx/108
        totalCardsMapping.put("108:2015", "xy6");
        // Ancient Origins - xxx/98
        totalCardsMapping.put("98:2015", "xy7");
        // BREAKthrough - xxx/162
        totalCardsMapping.put("162:2015", "xy8");
        // BREAKpoint - xxx/122
        totalCardsMapping.put("122:2016", "xy9");
        // Generations - xxx/83
        totalCardsMapping.put("83:2016", "g1");
        // Fates Collide - xxx/124
        totalCardsMapping.put("124:2016", "xy10");
        // Steam Siege - xxx/114
        totalCardsMapping.put("114:2016", "xy11");
        // Evolutions - xxx/108
        totalCardsMapping.put("108:2016", "xy12");

        // --- SUN & MOON ERA (2017-2019) ---
        // Sun & Moon - xxx/149
        totalCardsMapping.put("149:2017", "sm1");
        // Guardians Rising - xxx/145
        totalCardsMapping.put("145:2017", "sm2");
        // Burning Shadows - xxx/147
        totalCardsMapping.put("147:2017", "sm3");
        // Shining Legends - xxx/73
        totalCardsMapping.put("73:2017", "sm3.5");
        // Crimson Invasion - xxx/111
        totalCardsMapping.put("111:2017", "sm4");
        // Ultra Prism - xxx/156
        totalCardsMapping.put("156:2018", "sm5");
        // Forbidden Light - xxx/131
        totalCardsMapping.put("131:2018", "sm6");
        // Celestial Storm - xxx/168
        totalCardsMapping.put("168:2018", "sm7");
        // Dragon Majesty - xxx/70
        totalCardsMapping.put("70:2018", "sm7.5");
        // Lost Thunder - xxx/214
        totalCardsMapping.put("214:2018", "sm8");
        // Team Up - xxx/181
        totalCardsMapping.put("181:2019", "sm9");
        // Detective Pikachu - xxx/18
        totalCardsMapping.put("18:2019", "det1");
        // Unbroken Bonds - xxx/214
        totalCardsMapping.put("214:2019", "sm10");
        // Unified Minds - xxx/236
        totalCardsMapping.put("236:2019", "sm11");
        // Hidden Fates - xxx/68
        totalCardsMapping.put("68:2019", "sm115");
        // Cosmic Eclipse skipped — same 236:2019 as Unified Minds

        // --- SWORD & SHIELD ERA ---
        // Sword & Shield - xxx/202
        totalCardsMapping.put("202:2020", "swsh1");
        // Rebel Clash - xxx/192
        totalCardsMapping.put("192:2020", "swsh2");
        // Darkness Ablaze - xxx/189
        totalCardsMapping.put("189:2020", "swsh3");
        // Champion's Path - xxx/73
        totalCardsMapping.put("73:2020", "swsh3.5");
        // Vivid Voltage - xxx/185
        totalCardsMapping.put("185:2020", "swsh4");
        // Shining Fates - xxx/72
        totalCardsMapping.put("72:2021", "swsh4.5");
        // Battle Styles - xxx/163
        totalCardsMapping.put("163:2021", "swsh5");
        // Chilling Reign - xxx/198
        totalCardsMapping.put("198:2021", "swsh6");
        // Evolving Skies - xxx/203
        totalCardsMapping.put("203:2021", "swsh7");
        // Celebrations - xxx/25
        totalCardsMapping.put("25:2021", "cel25");
        // Fusion Strike - xxx/264
        totalCardsMapping.put("264:2021", "swsh8");
        // Brilliant Stars - xxx/172
        totalCardsMapping.put("172:2022", "swsh9");
        // Astral Radiance - xxx/189
        totalCardsMapping.put("189:2022", "swsh10");
        // Pokemon GO - xxx/78
        totalCardsMapping.put("78:2022", "swsh10.5");
        // Lost Origin - xxx/196
        totalCardsMapping.put("196:2022", "swsh11");
        // Silver Tempest - xxx/195
        totalCardsMapping.put("195:2022", "swsh12");
        // Crown Zenith - xxx/159
        totalCardsMapping.put("159:2023", "swsh12.5");

        // --- SCARLET & VIOLET ERA ---
        // Scarlet & Violet base - xxx/198
        totalCardsMapping.put("198:2023", "sv01");
        // Paldea Evolved - xxx/193
        totalCardsMapping.put("193:2023", "sv02");
        // Obsidian Flames - xxx/197
        totalCardsMapping.put("197:2023", "sv03");
        // 151 - xxx/165
        totalCardsMapping.put("165:2023", "sv03.5");
        // Paradox Rift - xxx/182
        totalCardsMapping.put("182:2023", "sv04");
        // Paldean Fates - xxx/91
        totalCardsMapping.put("91:2024", "sv04.5");
        // Temporal Forces - xxx/162
        totalCardsMapping.put("162:2024", "sv05");
        // Twilight Masquerade - xxx/167
        totalCardsMapping.put("167:2024", "sv06");
        // Shrouded Fable - xxx/64
        totalCardsMapping.put("64:2024", "sv06.5");
        // Stellar Crown - xxx/142
        totalCardsMapping.put("142:2024", "sv07");
        // Surging Sparks - xxx/191
        totalCardsMapping.put("191:2024", "sv08");
        // Prismatic Evolutions - xxx/131
        totalCardsMapping.put("131:2025", "sv08.5");
        // Journey Together - xxx/159
        totalCardsMapping.put("159:2025", "sv09");
        // Destined Rivals - xxx/182
        totalCardsMapping.put("182:2025", "sv10");
        // NOTE: Black Bolt (sv10.5b) and White Flare (sv10.5w) both have 86 cards in 2025
        // Cannot disambiguate by total+year alone — relies on set code (BLK vs WHT) from codeToTcgdexId
        totalCardsMapping.put("86:2025", "sv10.5b");  // Default to Black Bolt; WHT code lookup handles White Flare
    }
}