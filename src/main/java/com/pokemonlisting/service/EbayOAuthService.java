package com.pokemonlisting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemonlisting.config.EbayCredentialsConfig;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class EbayOAuthService {

    private final EbayCredentialsConfig ebayCredentialsConfig;

    public EbayOAuthService(EbayCredentialsConfig ebayCredentialsConfig) {
        this.ebayCredentialsConfig = ebayCredentialsConfig;
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

    // TODO 1: Exchange the authorization code for an access token and refresh token.
    // eBay token endpoint:
    //   Sandbox:    https://api.sandbox.ebay.com/identity/v1/oauth2/token
    //   Production: https://api.ebay.com/identity/v1/oauth2/token
    //
    // Request format:
    //   - Method: POST
    //   - Content-Type: application/x-www-form-urlencoded
    //   - Authorization: Basic base64(appId:certId)
    //     Hint: Base64.getEncoder().encodeToString((appId + ":" + certId).getBytes())
    //   - Body: grant_type=authorization_code&code={code}&redirect_uri={ruName}
    //
    // Parse the JSON response for "access_token" and "refresh_token" fields.
    // Then call ebayCredentialsConfig.saveTokens(accessToken, refreshToken) to persist them.
    //
    // Hint: use HttpClient + HttpRequest like in EbayListingService
    // Hint: use ObjectMapper to parse the JSON response
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

        ebayCredentialsConfig.saveTokens(accessToken, refreshToken);
    }
}
