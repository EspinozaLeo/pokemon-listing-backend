package com.pokemonlisting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pokemonlisting.dto.ShippingPresetRequest;
import com.pokemonlisting.dto.ShippingPresetResponse;
import com.pokemonlisting.dto.ShippingServiceOption;
import com.pokemonlisting.model.ShippingPreset;
import com.pokemonlisting.repository.ShippingPresetRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;

@Service
public class ShippingService {

    private final ShippingPresetRepository shippingPresetRepository;
    private final EbayTokenService ebayTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private List<ShippingServiceOption> servicesCache = new ArrayList<>();

    public ShippingService(ShippingPresetRepository shippingPresetRepository,
                           EbayTokenService ebayTokenService) {
        this.shippingPresetRepository = shippingPresetRepository;
        this.ebayTokenService = ebayTokenService;
    }

    @PostConstruct
    public void init() {
        try {
            fetchAndCacheServices();
        } catch (Exception e) {
            System.out.println("WARNING: Could not load shipping services from eBay at startup: " + e.getMessage());
        }
    }

    public List<ShippingPresetResponse> getPresets() {
        return shippingPresetRepository.findAll().stream()
                .map(ShippingPresetResponse::new)
                .toList();
    }

    public ShippingPresetResponse createPreset(ShippingPresetRequest request) {
        double cost = Boolean.TRUE.equals(request.getFreeShipping()) ? 0.00 : request.getShippingCost();
        int handlingTime = request.getHandlingTimeDays() != null ? request.getHandlingTimeDays() : 1;
        String costType = request.getCostType() != null ? request.getCostType() : "FLAT_RATE";

        ObjectNode root = objectMapper.createObjectNode();

        ArrayNode categoryTypes = objectMapper.createArrayNode();
        ObjectNode categoryType = objectMapper.createObjectNode();
        categoryType.put("name", "ALL_EXCLUDING_MOTORS_VEHICLES");
        categoryTypes.add(categoryType);
        root.set("categoryTypes", categoryTypes);

        root.put("marketplaceId", ebayTokenService.getMarketplaceId());
        root.put("name", request.getName());

        ObjectNode handlingTimeNode = objectMapper.createObjectNode();
        handlingTimeNode.put("unit", "DAY");
        handlingTimeNode.put("value", handlingTime);
        root.set("handlingTime", handlingTimeNode);

        ObjectNode shippingService = objectMapper.createObjectNode();
        shippingService.put("shippingCarrierCode", request.getShippingCarrierCode());
        shippingService.put("shippingServiceCode", request.getShippingServiceCode());
        shippingService.put("freeShipping", Boolean.TRUE.equals(request.getFreeShipping()));
        shippingService.put("sortOrder", 1);

        ObjectNode shippingCostNode = objectMapper.createObjectNode();
        shippingCostNode.put("value", String.format("%.2f", cost));
        shippingCostNode.put("currency", "USD");
        shippingService.set("shippingCost", shippingCostNode);

        if (request.getInsuranceAmount() != null) {
            ObjectNode insuranceFeeNode = objectMapper.createObjectNode();
            insuranceFeeNode.put("value", String.format("%.2f", request.getInsuranceAmount()));
            insuranceFeeNode.put("currency", "USD");
            shippingService.set("insuranceFee", insuranceFeeNode);
        }

        ObjectNode shippingOption = objectMapper.createObjectNode();
        shippingOption.put("costType", costType);
        shippingOption.put("optionType", "DOMESTIC");
        shippingOption.set("shippingServices", objectMapper.createArrayNode().add(shippingService));

        root.set("shippingOptions", objectMapper.createArrayNode().add(shippingOption));

        String body;
        try {
            body = objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize fulfillment policy payload: " + e.getMessage());
        }

        String url = ebayTokenService.getBaseUrl() + "/sell/account/v1/fulfillment_policy";

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", ebayTokenService.getBearerToken())
                .header("Content-Type", "application/json")
                .header("X-EBAY-C-MARKETPLACE-ID", ebayTokenService.getMarketplaceId())
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to reach eBay fulfillment policy API: " + e.getMessage());
        }

        String ebayPolicyId;
        try {
            if (response.statusCode() == 201) {
                ebayPolicyId = objectMapper.readTree(response.body()).get("fulfillmentPolicyId").asText();
            } else if (response.statusCode() == 400) {
                JsonNode errors = objectMapper.readTree(response.body()).path("errors");
                String duplicateId = null;
                for (JsonNode error : errors) {
                    if (error.path("errorId").asInt() == 20400) {
                        for (JsonNode param : error.path("parameters")) {
                            if ("DuplicateProfileId".equals(param.path("name").asText())) {
                                duplicateId = param.path("value").asText();
                            }
                        }
                    }
                }
                if (duplicateId == null) {
                    throw new RuntimeException("eBay fulfillment policy creation failed (400): " + response.body());
                }
                ebayPolicyId = duplicateId;
            } else {
                throw new RuntimeException("eBay fulfillment policy creation failed (" + response.statusCode() + "): " + response.body());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse eBay fulfillment policy response: " + e.getMessage());
        }

        ShippingPreset preset = new ShippingPreset();
        preset.setName(request.getName());
        preset.setShippingCarrierCode(request.getShippingCarrierCode());
        preset.setShippingServiceCode(request.getShippingServiceCode());
        preset.setShippingCost(cost);
        preset.setFreeShipping(request.getFreeShipping());
        preset.setHandlingTimeDays(handlingTime);
        preset.setCostType(costType);
        preset.setInsuranceAmount(request.getInsuranceAmount());
        preset.setEbayPolicyId(ebayPolicyId);

        ShippingPreset saved = shippingPresetRepository.save(preset);
        return new ShippingPresetResponse(saved);
    }

    public void deletePreset(Long id) {
        ShippingPreset preset = shippingPresetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shipping preset not found: " + id));

        String deleteUrl = ebayTokenService.getBaseUrl() + "/sell/account/v1/fulfillment_policy/" + preset.getEbayPolicyId();

        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(deleteUrl))
                .DELETE()
                .header("Authorization", ebayTokenService.getBearerToken())
                .header("X-EBAY-C-MARKETPLACE-ID", ebayTokenService.getMarketplaceId())
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to reach eBay fulfillment policy API: " + e.getMessage());
        }

        if (response.statusCode() != 204) {
            throw new RuntimeException("eBay fulfillment policy deletion failed (" + response.statusCode() + "): " + response.body());
        }

        shippingPresetRepository.deleteById(id);
    }

    public List<ShippingServiceOption> getAvailableServices() {
        return servicesCache;
    }

    private void fetchAndCacheServices() throws Exception {
        String url = ebayTokenService.getBaseUrl() + "/sell/metadata/v1/shipping/marketplace/EBAY_US/get_shipping_services";

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Authorization", ebayTokenService.getBearerToken())
                .header("X-EBAY-C-MARKETPLACE-ID", ebayTokenService.getMarketplaceId())
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("eBay shipping metadata API failed (" + response.statusCode() + "): " + response.body());
        }

        List<ShippingServiceOption> result = new ArrayList<>();
        JsonNode root = objectMapper.readTree(response.body());

        for (JsonNode service : root.path("shippingServices")) {
            if (!service.path("validForSellingFlow").asBoolean()) continue;
            if (service.path("internationalService").asBoolean()) continue;
            if (service.path("shippingCarrier").isNull() || service.path("shippingCarrier").isMissingNode()) continue;

            String carrierCode = service.path("shippingCarrier").asText(null);
            String serviceCode = service.path("shippingService").asText(null);
            Integer minShippingTime = service.path("minShippingTime").asInt();
            Integer maxShippingTime = service.path("maxShippingTime").asInt();

            result.add(new ShippingServiceOption(carrierCode, serviceCode, minShippingTime, maxShippingTime));
        }

        servicesCache = result;
    }
}
