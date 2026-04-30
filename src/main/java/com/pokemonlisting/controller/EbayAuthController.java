package com.pokemonlisting.controller;

import com.pokemonlisting.service.EbayOAuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ebay")
public class EbayAuthController {

    private final EbayOAuthService ebayOAuthService;

    public EbayAuthController(EbayOAuthService ebayOAuthService) {
        this.ebayOAuthService = ebayOAuthService;
    }

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize() {
        String url = ebayOAuthService.buildAuthorizationUrl();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", url);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
