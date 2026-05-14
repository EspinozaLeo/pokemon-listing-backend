package com.pokemonlisting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pokemonlisting.dto.ActiveListingResponse;
import com.pokemonlisting.dto.BatchListRequest;
import com.pokemonlisting.dto.BatchListResponse;
import com.pokemonlisting.dto.CardListingOverride;
import com.pokemonlisting.dto.ListCardRequest;
import com.pokemonlisting.dto.ListCardResponse;
import com.pokemonlisting.dto.UpdateListingRequest;
import com.pokemonlisting.dto.UpdateListingResponse;
import com.pokemonlisting.model.Card;
import com.pokemonlisting.model.CardStatus;
import com.pokemonlisting.repository.CardRepository;
import com.pokemonlisting.repository.ShippingPresetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
    private final ShippingPresetRepository shippingPresetRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public EbayListingService(CardRepository cardRepository,
                              EbayTokenService ebayTokenService,
                              EbayImageService ebayImageService,
                              ShippingPresetRepository shippingPresetRepository) {
        this.cardRepository = cardRepository;
        this.ebayTokenService = ebayTokenService;
        this.ebayImageService = ebayImageService;
        this.shippingPresetRepository = shippingPresetRepository;
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
            params.setShippingPresetId(override.getShippingPresetId() != null ? override.getShippingPresetId() : defaults.getShippingPresetId());
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
        } catch (IllegalArgumentException e) {
            return new ListCardResponse(cardId, card.getCardName(), e.getMessage(), true);
        } catch (Exception e) {
            return new ListCardResponse(cardId, card.getCardName(), "Failed to create inventory item: " + e.getMessage(), true);
        }

        String fulfillmentPolicyId;
        try {
            fulfillmentPolicyId = resolveFulfillmentPolicyId(request.getShippingPresetId());
        } catch (IllegalArgumentException e) {
            return new ListCardResponse(cardId, card.getCardName(), e.getMessage(), true);
        }

        String listingId;
        try {
            listingId = createAndPublishOffer(sku, request, fulfillmentPolicyId);
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

    private String createAndPublishOffer(String sku, ListCardRequest request, String fulfillmentPolicyId) throws Exception {
        String format = request.getFormat() != null ? request.getFormat() : "FIXED_PRICE";

        String offerBody = """
                {
                  "sku": "%s",
                  "marketplaceId": "%s",
                  "format": "%s",
                  "listingPolicies": {
                    "fulfillmentPolicyId": "%s",
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
                """.formatted(sku, ebayTokenService.getMarketplaceId(), format, fulfillmentPolicyId, request.getPrice());

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

    public List<ActiveListingResponse> getActiveListings() {
        List<Card> cards = cardRepository.findByStatus(CardStatus.LISTED);
        List<ActiveListingResponse> response = new ArrayList<>();
        for (Card card : cards) {
            response.add(new ActiveListingResponse(card));
        }
        return response;
    }

    private String resolveFulfillmentPolicyId(Long shippingPresetId) {
        if (shippingPresetId == null) {
            throw new IllegalArgumentException("shippingPresetId is required");
        }
        return shippingPresetRepository.findById(shippingPresetId)
                .orElseThrow(() -> new IllegalArgumentException("Shipping preset not found: " + shippingPresetId))
                .getEbayPolicyId();
    }

    // Maps condition shorthand to eBay's Ungraded Card Condition descriptor IDs for category 183454.
    // conditionDescriptors replace aspects for trading card categories — do not use aspect strings.
    private String cardConditionToDescriptorId(String cardCondition) {
        return switch (cardCondition.toUpperCase()) {
            case "NM" -> "400010";
            case "LP" -> "400015";
            case "MP" -> "400016";
            case "HP" -> "400017";
            default -> throw new IllegalArgumentException(
                    "Invalid condition '" + cardCondition + "'. Accepted values: NM, LP, MP, HP");
        };
    }

    public UpdateListingResponse updateListing(Long cardId, UpdateListingRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found: " + cardId));

        if (card.getStatus() != CardStatus.LISTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Card is not LISTED (current status: " + card.getStatus() + ")");
        }

        if (request.getPrice() == null && request.getCondition() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At least one of 'price' or 'condition' must be provided");
        }

        String descriptorId = null;
        if (request.getCondition() != null) {
            try {
                descriptorId = cardConditionToDescriptorId(request.getCondition());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        }

        String sku = "CARD-" + cardId;

        String offerId;
        try {
            offerId = getOfferIdBySku(sku);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to look up eBay offer: " + e.getMessage());
        }

        if (descriptorId != null) {
            try {
                updateInventoryItemCondition(sku, descriptorId);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Failed to update eBay inventory item: " + e.getMessage());
            }
        }

        if (request.getPrice() != null) {
            try {
                updateOfferPrice(offerId, request.getPrice());
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Failed to update eBay offer: " + e.getMessage());
            }
        }

        try {
            republishOffer(offerId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to republish eBay offer: " + e.getMessage());
        }

        return new UpdateListingResponse(card, request.getPrice(), request.getCondition());
    }

    private String getOfferIdBySku(String sku) throws Exception {
        String url = ebayTokenService.getBaseUrl() + "/sell/inventory/v1/offer?sku=" + sku;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Authorization", ebayTokenService.getBearerToken())
                .header("X-EBAY-C-MARKETPLACE-ID", ebayTokenService.getMarketplaceId())
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            throw new RuntimeException("eBay get offer by SKU failed (" + resp.statusCode() + "): " + resp.body());
        }

        JsonNode offers = objectMapper.readTree(resp.body()).path("offers");
        if (!offers.isArray() || offers.isEmpty()) {
            throw new RuntimeException("No eBay offer found for SKU: " + sku);
        }
        return offers.get(0).get("offerId").asText();
    }

    // eBay PUT replaces the entire resource — we GET first, mutate the condition, and PUT back
    // so we don't accidentally clear images, aspects, availability, etc.
    private void updateInventoryItemCondition(String sku, String descriptorId) throws Exception {
        String url = ebayTokenService.getBaseUrl() + "/sell/inventory/v1/inventory_item/" + sku;

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Authorization", ebayTokenService.getBearerToken())
                .header("X-EBAY-C-MARKETPLACE-ID", ebayTokenService.getMarketplaceId())
                .build();

        HttpResponse<String> getResp = httpClient.send(getReq, HttpResponse.BodyHandlers.ofString());

        if (getResp.statusCode() != 200) {
            throw new RuntimeException("eBay get inventory item failed (" + getResp.statusCode() + "): " + getResp.body());
        }

        ObjectNode item = (ObjectNode) objectMapper.readTree(getResp.body());

        ArrayNode descriptors = objectMapper.createArrayNode();
        ObjectNode descriptor = objectMapper.createObjectNode();
        descriptor.put("name", "40001");
        descriptor.set("values", objectMapper.createArrayNode().add(descriptorId));
        descriptors.add(descriptor);
        item.set("conditionDescriptors", descriptors);

        HttpRequest putReq = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(item)))
                .header("Authorization", ebayTokenService.getBearerToken())
                .header("Content-Type", "application/json")
                .header("Content-Language", "en-US")
                .header("X-EBAY-C-MARKETPLACE-ID", ebayTokenService.getMarketplaceId())
                .build();

        HttpResponse<String> putResp = httpClient.send(putReq, HttpResponse.BodyHandlers.ofString());

        if (putResp.statusCode() != 200 && putResp.statusCode() != 204) {
            throw new RuntimeException("eBay update inventory item failed (" + putResp.statusCode() + "): " + putResp.body());
        }
    }

    private void updateOfferPrice(String offerId, Double newPrice) throws Exception {
        String url = ebayTokenService.getBaseUrl() + "/sell/inventory/v1/offer/" + offerId;

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Authorization", ebayTokenService.getBearerToken())
                .header("X-EBAY-C-MARKETPLACE-ID", ebayTokenService.getMarketplaceId())
                .build();

        HttpResponse<String> getResp = httpClient.send(getReq, HttpResponse.BodyHandlers.ofString());

        if (getResp.statusCode() != 200) {
            throw new RuntimeException("eBay get offer failed (" + getResp.statusCode() + "): " + getResp.body());
        }

        ObjectNode offer = (ObjectNode) objectMapper.readTree(getResp.body());

        ObjectNode pricingSummary = offer.has("pricingSummary") && offer.get("pricingSummary").isObject()
                ? (ObjectNode) offer.get("pricingSummary")
                : offer.putObject("pricingSummary");
        ObjectNode price = pricingSummary.has("price") && pricingSummary.get("price").isObject()
                ? (ObjectNode) pricingSummary.get("price")
                : pricingSummary.putObject("price");
        price.put("value", String.format("%.2f", newPrice));
        price.put("currency", "USD");

        HttpRequest putReq = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(offer)))
                .header("Authorization", ebayTokenService.getBearerToken())
                .header("Content-Type", "application/json")
                .header("Content-Language", "en-US")
                .header("X-EBAY-C-MARKETPLACE-ID", ebayTokenService.getMarketplaceId())
                .build();

        HttpResponse<String> putResp = httpClient.send(putReq, HttpResponse.BodyHandlers.ofString());

        if (putResp.statusCode() != 200 && putResp.statusCode() != 204) {
            throw new RuntimeException("eBay update offer failed (" + putResp.statusCode() + "): " + putResp.body());
        }
    }

    private void republishOffer(String offerId) throws Exception {
        String url = ebayTokenService.getBaseUrl() + "/sell/inventory/v1/offer/" + offerId + "/publish";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Authorization", ebayTokenService.getBearerToken())
                .header("Content-Type", "application/json")
                .header("X-EBAY-C-MARKETPLACE-ID", ebayTokenService.getMarketplaceId())
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            throw new RuntimeException("eBay publish offer failed (" + resp.statusCode() + "): " + resp.body());
        }
    }
}
