package com.pokemonlisting.dto;

import com.pokemonlisting.model.Card;
import com.pokemonlisting.model.CardStatus;

public class ActiveListingResponse {

    private Long cardId;
    private String cardName;
    private String cardNumber;
    private String setName;
    private String ebayListingId;
    private String ebayListingUrl;
    private CardStatus status;

    public ActiveListingResponse(Card card) {
        this.cardId = card.getId();
        this.cardName = card.getCardName();
        this.cardNumber = card.getCardNumber();
        this.setName = card.getSetName();
        this.ebayListingId = card.getEbayListingId();
        // TODO (prod switchover): build URL from EbayTokenService.getBaseUrl() instead of hardcoding sandbox domain
        this.ebayListingUrl = "https://www.sandbox.ebay.com/itm/" + card.getEbayListingId();
        this.status = card.getStatus();
    }

    public Long getCardId() { return cardId; }
    public String getCardName() { return cardName; }
    public String getCardNumber() { return cardNumber; }
    public String getSetName() { return setName; }
    public String getEbayListingId() { return ebayListingId; }
    public String getEbayListingUrl() { return ebayListingUrl; }
    public CardStatus getStatus() { return status; }
}
