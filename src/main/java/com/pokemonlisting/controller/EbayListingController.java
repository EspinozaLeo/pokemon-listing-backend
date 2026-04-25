package com.pokemonlisting.controller;

import com.pokemonlisting.dto.ListCardRequest;
import com.pokemonlisting.dto.ListCardResponse;
import com.pokemonlisting.service.EbayListingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
public class EbayListingController {

    private final EbayListingService ebayListingService;

    public EbayListingController(EbayListingService ebayListingService) {
        this.ebayListingService = ebayListingService;
    }

    @PostMapping("/{id}/list")
    public ResponseEntity<ListCardResponse> listCard(@PathVariable Long id,
                                                     @RequestBody ListCardRequest request) {
        ListCardResponse response = ebayListingService.listCard(id, request);
        if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

}
