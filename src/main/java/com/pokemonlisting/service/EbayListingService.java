package com.pokemonlisting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemonlisting.dto.BatchListRequest;
import com.pokemonlisting.dto.BatchListResponse;
import com.pokemonlisting.dto.ListCardRequest;
import com.pokemonlisting.dto.ListCardResponse;
import com.pokemonlisting.model.Card;
import com.pokemonlisting.model.CardStatus;
import com.pokemonlisting.repository.CardRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class EbayListingService {

    private final CardRepository cardRepository;
    private final EbayTokenService ebayTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public EbayListingService(CardRepository cardRepository,
                               EbayTokenService ebayTokenService) {
        this.cardRepository = cardRepository;
        this.ebayTokenService = ebayTokenService;
    }

    public BatchListResponse listCards(BatchListRequest request) {
        // TODO 5: Loop over request.getCardIds() and call listCard() for each one
        // Collect each ListCardResponse into a list
        // Hint: use a regular for-loop or stream — either works
        //   List<ListCardResponse> results = new ArrayList<>();
        //   for (Long id : request.getCardIds()) {
        //       results.add(listCard(id, request.getListingParams()));
        //   }

        // TODO 6: Build and return a BatchListResponse using the results list
        // Hint: count succeeded with results.stream().filter(r -> "LISTED".equals(r.getStatus())).count()
        return null; // replace this
    }

    public ListCardResponse listCard(Long cardId, ListCardRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found: " + cardId));

        if (card.getStatus() != CardStatus.IDENTIFIED) {
            return new ListCardResponse(cardId, card.getCardName(), "Card is not identified yet", true);
        }

        String sku = "CARD-" + cardId;

        try {
            createInventoryItem(card, sku, request);
        } catch (Exception e) {
            return new ListCardResponse(cardId, card.getCardName(), "Failed to create inventory item: " + e.getMessage(), true);
        }

        String listingId;
        try {
            listingId = createAndPublishOffer(sku, request);
        } catch (Exception e) {
            return new ListCardResponse(cardId, card.getCardName(), "Failed to publish offer: " + e.getMessage(), true);
        }

        card.setStatus(CardStatus.LISTED);
        card.setEbayListingId(listingId);
        cardRepository.save(card);

        String listingUrl = "https://www.sandbox.ebay.com/itm/" + listingId;
        return new ListCardResponse(cardId, card.getCardName(), listingId, listingUrl);
    }

    private void createInventoryItem(Card card, String sku, ListCardRequest request) throws Exception {
        String body = """
                {
                  "condition": "%s",
                  "product": {
                    "title": "%s %s",
                    "description": "Pokemon Card - %s, Set: %s, Number: %s, Rarity: %s",
                    "aspects": {
                      "Set": ["%s"],
                      "Card Number": ["%s"]
                    }
                  },
                  "availability": {
                    "shipToLocationAvailability": {
                      "quantity": 1
                    }
                  }
                }
                """.formatted(
                request.getCondition(),
                card.getCardName(), card.getSetName(),
                card.getCardName(), card.getSetName(), card.getCardNumber(), card.getRarity(),
                card.getSetName(), card.getCardNumber()
        );

        String url = ebayTokenService.getBaseUrl() + "/sell/inventory/v1/inventory_item/" + sku;

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", ebayTokenService.getBearerToken())
                .header("Content-Type", "application/json")
                .header("X-EBAY-C-MARKETPLACE-ID", ebayTokenService.getMarketplaceId())
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new RuntimeException("eBay inventory item creation failed (" + response.statusCode() + "): " + response.body());
        }
    }

    private String createAndPublishOffer(String sku, ListCardRequest request) throws Exception {
        String format = request.getFormat() != null ? request.getFormat() : "FIXED_PRICE";

        // NOTE: fulfillmentPolicyId, paymentPolicyId, returnPolicyId must be set up
        // in your eBay sandbox seller account via the eBay Account API or Seller Hub.
        // Replace the placeholder values below with your actual sandbox policy IDs.
        String offerBody = """
                {
                  "sku": "%s",
                  "marketplaceId": "%s",
                  "format": "%s",
                  "listingPolicies": {
                    "fulfillmentPolicyId": "6224962000",
                    "paymentPolicyId": "6224993000",
                    "returnPolicyId": "6224992000"
                  },
                  "pricingSummary": {
                    "price": {
                      "value": "%.2f",
                      "currency": "USD"
                    }
                  },
                  "categoryId": "183454"
                }
                """.formatted(sku, ebayTokenService.getMarketplaceId(), format, request.getPrice());

        String offerUrl = ebayTokenService.getBaseUrl() + "/sell/inventory/v1/offer";

        HttpRequest offerRequest = HttpRequest.newBuilder()
                .uri(URI.create(offerUrl))
                .POST(HttpRequest.BodyPublishers.ofString(offerBody))
                .header("Authorization", ebayTokenService.getBearerToken())
                .header("Content-Type", "application/json")
                .header("X-EBAY-C-MARKETPLACE-ID", ebayTokenService.getMarketplaceId())
                .build();

        HttpResponse<String> offerResponse = httpClient.send(offerRequest, HttpResponse.BodyHandlers.ofString());

        if (offerResponse.statusCode() != 201) {
            throw new RuntimeException("eBay offer creation failed (" + offerResponse.statusCode() + "): " + offerResponse.body());
        }

        JsonNode offerJson = objectMapper.readTree(offerResponse.body());
        String offerId = offerJson.get("offerId").asText();

        String publishUrl = ebayTokenService.getBaseUrl() + "/sell/inventory/v1/offer/" + offerId + "/publish";

        HttpRequest publishRequest = HttpRequest.newBuilder()
                .uri(URI.create(publishUrl))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Authorization", ebayTokenService.getBearerToken())
                .header("Content-Type", "application/json")
                .header("X-EBAY-C-MARKETPLACE-ID", ebayTokenService.getMarketplaceId())
                .build();

        HttpResponse<String> publishResponse = httpClient.send(publishRequest, HttpResponse.BodyHandlers.ofString());

        if (publishResponse.statusCode() != 200) {
            throw new RuntimeException("eBay offer publish failed (" + publishResponse.statusCode() + "): " + publishResponse.body());
        }

        JsonNode publishJson = objectMapper.readTree(publishResponse.body());
        return publishJson.get("listingId").asText();
    }
}
