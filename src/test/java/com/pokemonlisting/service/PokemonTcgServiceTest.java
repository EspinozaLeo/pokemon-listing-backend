package com.pokemonlisting.service;

import com.pokemonlisting.dto.PokemonCard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PokemonTcgServiceTest {

    private final PokemonTcgService tcgDexService = new PokemonTcgService(mock(ApiUsageService.class));

    @Test
    void testSearchCard_Toedscool_SV1() {
        // Arrange
        String cardNumber = "25";
        String setId = "sv01";

        // Act
        PokemonCard card = tcgDexService.searchCard(cardNumber, setId);

        // Assert
        assertNotNull(card, "Card should be found");
        assertEquals("Toedscool", card.getName());
        assertEquals("Scarlet & Violet", card.getSetName());
        assertTrue(card.getCardNumber().contains("25"));
        assertNotNull(card.getRarity());

        // Debug output
        System.out.println("=== Found Toedscool ===");
        System.out.println(card);
    }

    @Test
    void testSearchCard_Blastoise_151() {
        // The card you tested earlier!
        String cardNumber = "9";
        String setId = "sv03.5"; // 151 set

        PokemonCard card = tcgDexService.searchCard(cardNumber, setId);

        assertNotNull(card, "Blastoise ex should be found");
        assertTrue(card.getName().contains("Blastoise"));

        System.out.println("=== Found Blastoise ===");
        System.out.println(card);
    }

    @Test
    void testSearchCard_Charizard_Base() {
        // Vintage card
        String cardNumber = "4";
        String setId = "base1";

        PokemonCard card = tcgDexService.searchCard(cardNumber, setId);

        assertNotNull(card, "Charizard should be found");
        assertEquals("Charizard", card.getName());

        System.out.println("=== Found Charizard ===");
        System.out.println(card);
    }

    @Test
    void testSearchCard_NotFound() {
        // Card that doesn't exist
        String cardNumber = "999";
        String setId = "sv1";

        PokemonCard card = tcgDexService.searchCard(cardNumber, setId);

        assertNull(card, "Fake card should return null");
        System.out.println("=== Card Not Found (expected) ===");
    }

    @Test
    void testSearchCard_NullCardNumber() {
        assertThrows(IllegalArgumentException.class, () -> {
            tcgDexService.searchCard(null, "sv1");
        }, "Should throw exception for null card number");
    }

    @Test
    void testSearchCard_EmptyCardNumber() {
        assertThrows(IllegalArgumentException.class, () -> {
            tcgDexService.searchCard("", "sv1");
        }, "Should throw exception for empty card number");
    }

    @Test
    void testSearchCard_NullSetId() {
        assertThrows(IllegalArgumentException.class, () -> {
            tcgDexService.searchCard("25", null);
        }, "Should throw exception for null set ID");
    }

    @Test
    void testSearchCard_EmptySetId() {
        assertThrows(IllegalArgumentException.class, () -> {
            tcgDexService.searchCard("25", "");
        }, "Should throw exception for empty set ID");
    }
}