package com.pokemonlisting.service;

import com.pokemonlisting.config.EbayCredentialsConfig;
import com.pokemonlisting.model.EbayToken;
import com.pokemonlisting.repository.EbayTokenRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class EbayTokenService {

    private final EbayCredentialsConfig ebayCredentialsConfig;
    private final EbayOAuthService ebayOAuthService;
    private final EbayTokenRepository ebayTokenRepository;

    private Instant tokenExpiresAt = null;

    public EbayTokenService(EbayCredentialsConfig ebayCredentialsConfig,
                            EbayOAuthService ebayOAuthService,
                            EbayTokenRepository ebayTokenRepository) {
        this.ebayCredentialsConfig = ebayCredentialsConfig;
        this.ebayOAuthService = ebayOAuthService;
        this.ebayTokenRepository = ebayTokenRepository;
    }

    @PostConstruct
    public void init() {
        ebayTokenRepository.findByUserId("default").ifPresent(token ->
            this.tokenExpiresAt = token.getExpiresAt()
        );
    }

    public synchronized String getBearerToken() {
        if (tokenExpiresAt != null && Instant.now().isAfter(tokenExpiresAt.minusSeconds(300))) {
            try {
                long expiresIn = ebayOAuthService.refreshAccessToken();
                tokenExpiresAt = Instant.now().plusSeconds(expiresIn);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("401")) {
                    throw new RuntimeException("eBay refresh token expired. Re-authorize at GET /api/ebay/authorize");
                }
                System.err.println("Warning: eBay token refresh failed — using existing token. " + e.getMessage());
            }
        }

        return "Bearer " + ebayTokenRepository.findByUserId("default")
                .map(EbayToken::getAccessToken)
                .orElseThrow(() -> new RuntimeException("No eBay token found. Authorize at GET /api/ebay/authorize"));
    }

    public String getBaseUrl() {
        if (ebayCredentialsConfig.isSandbox()) return "https://api.sandbox.ebay.com";
        else return "https://api.ebay.com";
    }

    public String getMarketplaceId() {
        return "EBAY_US";
    }
}
