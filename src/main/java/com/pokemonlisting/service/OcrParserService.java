package com.pokemonlisting.service;

import com.pokemonlisting.dto.OcrResult;
import com.pokemonlisting.dto.OcrResult.ConfidenceLevel;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrParserService {

    private final PokemonSetCodeMapper setCodeMapper;
    private final PokemonDexService dexService;

    // Regex patterns
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("(\\d{1,3})/(\\d{1,3})");
    private static final Pattern SET_CODE_PATTERN = Pattern.compile("[A-Z]{2,5}\\s+EN\\s+(\\d{1,3})/(\\d{1,3})");
    private static final Pattern COPYRIGHT_PATTERN = Pattern.compile("(?:©|C|0)?\\s*(20\\d{2})\\s+(?:Pokémon|Pokemon|Nintendo|Creatures)");
    private static final Pattern DEX_ID_PATTERN = Pattern.compile("NO\\.\\s*(\\d{3,4})");

    public OcrParserService(PokemonSetCodeMapper setCodeMapper, PokemonDexService dexService) {
        this.setCodeMapper = setCodeMapper;
        this.dexService = dexService;
    }

    /**
     * Parses raw OCR text into structured card information.
     *
     * @param rawText OCR output from Google Vision
     * @return OcrResult with all extracted fields
     */
    public OcrResult parseCardDetails(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return createLowConfidenceResult();
        }

        // Clean text for regex matching
        String cleanedText = rawText.replaceAll("\\s+", " ").trim();

        // Extract all fields
        String cardNumber = extractCardNumber(cleanedText);
        String totalCards = extractTotalCards(cleanedText);
        String setCode = extractSetCode(cleanedText);
        String copyrightYear = extractCopyrightYear(cleanedText);
        String dexId = extractDexId(cleanedText);
        String cardName = extractCardName(rawText);  // Use raw text (preserves newlines)

        // Process Dex ID and validate/correct card name
        Integer normalizedDexId = normalizeDexId(dexId);
        String expectedName = null;
        Boolean nameMatchesDex = null;

        if (normalizedDexId != null && dexService.isValidDexId(normalizedDexId)) {
            expectedName = dexService.getName(normalizedDexId);

            if (cardName != null && expectedName != null) {
                nameMatchesDex = dexService.validateName(cardName, normalizedDexId);

                // If name doesn't match, use expected name and mark as corrected
                if (Boolean.FALSE.equals(nameMatchesDex)) {
                    cardName = expectedName;
                    nameMatchesDex = true;  // Name is now correct (we fixed it)
                }
            } else if (cardName == null && expectedName != null) {
                // Recovered card name from Dex ID
                cardName = expectedName;
                nameMatchesDex = true;  // Recovered name is valid
            }
        }

        // Map set code to TCGdex ID
        String tcgdexSetId = mapToTcgdexId(setCode, totalCards, copyrightYear);

        // Determine confidence
        ConfidenceLevel confidence = determineConfidence(
                cardNumber, setCode, tcgdexSetId, nameMatchesDex
        );

        return new OcrResult(
                cardName,
                cardNumber,
                totalCards,
                setCode,
                tcgdexSetId,
                copyrightYear,
                dexId,
                normalizedDexId,
                expectedName,
                nameMatchesDex,
                confidence
        );
    }

    /**
     * Extract card number (e.g., "69" from "069/131")
     */
    private String extractCardNumber(String text) {
        Matcher matcher = CARD_NUMBER_PATTERN.matcher(text);
        if (matcher.find()) {
            String number = matcher.group(1);
            // Remove leading zeros
            return String.valueOf(Integer.parseInt(number));
        }
        return null;
    }

    /**
     * Extract total cards (e.g., "131" from "069/131")
     */
    private String extractTotalCards(String text) {
        Matcher matcher = CARD_NUMBER_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return null;
    }

    /**
     * Extract set code (e.g., "PRE" from "PRE EN 069/131")
     * This is best-effort as OCR often mangles it.
     */
    private String extractSetCode(String text) {
        // Try to match pattern: [SET_CODE] EN [NUMBER]/[TOTAL]
        Matcher fullMatcher = SET_CODE_PATTERN.matcher(text);
        if (fullMatcher.find()) {
            // Extract the part before "EN"
            String beforePattern = text.substring(0, fullMatcher.start());

            // Look for 2-5 uppercase letters before "EN"
            Pattern setPattern = Pattern.compile("([A-Z0-9]{2,5})\\s+EN");
            Matcher setMatcher = setPattern.matcher(text);

            if (setMatcher.find()) {
                String candidate = setMatcher.group(1).trim();

                // Filter out regulation marks and common false positives
                if (candidate.length() <= 1 ||
                        candidate.equals("HP") ||
                        candidate.equals("NO")) {
                    return null;
                }

                return candidate;
            }
        }

        // Fallback: look for pattern near card number
        // This is less reliable but worth trying
        Pattern fallbackPattern = Pattern.compile("([A-Z]{2,4})\\s+\\d{1,3}/\\d{1,3}");
        Matcher fallbackMatcher = fallbackPattern.matcher(text);

        if (fallbackMatcher.find()) {
            String candidate = fallbackMatcher.group(1);

            if (!candidate.equals("HP") &&
                    !candidate.equals("NO") &&
                    !candidate.equals("EN")) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Extract copyright year (handles ©, C, 0, or missing symbol)
     */
    private String extractCopyrightYear(String text) {
        Matcher matcher = COPYRIGHT_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extract Pokédex ID (e.g., "0133" or "133" from "NO. 0133")
     */
    private String extractDexId(String text) {
        Matcher matcher = DEX_ID_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extract card name from OCR text.
     * Uses multiple strategies to identify the name line.
     */
    private String extractCardName(String text) {
        String[] lines = text.split("\\n");

        // Strategy 1: For Pokémon cards - line before HP
        for (int i = 0; i < lines.length - 1; i++) {
            String currentLine = lines[i].trim();
            String nextLine = lines[i + 1].trim();

            // If next line has HP, current line is probably the name
            if (nextLine.matches(".*HP\\s*\\d+.*") || nextLine.matches("HP\\s*\\d+")) {
                // Skip evolution stages
                if (currentLine.matches("(BASIC|STAGE 1|STAGE 2|STAGE)")) {
                    continue;
                }
                return cleanCardName(currentLine);
            }
        }

        // Strategy 2: For Pokémon - line between BASIC and HP
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.matches("(BASIC|STAGE 1|STAGE 2)")) {
                // Next non-empty line should be the name (before HP)
                for (int j = i + 1; j < Math.min(i + 3, lines.length); j++) {
                    String nextLine = lines[j].trim();
                    if (!nextLine.isEmpty() &&
                            !nextLine.matches("HP.*") &&
                            !nextLine.matches(".*\\d{1,3}/\\d{1,3}.*")) {
                        return cleanCardName(nextLine);
                    }
                }
            }
        }

        // Strategy 3: For Trainer cards - structure is more complex
        // Look for pattern: "Trainer" → "Supporter/Item/etc" → NAME → description
        Integer trainerLineIndex = null;
        Integer typeLineIndex = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.equals("Trainer")) {
                trainerLineIndex = i;
            } else if (line.matches("(Supporter|Item|Stadium|Tool)") && trainerLineIndex != null) {
                typeLineIndex = i;
                break;
            }
        }

        if (trainerLineIndex != null && typeLineIndex != null) {
            // Name is the first substantial line after the type
            for (int i = typeLineIndex + 1; i < Math.min(typeLineIndex + 5, lines.length); i++) {
                String line = lines[i].trim();

                // Skip empty lines and metadata
                if (!line.isEmpty() &&
                        !line.matches("\\d{1,3}/\\d{1,3}.*") &&
                        !line.matches(".*©.*") &&
                        !line.matches(".*Pokémon.*") &&
                        line.length() > 2) {
                    return cleanCardName(line);
                }
            }
        }

        // Strategy 4: Fallback - first line that looks like a name
        for (String line : lines) {
            String trimmed = line.trim();

            if (isLikelyCardName(trimmed)) {
                return cleanCardName(trimmed);
            }
        }

        return null;
    }

    /**
     * Check if a line looks like a card name.
     */
    private boolean isLikelyCardName(String line) {
        if (line == null || line.isEmpty() || line.length() <= 2) {
            return false;
        }

        // Exclude common non-name patterns
        String[] excludePatterns = {
                "BASIC", "STAGE", "STAGE 1", "STAGE 2",
                "Trainer", "Supporter", "Item", "Stadium", "Tool",
                "HP\\s*\\d+.*",           // HP line
                "\\d{1,3}/\\d{1,3}.*",    // Card number
                "NO\\..*",                 // Dex number
                ".*©.*",                   // Copyright
                ".*Pokémon.*Nintendo.*",   // Copyright footer
                ".*Creatures.*GAME.*",     // Copyright footer
                "weakness.*",              // Game text
                "resistance.*",            // Game text
                "retreat.*",               // Game text
                "Ability",                 // Ability header
                "Illus\\..*",             // Illustrator
                ".*HT:.*WT:.*"            // Height/weight
        };

        for (String pattern : excludePatterns) {
            if (line.matches(pattern)) {
                return false;
            }
        }

        // Must start with a capital letter or apostrophe (for names like 'Farfetch'd)
        if (!line.matches("^[A-Z'].*")) {
            return false;
        }

        return true;
    }

    /**
     * Clean extracted card name - remove trailing junk.
     * GENERALIZED approach using linguistic patterns.
     */
    private String cleanCardName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        // Remove copyright symbols and everything after
        name = name.split("©")[0];
        name = name.split("\\(C\\)")[0];

        // Remove card number pattern and everything after
        name = name.replaceAll("\\d{1,3}/\\d{1,3}.*", "");

        // Remove common sentence starters (instructions on Trainer cards)
        // Pattern: Remove everything after a verb at sentence start
        String[] sentenceStarters = {
                "Draw ", "Search ", "Put ", "Choose ", "Look ", "Shuffle ",
                "Discard ", "Attach ", "Switch ", "Flip ", "Reveal ", "Take ",
                "You may ", "Your ", "This ", "During ", "If ", "When ",
                "Each ", "All ", "Until ", "Once ", "At ", "Remove ", "Return "
        };

        for (String starter : sentenceStarters) {
            int index = name.indexOf(starter);
            if (index > 0) {  // Must be AFTER the name starts
                name = name.substring(0, index);
                break;
            }
        }

        // Remove periods and everything after (often starts description)
        if (name.contains(".")) {
            // But keep periods in names like "Mr. Mime"
            if (!name.matches(".*(Mr|Mrs|Dr|Prof)\\..*")) {
                name = name.split("\\.")[0];
            }
        }

        // Trim whitespace
        name = name.trim();

        // If name is now too long (probably includes description), take first few words
        String[] words = name.split("\\s+");
        if (words.length > 5) {
            // Likely includes description, take first 2-3 words (typical card name length)
            // But be smart: if it looks like "Team Rocket's Houndour" keep all
            if (name.matches(".*'s .*")) {
                // Possessive name like "Team Rocket's X" - keep first 3-4 words
                name = String.join(" ", Arrays.copyOfRange(words, 0, Math.min(4, words.length)));
            } else {
                // Regular name - keep first 2-3 words
                name = String.join(" ", Arrays.copyOfRange(words, 0, Math.min(3, words.length)));
            }
        }

        return name.trim();
    }

    /**
     * Normalize Dex ID (remove leading zeros, convert to integer)
     */
    private Integer normalizeDexId(String dexId) {
        if (dexId == null || dexId.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(dexId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Map set code to TCGdex ID.
     * Strategy:
     * 1. Try set code mapping if clean and unambiguous
     * 2. Fall back to total+year (very reliable!)
     * 3. Last resort: try set code anyway
     */
    private String mapToTcgdexId(String setCode, String totalCards, String copyrightYear) {
        // Strategy 1: Try set code mapping if clean AND not ambiguous
        if (setCode != null && isCleanSetCode(setCode)) {
            // Check if it's an ambiguous code (PAL/PAR/PAF)
            if (!setCodeMapper.isAmbiguous(setCode)) {
                String tcgdexId = setCodeMapper.toTcgdexId(setCode);
                if (tcgdexId != null) {
                    return tcgdexId;  // Fast path for MEW, OBF, SVI, etc.
                }
            }
        }

        // Strategy 2: Use total+year fallback (handles ambiguous codes)
        if (totalCards != null && copyrightYear != null) {
            String tcgdexId = setCodeMapper.toTcgdexIdByTotal(totalCards, copyrightYear);
            if (tcgdexId != null) {
                return tcgdexId;  // Very reliable! Handles PAL/PAR/PAF perfectly
            }
        }

        // Strategy 3: Try set code anyway as last resort
        // (in case it's ambiguous but we don't have total+year)
        if (setCode != null && isCleanSetCode(setCode)) {
            return setCodeMapper.toTcgdexId(setCode);
        }

        return null;
    }

    /**
     * Check if a set code looks clean (not mangled by OCR)
     */
    private boolean isCleanSetCode(String setCode) {
        if (setCode == null) {
            return false;
        }
        // A valid set code is exactly 3 uppercase letters — garbage OCR output won't match
        return setCode.trim().toUpperCase().matches("[A-Z]{3}");
    }

    /**
     * Determine overall confidence level
     */
    private ConfidenceLevel determineConfidence(
            String cardNumber,
            String setCode,
            String tcgdexSetId,
            Boolean nameMatchesDex
    ) {
        // HIGH: Have card number + set ID + validated name
        if (cardNumber != null && tcgdexSetId != null &&
                nameMatchesDex != null && nameMatchesDex) {
            return ConfidenceLevel.HIGH;
        }

        // MEDIUM: Have card number + either set or name validation
        if (cardNumber != null && (tcgdexSetId != null ||
                (nameMatchesDex != null && nameMatchesDex))) {
            return ConfidenceLevel.MEDIUM;
        }

        // MEDIUM: Have card number only (can try fallback strategies)
        if (cardNumber != null) {
            return ConfidenceLevel.MEDIUM;
        }

        // LOW: Missing critical data
        return ConfidenceLevel.LOW;
    }

    /**
     * Create a low-confidence result for invalid input
     */
    private OcrResult createLowConfidenceResult() {
        return new OcrResult(
                null, null, null, null, null, null,
                null, null, null, null, ConfidenceLevel.LOW
        );
    }
}