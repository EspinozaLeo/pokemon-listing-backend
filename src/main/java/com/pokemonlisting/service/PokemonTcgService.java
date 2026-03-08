package com.pokemonlisting.service;

import com.pokemonlisting.dto.PokemonCard;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PokemonTcgService {

    private static final String BASE_URL = "https://api.tcgdex.net/v2/en";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PokemonTcgService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Searches for a Pokemon card by card number and set ID.
     *
     * @param cardNumber Card number (e.g., "25", "9")
     * @param setId TCGdex set ID (e.g., "sv1", "sv3pt5", "swsh1")
     * @return PokemonCard with details, or null if not found
     * @throws IllegalArgumentException if cardNumber or setId is null/empty
     */
    public PokemonCard searchCard(String cardNumber, String setId) {

        // ========== VALIDATION ==========
        if(cardNumber == null || cardNumber.isEmpty()){
            throw new IllegalArgumentException("Card number is null or empty");
        }
        
    }
}