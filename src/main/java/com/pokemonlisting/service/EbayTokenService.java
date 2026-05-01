package com.pokemonlisting.service;

import com.pokemonlisting.config.EbayCredentialsConfig;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class EbayTokenService {

    private final EbayCredentialsConfig ebayCredentialsConfig;
    private final EbayOAuthService ebayOAuthService;

    // Tracks when the current access token expires in memory.
    // Null means we haven't set an expiry yet (e.g. app just started with a manually pasted token).
    private Instant tokenExpiresAt = null;

    public EbayTokenService(EbayCredentialsConfig ebayCredentialsConfig,
                            EbayOAuthService ebayOAuthService) {
        this.ebayCredentialsConfig = ebayCredentialsConfig;
        this.ebayOAuthService = ebayOAuthService;
    }

    public String getBearerToken() {
        if (tokenExpiresAt != null && Instant.now().isAfter(tokenExpiresAt.minusSeconds(300))) {
            try {
                long expiresIn = ebayOAuthService.refreshAccessToken();
                tokenExpiresAt = Instant.now().plusSeconds(expiresIn);
            } catch (Exception e) {
                System.err.println("Warning: eBay token refresh failed — using existing token. " + e.getMessage());
            }
        }
        String token = ebayCredentialsConfig.getAccessToken();
        if (token == null) token = ebayCredentialsConfig.getUserToken();
        return "Bearer " + token;
    }

    // Called after a successful token exchange (TLS-47) to set the initial expiry timer.
    // eBay access tokens expire in 7200 seconds (2 hours).
    public void setTokenExpiry(long expiresInSeconds) {
        this.tokenExpiresAt = Instant.now().plusSeconds(expiresInSeconds);
    }

    public String getBaseUrl() {
        if (ebayCredentialsConfig.isSandbox()) return "https://api.sandbox.ebay.com";
        else return "https://api.ebay.com";
    }

    public String getMarketplaceId() {
        return "EBAY_US";
    }
}
