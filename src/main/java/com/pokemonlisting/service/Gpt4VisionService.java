package com.pokemonlisting.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Service
public class Gpt4VisionService {

    private static final Logger log = LoggerFactory.getLogger(Gpt4VisionService.class);

    private final String openAiKeyPath;

    public Gpt4VisionService(
            @Value("${openai.api.key.path}") String openAiKeyPath
    ) {
        this.openAiKeyPath = openAiKeyPath;
    }

    /**
     * CardData is the inner DTO that holds the parsed card details
     * returned by GPT-4V. All fields can be null if GPT-4V could not
     * identify them.
     */
    public static class CardData {
        private final String cardName;
        private final String setName;
        private final String cardNumber;
        private final String rarity;

        public CardData(String cardName, String setName, String cardNumber, String rarity) {
            this.cardName = cardName;
            this.setName = setName;
            this.cardNumber = cardNumber;
            this.rarity = rarity;
        }

        public String getCardName() { return cardName; }
        public String getSetName() { return setName; }
        public String getCardNumber() { return cardNumber; }
        public String getRarity() { return rarity; }
    }

    /**
     * identifyCard(imagePath) sends the image at the given path to
     * OpenAI GPT-4 Vision and parses the response into a CardData DTO.
     * Returns null if the call fails or the response cannot be parsed.
     * Every call is logged for cost tracking (est. $0.01/call).
     *
     * @param imagePath Absolute path to the card image file
     * @return CardData with cardName, setName, cardNumber, rarity — or null on failure
     */
    public CardData identifyCard(String imagePath) {
        log.info("GPT-4V identify called for image: {}", imagePath);

        String openAIKey;
        try{
            openAIKey = Files.readString(Path.of(openAiKeyPath)).trim();
        } catch(IOException e) {
            log.warn("Failed to read OpenAI API key from file: {}", e.getMessage());
            return null;
        }
        
        String encodedString;
        Path path = Paths.get(imagePath);
        if(!Files.exists(path)){
            throw new RuntimeException("File not found: " + imagePath);
        }

        try{
            byte[] imageData = Files.readAllBytes((path));
            encodedString = Base64.getEncoder().encodeToString(imageData);
        } catch(IOException e){
            log.warn("Failed to encode file: {}", e.getMessage());
            return null;
        }

        String requestBody = String.format(
            "{" +
                "\"model\": \"gpt-4o\"," +
                "\"max_tokens\": 300," +
                "\"messages\": [{" +
                    "\"role\": \"user\"," +
                    "\"content\": [" +
                        "{" +
                            "\"type\": \"image_url\"," +
                            "\"image_url\": {\"url\": \"data:image/jpeg;base64,%s\"}" +
                        "}," +
                        "{" +
                            "\"type\": \"text\"," +
                            "\"text\": \"This is a Pokemon trading card. Respond ONLY with a raw JSON object, no markdown, no code blocks. Use these exact keys: cardName, setName, cardNumber, rarity. Example: {\\\"cardName\\\":\\\"Pikachu\\\",\\\"setName\\\":\\\"Scarlet & Violet\\\",\\\"cardNumber\\\":\\\"001/198\\\",\\\"rarity\\\":\\\"Common\\\"}\"" +
                        "}" +
                    "]" +
                "}]" +
            "}",
            encodedString
        );

        String responseBody;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + openAIKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            responseBody = response.body();
        } catch (Exception e) {
            log.warn("GPT-4V request failed: {}", e.getMessage());
            return null;
        }

        try {
            // Extract the content string from choices[0].message.content
            // Handle both "content":"..." and "content": "..." (with space)
            String contentMarker = responseBody.contains("\"content\":\"")
                    ? "\"content\":\""
                    : "\"content\": \"";
            int contentStart = responseBody.indexOf(contentMarker);
            if (contentStart == -1) {
                log.warn("GPT-4V response missing content field. Response: {}", responseBody);
                return null;
            }
            contentStart += contentMarker.length();

            // Walk forward to find the closing quote, skipping escaped characters
            int contentEnd = contentStart;
            while (contentEnd < responseBody.length()) {
                char c = responseBody.charAt(contentEnd);
                if (c == '\\') {
                    contentEnd += 2;
                } else if (c == '"') {
                    break;
                } else {
                    contentEnd++;
                }
            }

            // Unescape the inner JSON string GPT-4V returned
            String contentJson = responseBody.substring(contentStart, contentEnd)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "")
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            String cardName   = extractJsonValue(contentJson, "cardName");
            String setName    = extractJsonValue(contentJson, "setName");
            String cardNumber = extractJsonValue(contentJson, "cardNumber");
            String rarity     = extractJsonValue(contentJson, "rarity");

            log.info("GPT-4V identified: {} / {} / {} / {}", cardName, setName, cardNumber, rarity);
            return new CardData(cardName, setName, cardNumber, rarity);

        } catch (Exception e) {
            log.warn("Failed to parse GPT-4V response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * extractJsonValue(json, key) extracts a string value from a simple JSON object.
     * Only works for string values — does not handle nested objects or arrays.
     *
     * @param json Raw JSON string
     * @param key  The key whose value to extract
     * @return The string value, or null if key not found
     */
    private String extractJsonValue(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start == -1) return null;
        start += marker.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}
