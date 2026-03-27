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
        //Scarlet & Violet base (SV1) - sv01
        codeToTcgdexId.put("SVI", "sv01");
        codeToTcgdexId.put("SVL", "sv01");
        codeToTcgdexId.put("SVl", "sv01");

        //Paldea Evolved (PAL) - sv02
        //ONLY exact match - too risky to add variations (conflicts with PAR/PAF)
        codeToTcgdexId.put("PAL", "sv02");

        //Obsidian Flames (OBF) - sv03
        codeToTcgdexId.put("OBF", "sv03");
        codeToTcgdexId.put("OBE", "sv03");
        codeToTcgdexId.put("DBF", "sv03");
        codeToTcgdexId.put("ODF", "sv03");
        codeToTcgdexId.put("OBP", "sv03");

        //151 (MEW) - sv03.5
        codeToTcgdexId.put("MEW", "sv03.5");
        codeToTcgdexId.put("NEW", "sv03.5");
        codeToTcgdexId.put("MFW", "sv03.5");
        codeToTcgdexId.put("NFW", "sv03.5");
        codeToTcgdexId.put("MEM", "sv03.5");

        //Paradox Rift (PAR) - sv04
        //ONLY exact match - too risky to add variations (conflicts with PAL/PAF)
        codeToTcgdexId.put("PAR", "sv04");

        //Paldean Fates (PAF) - sv04.5
        //ONLY exact match - too risky to add variations (conflicts with PAL/PAR)
        codeToTcgdexId.put("PAF", "sv04.5");

        //Temporal Forces (TEF) - sv05
        codeToTcgdexId.put("TEF", "sv05");

        //Twilight Masquerade (TWM) - sv06
        codeToTcgdexId.put("TWM", "sv06");

        //Shrouded Fable (SFA) - sv06.5
        codeToTcgdexId.put("SFA", "sv06.5");

        //Stellar Crown (SCR) - sv07
        codeToTcgdexId.put("SCR", "sv07");

        //Surging Sparks (SSP) - sv08
        codeToTcgdexId.put("SSP", "sv08");

        //Prismatic Evolutions (PRE) - sv08.5
        codeToTcgdexId.put("PRE", "sv08.5");
        // Add more sets as needed
    }

    /**
     * initializeTotalCardsMapping() initialize total cards (denominator) +
     * year mapping. This is the reliable fallback when set codes are
     * ambiguous or mangled.
     */
    private void initializeTotalCardsMapping() {
        //Format: "denominator:year" → tcgdexId
        // The denominator is what appears on cards (e.g., "091" in "074/091")

        //Scarlet & Violet base - xxx/198
        totalCardsMapping.put("198:2023", "sv01");

        //Paldea Evolved - xxx/193
        totalCardsMapping.put("193:2023", "sv02");

        //Obsidian Flames - xxx/197
        totalCardsMapping.put("197:2023", "sv03");

        //151 - xxx/165
        totalCardsMapping.put("165:2023", "sv03.5");
        totalCardsMapping.put("207:2023", "sv03.5");  // If there are variants

        //Paradox Rift - xxx/182
        totalCardsMapping.put("182:2023", "sv04");

        //Paldean Fates - xxx/091
        totalCardsMapping.put("91:2024", "sv04.5");

        //Temporal Forces - xxx/162
        totalCardsMapping.put("162:2024", "sv05");

        //Twilight Masquerade - xxx/167
        totalCardsMapping.put("167:2024", "sv06");

        //Shrouded Fable - xxx/064
        totalCardsMapping.put("64:2024", "sv06.5");

        //Stellar Crown - xxx/142
        totalCardsMapping.put("142:2024", "sv07");

        //Surging Sparks - xxx/191
        totalCardsMapping.put("191:2024", "sv08");

        //Prismatic Evolutions - xxx/131
        totalCardsMapping.put("131:2025", "sv08.5");

        //


        // Add more as you discover set sizes
    }
}