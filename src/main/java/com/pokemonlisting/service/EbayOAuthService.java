package com.pokemonlisting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemonlisting.config.EbayCredentialsConfig;
import com.pokemonlisting.model.EbayToken;
import com.pokemonlisting.repository.EbayTokenRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class EbayOAuthService {

    private final EbayCredentialsConfig ebayCredentialsConfig;
    private final EbayTokenRepository ebayTokenRepository;

    public EbayOAuthService(EbayCredentialsConfig ebayCredentialsConfig,
                            EbayTokenRepository ebayTokenRepository) {
        this.ebayCredentialsConfig = ebayCredentialsConfig;
        this.ebayTokenRepository = ebayTokenRepository;
    }

    public String buildAuthorizationUrl() {
        String baseUrl = ebayCredentialsConfig.isSandbox()
                ? "https://auth.sandbox.ebay.com/oauth2/authorize"
                : "https://auth.ebay.com/oauth2/authorize";

        String scopes = "https://api.ebay.com/oauth/api_scope/sell.inventory "
                      + "https://api.ebay.com/oauth/api_scope/sell.account";

        String encodedScopes = java.net.URLEncoder.encode(scopes, java.nio.charset.StandardCharsets.UTF_8);

        return String.format("%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s",
                baseUrl,
                ebayCredentialsConfig.getAppId(),
                ebayCredentialsConfig.getRuName(),
                encodedScopes);
    }

    public void exchangeCodeForTokens(String code) throws Exception {
        String tokenUrl = ebayCredentialsConfig.isSandbox()
                ? "https://api.sandbox.ebay.com/identity/v1/oauth2/token"
                : "https://api.ebay.com/identity/v1/oauth2/token";

        String credentials = ebayCredentialsConfig.getAppId() + ":" + ebayCredentialsConfig.getCertId();
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        String body = "grant_type=authorization_code"
                + "&code=" + code
                + "&redirect_uri=" + ebayCredentialsConfig.getRuName();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", basicAuth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("eBay token exchange failed (" + response.statusCode() + "): " + response.body());
        }

        JsonNode json = new ObjectMapper().readTree(response.body());
        String accessToken = json.get("access_token").asText();
        String refreshToken = json.get("refresh_token").asText();
        long expiresIn = json.get("expires_in").asLong();

        EbayToken token = ebayTokenRepository.findByUserId("default")
                .orElse(new EbayToken());
        token.setUserId("default");
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setExpiresAt(Instant.now().plusSeconds(expiresIn));
        token.setUpdatedAt(Instant.now());
        ebayTokenRepository.save(token);
    }

    public long refreshAccessToken() throws Exception {
        String tokenUrl = ebayCredentialsConfig.isSandbox()
                ? "https://api.sandbox.ebay.com/identity/v1/oauth2/token"
                : "https://api.ebay.com/identity/v1/oauth2/token";

        String credentials = ebayCredentialsConfig.getAppId() + ":" + ebayCredentialsConfig.getCertId();
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        String refreshToken = ebayTokenRepository.findByUserId("default")
                .map(EbayToken::getRefreshToken)
                .orElseThrow(() -> new RuntimeException("No eBay token found. Authorize at GET /api/ebay/authorize"));

        String body = "grant_type=refresh_token"
                + "&refresh_token=" + refreshToken;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", basicAuth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            throw new RuntimeException("eBay refresh token expired (401). Re-authorize at GET /api/ebay/authorize");
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("eBay token refresh failed (" + response.statusCode() + "): " + response.body());
        }

        JsonNode json = new ObjectMapper().readTree(response.body());
        String newAccessToken = json.get("access_token").asText();
        long expiresIn = json.get("expires_in").asLong();

        EbayToken token = ebayTokenRepository.findByUserId("default").orElseThrow();
        token.setAccessToken(newAccessToken);
        token.setExpiresAt(Instant.now().plusSeconds(expiresIn));
        token.setUpdatedAt(Instant.now());
        ebayTokenRepository.save(token);

        return expiresIn;
    }
}
