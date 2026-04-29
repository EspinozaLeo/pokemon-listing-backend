package com.pokemonlisting.controller;

import com.pokemonlisting.dto.BatchListRequest;
import com.pokemonlisting.dto.BatchListResponse;
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

    // TODO 7: Add a POST /api/cards/list-batch endpoint
    // It should accept a @RequestBody BatchListRequest and return ResponseEntity<BatchListResponse>
    // Call ebayListingService.listCards(request)
    // Always return 200 OK — partial failures are captured inside BatchListResponse.results
    // Hint:
    //   @PostMapping("/list-batch")
    //   public ResponseEntity<BatchListResponse> listCards(@RequestBody BatchListRequest request) { ... }

}
