package com.pokemonlisting.dto;

import com.pokemonlisting.model.Card;

public class UpdateListingResponse {

    private Long cardId;
    private String ebayListingId;
    private String ebayListingUrl;
    private Double updatedPrice;
    private String updatedCondition;

    public UpdateListingResponse(Card card, Double updatedPrice, String updatedCondition) {
        this.cardId = card.getId();
        this.ebayListingId = card.getEbayListingId();
        // TODO (prod switchover): build URL from EbayTokenService.getBaseUrl() instead of hardcoding sandbox domain
        this.ebayListingUrl = "https://www.sandbox.ebay.com/itm/" + card.getEbayListingId();
        this.updatedPrice = updatedPrice;
        this.updatedCondition = updatedCondition;
    }

    public Long getCardId() { return cardId; }
    public String getEbayListingId() { return ebayListingId; }
    public String getEbayListingUrl() { return ebayListingUrl; }
    public Double getUpdatedPrice() { return updatedPrice; }
    public String getUpdatedCondition() { return updatedCondition; }
}
