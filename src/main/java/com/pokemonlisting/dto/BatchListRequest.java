package com.pokemonlisting.dto;

import java.util.List;

public class BatchListRequest {

    private List<Long> cardIds;
    private ListCardRequest listingParams;

    public BatchListRequest() {}

    public BatchListRequest(List<Long> cardIds, ListCardRequest listingParams) {
        this.cardIds = cardIds;
        this.listingParams = listingParams;
    }

    public List<Long> getCardIds() {
        return cardIds;
    }

    public void setCardIds(List<Long> cardIds) {
        this.cardIds = cardIds;
    }

    public ListCardRequest getListingParams() {
        return listingParams;
    }

    public void setListingParams(ListCardRequest listingParams) {
        this.listingParams = listingParams;
    }
}
