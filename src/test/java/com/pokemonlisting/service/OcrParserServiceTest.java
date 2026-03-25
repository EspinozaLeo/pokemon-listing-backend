package com.pokemonlisting.service;

import com.pokemonlisting.dto.OcrResult;
import com.pokemonlisting.dto.OcrResult.ConfidenceLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OcrParserServiceTest {

    private OcrParserService parserService;

    @BeforeEach
    void setUp() {
        PokemonSetCodeMapper mapper = new PokemonSetCodeMapper();
        PokemonDexService dexService = new PokemonDexService();
        parserService = new OcrParserService(mapper, dexService);
    }

    @Test
    void testParseCardDetails_Eevee_RealOCR() {
        String ocrText = """
            BASIC
            Eevee
            P50*
            NO. 0133 Evolution Pokémon HT: 1 WT: 14.3 lbs.
            Ability
            Boosted Evolution
            As long as this Pokémon is in the Active Spot, it can evolve
            during your first turn or the turn you play it.
            Reckless Charge
            This Pokémon also does 10 damage to itself.
            30
            weakness ×2 ❘ resistance
            Illus. Naoyo Kimura
            retreat
            Its genetic code is irregular. It may mulate if it is
            exposed to radiation from element stones.
            H PREEN 074/131
            2025 Pokémon/Nintendo/Creatures/GAME FREAK
            """;

        OcrResult result = parserService.parseCardDetails(ocrText);

        System.out.println("=== Eevee Test Result ===");
        System.out.println(result);

        // Assertions
        assertEquals("Eevee", result.getCardName());
        assertEquals("74", result.getCardNumber());
        assertEquals("131", result.getTotalCards());
        assertEquals("2025", result.getCopyrightYear());
        assertEquals("0133", result.getDexId());
        assertEquals(133, result.getNormalizedDexId());
        assertEquals("Eevee", result.getExpectedName());
        assertTrue(result.getNameMatchesDex());
        // Note: setCode might be null or "PREEN" (mangled), tcgdexSetId might be null
        assertNotNull(result.getConfidence());
    }

    @Test
    void testParseCardDetails_Buneary() {
        String ocrText = """
            BASIC
            Buneary
            HP60
            *
            NO. 0427 Rabbit Pokémon HT 1'4" WT: 12.1 lbs.
            Smash Kick
            20
            weakness
            ×2 | resistance
            Illus. Naoki Saito
            H PRE EN 083/131
            retreat ⭑
            If both of Buneary's ears are rolled up,
            something is wrong with its body or mind. It's
            a sure sign the Pokémon is in need of care.
            C2025 Pokémon/Nintendo/Creatures/GAME FREAK
            """;

        OcrResult result = parserService.parseCardDetails(ocrText);

        System.out.println("=== Buneary Test Result ===");
        System.out.println(result);

        assertEquals("Buneary", result.getCardName());
        assertEquals("83", result.getCardNumber());
        assertEquals("131", result.getTotalCards());
        assertEquals("2025", result.getCopyrightYear());
        assertEquals("0427", result.getDexId());
        assertEquals(427, result.getNormalizedDexId());
        assertEquals("Buneary", result.getExpectedName());
        assertTrue(result.getNameMatchesDex());
    }

    @Test
    void testParseCardDetails_Tatsugiri_TWM() {
        String ocrText = """
            BASIC
            Tatsugiri
            00
            NO. 0978 Mimicry Pokémon HT: 1 WT 17.6 lbs.
            HP
            70
            Ability
            Attract Customers
            Once during your turn, if this Pokémon is in the Active
            Spot, you may look at the top 6 cards of your deck, reveal
            a Supporter card you find there, and put it into your hand.
            Shuffle the other cards back into your deck.
            Surf
            50
            weakness
            Illus. Jerky
            resistance
            retreat
            Tatsugiri is an extremely cunning Pokémon.
            It feigns weakness to lure in prey, then
            orders its partner to attack.
            H TWMEN 131/167
            ©2024 Pokémon/Nintendo/Creatures/GAME FREAK
            """;

        OcrResult result = parserService.parseCardDetails(ocrText);

        System.out.println("=== Tatsugiri Test Result ===");
        System.out.println(result);

        assertEquals("Tatsugiri", result.getCardName());
        assertEquals("131", result.getCardNumber());
        assertEquals("167", result.getTotalCards());
        assertEquals("2024", result.getCopyrightYear());
        assertEquals("0978", result.getDexId());
        assertEquals(978, result.getNormalizedDexId());
    }

    @Test
    void testParseCardDetails_TrainerCard_NoDeX() {
        String ocrText = """
            Trainer
            Supporter
            Professor's Research
            Draw 7 cards.
            ©2023 Pokémon
            """;

        OcrResult result = parserService.parseCardDetails(ocrText);

        System.out.println("=== Trainer Card Test Result ===");
        System.out.println(result);

        assertEquals("Professor's Research", result.getCardName());
        assertNull(result.getDexId());
        assertNull(result.getNormalizedDexId());
        assertEquals("2023", result.getCopyrightYear());
    }

    @Test
    void testParseCardDetails_NameTypo_Corrected() {
        // Simulate OCR typo in name
        String ocrText = """
            BASIC
            Pikatchu
            HP 60
            NO. 0025 Mouse Pokémon
            025/198 SV1 EN
            ©2023 Pokémon
            """;

        OcrResult result = parserService.parseCardDetails(ocrText);

        System.out.println("=== Name Typo Test Result ===");
        System.out.println(result);

        // Should correct "Pikatchu" to "Pikachu" using Dex lookup
        assertEquals("Pikachu", result.getCardName());
        assertEquals("Pikachu", result.getExpectedName());
        assertEquals(25, result.getNormalizedDexId());
    }

    @Test
    void testParseCardDetails_MissingName_Recovered() {
        // Simulate missing card name but have Dex ID
        String ocrText = """
            BASIC
            HP 130
            NO. 0006 Flame Pokémon
            4/102
            ©1999 Wizards
            """;

        OcrResult result = parserService.parseCardDetails(ocrText);

        System.out.println("=== Missing Name Test Result ===");
        System.out.println(result);

        // Should recover name from Dex ID
        assertEquals("Charizard", result.getCardName());
        assertEquals("Charizard", result.getExpectedName());
        assertEquals(6, result.getNormalizedDexId());
        assertTrue(result.getNameMatchesDex());
    }

    @Test
    void testParseCardDetails_EmptyText() {
        OcrResult result = parserService.parseCardDetails("");

        assertEquals(ConfidenceLevel.LOW, result.getConfidence());
        assertNull(result.getCardNumber());
    }

    @Test
    void testParseCardDetails_NullText() {
        OcrResult result = parserService.parseCardDetails(null);

        assertEquals(ConfidenceLevel.LOW, result.getConfidence());
        assertNull(result.getCardNumber());
    }
}