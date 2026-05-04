package com.pokemonlisting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemonlisting.dto.BatchListRequest;
import com.pokemonlisting.dto.BatchListResponse;
import com.pokemonlisting.dto.CardListingOverride;
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
import java.util.ArrayList;
import java.util.List;

@Service
public class EbayListingService {

    private final CardRepository cardRepository;
    private final EbayTokenService ebayTokenService;
    private final EbayImageService ebayImageService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public EbayListingService(CardRepository cardRepository,
                              EbayTokenService ebayTokenService,
                              EbayImageService ebayImageService) {
        this.cardRepository = cardRepository;
        this.ebayTokenService = ebayTokenService;
        this.ebayImageService = ebayImageService;
    }

    public BatchListResponse listCards(BatchListRequest request) {
        List<ListCardResponse> results = new ArrayList<>();
        ListCardRequest defaults = request.getListingParams();

        for (CardListingOverride override : request.getCards()) {
            ListCardRequest params = new ListCardRequest(
                override.getPrice()     != null ? override.getPrice()     : defaults.getPrice(),
                override.getCondition() != null ? override.getCondition() : defaults.getCondition(),
                override.getFormat()    != null ? override.getFormat()    : defaults.getFormat()
            );
            results.add(listCard(override.getCardId(), params));
        }

        int total = results.size();
        int succeeded = (int) results.stream().filter(r -> "LISTED".equals(r.getStatus())).count();
        int failed = total - succeeded;

        return new BatchListResponse(total, succeeded, failed, results);
    }

    public ListCardResponse listCard(Long cardId, ListCardRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found: " + cardId));

        if (card.getStatus() != CardStatus.IDENTIFIED) {
            return new ListCardResponse(cardId, card.getCardName(), "Card is not identified yet", true);
        }

        String sku = "CARD-" + cardId;

        List<String> imageUrls = ebayImageService.getImageUrls(cardId);

        try {
            createInventoryItem(card, sku, request, imageUrls);
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
        boolean imagesAttached = !imageUrls.isEmpty();
        return new ListCardResponse(cardId, card.getCardName(), listingId, listingUrl, imagesAttached);
    }

    private void createInventoryItem(Card card, String sku, ListCardRequest request, List<String> imageUrls) throws Exception {
        String conditionDescriptorId = cardConditionToDescriptorId(request.getCondition());

        String imageUrlsJson = imageUrls.isEmpty() ? "[]"
                : "[" + String.join(",", imageUrls.stream().map(u -> "\"" + u + "\"").toList()) + "]";

        String body = """
                {
                  "condition": "USED_VERY_GOOD",
                  "conditionDescriptors": [
                    {
                      "name": "40001",
                      "values": ["%s"]
                    }
                  ],
                  "product": {
                    "title": "%s %s",
                    "description": "Pokemon Card - %s, Set: %s, Number: %s, Rarity: %s",
                    "imageUrls": %s,
                    "aspects": {
                      "Game": ["Pokemon TCG"],
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
                conditionDescriptorId,
                card.getCardName(), card.getSetName(),
                card.getCardName(), card.getSetName(), card.getCardNumber(), card.getRarity(),
                imageUrlsJson,
                card.getSetName(), card.getCardNumber()
        );

        String url = ebayTokenService.getBaseUrl() + "/sell/inventory/v1/inventory_item/" + sku;

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", ebayTokenService.getBearerToken())
                .header("Content-Type", "application/json")
                .header("Content-Language", "en-US")
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
                  "categoryId": "183454",
                  "merchantLocationKey": "main-warehouse",
                  "shipToLocations": {
                    "regionIncluded": [
                      {
                        "regionName": "United States",
                        "regionType": "COUNTRY",
                        "regionId": "US"
                      }
                    ]
                  }
                }
                """.formatted(sku, ebayTokenService.getMarketplaceId(), format, request.getPrice());

        String offerUrl = ebayTokenService.getBaseUrl() + "/sell/inventory/v1/offer";

        HttpRequest offerRequest = HttpRequest.newBuilder()
                .uri(URI.create(offerUrl))
                .POST(HttpRequest.BodyPublishers.ofString(offerBody))
                .header("Authorization", ebayTokenService.getBearerToken())
                .header("Content-Type", "application/json")
                .header("Content-Language", "en-US")
                .header("X-EBAY-C-MARKETPLACE-ID", ebayTokenService.getMarketplaceId())
                .build();

        HttpResponse<String> offerResponse = httpClient.send(offerRequest, HttpResponse.BodyHandlers.ofString());

        String offerId;
        if (offerResponse.statusCode() == 201) {
            offerId = objectMapper.readTree(offerResponse.body()).get("offerId").asText();
        } else if (offerResponse.statusCode() == 400) {
            // Offer may already exist from a previous failed attempt — extract the offerId from the error
            JsonNode errors = objectMapper.readTree(offerResponse.body()).path("errors");
            String existingOfferId = null;
            for (JsonNode error : errors) {
                if (error.path("errorId").asInt() == 25002) {
                    for (JsonNode param : error.path("parameters")) {
                        if ("offerId".equals(param.path("name").asText())) {
                            existingOfferId = param.path("value").asText();
                        }
                    }
                }
            }
            if (existingOfferId == null) {
                throw new RuntimeException("eBay offer creation failed (" + offerResponse.statusCode() + "): " + offerResponse.body());
            }
            offerId = existingOfferId;
        } else {
            throw new RuntimeException("eBay offer creation failed (" + offerResponse.statusCode() + "): " + offerResponse.body());
        }

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

    // Maps condition shorthand to eBay's Ungraded Card Condition descriptor IDs for category 183454.
    // conditionDescriptors replace aspects for trading card categories — do not use aspect strings.
    private String cardConditionToDescriptorId(String cardCondition) {
        return switch (cardCondition.toUpperCase()) {
            case "NM"      -> "400010";
            case "LP"      -> "400015";
            case "MP"      -> "400016";
            case "HP"      -> "400017";
            case "DAMAGED" -> "400017";
            default        -> "400010";
        };
    }
}
