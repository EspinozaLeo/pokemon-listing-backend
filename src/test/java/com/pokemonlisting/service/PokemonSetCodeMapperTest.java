package com.pokemonlisting.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PokemonSetCodeMapperTest {

    private PokemonSetCodeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PokemonSetCodeMapper();
    }

    @Test
    void testToTcgdexId_SV1() {
        assertEquals("sv01", mapper.toTcgdexId("SV1"));
    }

    @Test
    void testToTcgdexId_MEW() {
        assertEquals("sv03.5", mapper.toTcgdexId("MEW"));
    }

    @Test
    void testToTcgdexId_151() {
        assertEquals("sv03.5", mapper.toTcgdexId("151"));
    }

    @Test
    void testToTcgdexId_CaseInsensitive() {
        assertEquals("sv01", mapper.toTcgdexId("sv1"));
        assertEquals("sv03.5", mapper.toTcgdexId("mew"));
    }

    @Test
    void testToTcgdexId_WithWhitespace() {
        assertEquals("sv01", mapper.toTcgdexId("  SV1  "));
    }

    @Test
    void testToTcgdexId_Unknown() {
        assertNull(mapper.toTcgdexId("UNKNOWN"));
    }

    @Test
    void testHasMapping_Known() {
        assertTrue(mapper.hasMapping("SV1"));
        assertTrue(mapper.hasMapping("MEW"));
    }

    @Test
    void testHasMapping_Unknown() {
        assertFalse(mapper.hasMapping("FAKE"));
    }

    @Test
    void testToTcgdexIdByTotal_PaldeanFates() {
        assertEquals("sv04.5", mapper.toTcgdexIdByTotal("91", "2024"));
    }

    @Test
    void testToTcgdexIdByTotal_ParadoxRift() {
        assertEquals("sv04", mapper.toTcgdexIdByTotal("182", "2023"));
    }

    @Test
    void testToTcgdexIdByTotal_PaldeaEvolved() {
        assertEquals("sv02", mapper.toTcgdexIdByTotal("193", "2023"));
    }

    @Test
    void testToTcgdexIdByTotal_WithLeadingZeros() {
        // Should handle "091" same as "91"
        assertEquals("sv04.5", mapper.toTcgdexIdByTotal("091", "2024"));
    }

    @Test
    void testIsAmbiguous_PAL() {
        assertTrue(mapper.isAmbiguous("PAL"));
    }

    @Test
    void testIsAmbiguous_PAR() {
        assertTrue(mapper.isAmbiguous("PAR"));
    }

    @Test
    void testIsAmbiguous_PAF() {
        assertTrue(mapper.isAmbiguous("PAF"));
    }

    @Test
    void testIsAmbiguous_FAR() {
        assertTrue(mapper.isAmbiguous("FAR"));  // Could be PAR or FAL
    }

    @Test
    void testIsAmbiguous_MEW() {
        assertFalse(mapper.isAmbiguous("MEW"));  // Unique pattern
    }

    @Test
    void testIsAmbiguous_OBF() {
        assertFalse(mapper.isAmbiguous("OBF"));  // Unique pattern
    }
}