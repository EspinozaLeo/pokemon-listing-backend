package com.pokemonlisting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemonlisting.model.CardImage;
import com.pokemonlisting.model.UploadedImage;
import com.pokemonlisting.repository.CardImageRepository;
import com.pokemonlisting.repository.UploadedImageRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EbayImageService {

    private final CardImageRepository cardImageRepository;
    private final UploadedImageRepository uploadedImageRepository;
    private final EbayTokenService ebayTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public EbayImageService(CardImageRepository cardImageRepository,
                            UploadedImageRepository uploadedImageRepository,
                            EbayTokenService ebayTokenService) {
        this.cardImageRepository = cardImageRepository;
        this.uploadedImageRepository = uploadedImageRepository;
        this.ebayTokenService = ebayTokenService;
    }

    /**
     * Returns eBay-hosted image URLs for a card.
     * Tries to upload each image to eBay Picture Services first.
     * Falls back to the local/ngrok URL if upload fails.
     */
    public List<String> getImageUrls(Long cardId) {
        List<CardImage> cardImages = cardImageRepository.findByCardIdOrderByDisplayOrderAsc(cardId);
        List<String> urls = new ArrayList<>();

        for (CardImage cardImage : cardImages) {
            Optional<UploadedImage> uploadedImage = uploadedImageRepository.findById(cardImage.getUploadedImageId());
            if (uploadedImage.isEmpty()) {
                System.err.println("Warning: no UploadedImage found for CardImage id=" + cardImage.getId());
                continue;
            }

            UploadedImage image = uploadedImage.get();

            try {
                String ebayUrl = uploadImageToEbay(image);
                urls.add(ebayUrl);
            } catch (Exception e) {
                System.err.println("Warning: eBay image upload failed for image id=" + image.getId()
                        + ", skipping. Reason: " + e.getMessage());
            }
        }

        return urls;
    }

    /**
     * Uploads a single image file to eBay Picture Services via the Media API
     * and returns the eBay-hosted EPS image URL.
     *
     * Docs: https://developer.ebay.com/api-docs/sell/media/overview.html
     * Endpoint: POST /commerce/media/v1_beta/image/create_image_from_file
     * Format: multipart/form-data, key "image" = file bytes
     * Response: 201 Created — body has { "imageUrl": "...", "expirationDate": "..." }
     *
     * Note: the eBay Media API uses a different host than the Inventory API.
     * Inventory API: api.ebay.com / api.sandbox.ebay.com
     * Media API:     apim.ebay.com / apim.sandbox.ebay.com
     * The mediaApiBaseUrl below is built from ebayTokenService.getBaseUrl() by replacing
     * "api." with "apim." — verify this against the sandbox docs if uploads return 404/401.
     */
    private String uploadImageToEbay(UploadedImage image) throws Exception {
        byte[] fileBytes = Files.readAllBytes(Path.of(image.getFilePath()));
        String contentType = (image.getContentType() != null) ? image.getContentType() : "image/jpeg";

        // Build multipart/form-data body manually — Java's HttpClient has no built-in multipart support.
        String boundary = "EbayImageUpload" + System.currentTimeMillis();
        String CRLF = "\r\n";

        byte[] headerBytes = (
                "--" + boundary + CRLF
                + "Content-Disposition: form-data; name=\"image\"; filename=\"" + image.getSavedFilename() + "\"" + CRLF
                + "Content-Type: " + contentType + CRLF
                + CRLF
        ).getBytes(StandardCharsets.UTF_8);

        byte[] footerBytes = (CRLF + "--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8);

        byte[] body = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(fileBytes,   0, body, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);

        // Media API host differs from the Inventory API host (apim vs api).
        String mediaBaseUrl = ebayTokenService.getBaseUrl().replace("://api.", "://apim.");
        String uploadUrl = mediaBaseUrl + "/commerce/media/v1_beta/image/create_image_from_file";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .header("Authorization", ebayTokenService.getBearerToken())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("X-EBAY-C-MARKETPLACE-ID", ebayTokenService.getMarketplaceId())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 201) {
            throw new RuntimeException("eBay image upload failed (" + response.statusCode() + "): " + response.body());
        }

        // Response body: { "imageUrl": "<EPS URL>", "expirationDate": "<ISO date>" }
        // imageUrl is the URL to pass into product.imageUrls in createInventoryItem.
        // The Location header also contains the image ID URI — not stored here but can be added later.
        JsonNode json = objectMapper.readTree(response.body());
        String imageUrl = json.path("imageUrl").asText(null);

        if (imageUrl == null || imageUrl.isBlank()) {
            throw new RuntimeException("eBay image upload response missing imageUrl: " + response.body());
        }

        return imageUrl;
    }
}
