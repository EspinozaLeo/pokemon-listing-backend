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

    // TODO 2: Implement the GET /api/ebay/authorize endpoint.
    // Call ebayOAuthService.buildAuthorizationUrl() to get the URL.
    // Redirect the user to that URL using a 302 response.
    //
    // Hint: To redirect with ResponseEntity:
    //   HttpHeaders headers = new HttpHeaders();
    //   headers.add("Location", url);
    //   return new ResponseEntity<>(headers, HttpStatus.FOUND);   // 302 FOUND = redirect
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize() {
        String url = ebayOAuthService.buildAuthorizationUrl();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", url);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
