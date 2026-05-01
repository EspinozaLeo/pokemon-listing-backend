package com.pokemonlisting.controller;

import com.pokemonlisting.service.EbayOAuthService;
import com.pokemonlisting.service.EbayTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ebay")
public class EbayAuthController {

    private final EbayOAuthService ebayOAuthService;
    private final EbayTokenService ebayTokenService;

    public EbayAuthController(EbayOAuthService ebayOAuthService,
                               EbayTokenService ebayTokenService) {
        this.ebayOAuthService = ebayOAuthService;
        this.ebayTokenService = ebayTokenService;
    }

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize() {
        String url = ebayOAuthService.buildAuthorizationUrl();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", url);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam String code) {
        try {
            ebayOAuthService.exchangeCodeForTokens(code);
            ebayTokenService.setTokenExpiry(7200);
            return ResponseEntity.ok("eBay tokens saved successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Token exchange failed: " + e.getMessage());
        }
    }
}
