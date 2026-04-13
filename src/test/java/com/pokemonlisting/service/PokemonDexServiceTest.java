package com.pokemonlisting.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PokemonDexServiceTest {

    private PokemonDexService dexService;

    @BeforeEach
    void setUp() {
        dexService = new PokemonDexService();
    }

    @Test
    void testGetName_Pikachu() {
        assertEquals("Pikachu", dexService.getName(25));
    }

    @Test
    void testGetName_Eevee() {
        assertEquals("Eevee", dexService.getName(133));
    }

    @Test
    void testGetName_Charizard() {
        assertEquals("Charizard", dexService.getName(6));
    }

    @Test
    void testGetName_Invalid() {
        assertNull(dexService.getName(9999));
    }

    @Test
    void testIsValidDexId_Valid() {
        assertTrue(dexService.isValidDexId(1));
        assertTrue(dexService.isValidDexId(25));
        assertTrue(dexService.isValidDexId(1025));
    }

    @Test
    void testIsValidDexId_Invalid() {
        assertFalse(dexService.isValidDexId(0));
        assertFalse(dexService.isValidDexId(1026));
        assertFalse(dexService.isValidDexId(9999));
    }

    @Test
    void testValidateName_ExactMatch() {
        assertTrue(dexService.validateName("Pikachu", 25));
        assertTrue(dexService.validateName("Eevee", 133));
    }

    @Test
    void testValidateName_CaseInsensitive() {
        assertTrue(dexService.validateName("PIKACHU", 25));
        assertTrue(dexService.validateName("pikachu", 25));
    }

    @Test
    void testValidateName_WithSuffix() {
        assertTrue(dexService.validateName("Pikachu ex", 25));
        assertTrue(dexService.validateName("Charizard VMAX", 6));
    }

    @Test
    void testValidateName_TeamRocketsFormat() {
        assertTrue(dexService.validateName("Team Rocket's Houndour", 228));
    }

    @Test
    void testValidateName_Mismatch() {
        assertFalse(dexService.validateName("Pikachu", 133)); // Wrong Pokémon
    }
}