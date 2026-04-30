package com.pokemonlisting.service;

import com.pokemonlisting.config.EbayCredentialsConfig;
import org.springframework.stereotype.Service;

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
}
