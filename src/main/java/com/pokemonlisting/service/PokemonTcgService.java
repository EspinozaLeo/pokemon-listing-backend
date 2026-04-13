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
    private final ApiUsageService apiUsageService;

    public PokemonTcgService(ApiUsageService apiUsageService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.apiUsageService = apiUsageService;
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
        if(cardNumber == null || cardNumber.isEmpty()){
            throw new IllegalArgumentException("Card number is null or empty");
        }
        if(setId == null || setId.isEmpty()){
            throw new IllegalArgumentException("Set ID number is null or empty");
        }

        String url = BASE_URL + "/sets/" + setId + "/" + cardNumber;
        System.out.println("Calling TCGdex API: " + url);

        try {
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("=== RAW JSON RESPONSE ===");
            System.out.println(response);
            System.out.println("=========================\n");
            if (response == null || response.isEmpty()) {
                return null;
            }
            if(response == null || response.isEmpty()){
                return null;
            }

            JsonNode root = objectMapper.readTree(response);
            String name = root.get("name").asText();
            String localId = root.get("localId").asText();
            JsonNode setNode = root.get("set");
            String setName = setNode.get("name").asText();
            String rarity = root.has("rarity") ? root.get("rarity").asText() : "Unknown";
            JsonNode cardCountNode = setNode.get("cardCount");
            String totalCards = cardCountNode.get("official").asText();
            String fullCardNumber = localId + "/" + totalCards;

            apiUsageService.logApiCall("TCGDEX", 0.00, null);

            return new PokemonCard(name, setName, fullCardNumber, rarity);
        } catch (HttpClientErrorException e) {
            // ========== HANDLE 404 (NOT FOUND) ==========
            if (e.getStatusCode().value() == 404){
                return null;
            }
            throw new RuntimeException("TCGdex API error: " + e.getStatusCode() + " - " + e.getMessage());

        } catch (Exception e) {
            // ========== HANDLE OTHER ERRORS ==========
            throw new RuntimeException("Failed to search card in TCGdex: " + e.getMessage(), e);
        }
    }
}