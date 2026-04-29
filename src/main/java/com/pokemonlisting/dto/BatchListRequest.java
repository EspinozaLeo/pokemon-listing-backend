package com.pokemonlisting.dto;

import java.util.List;

public class BatchListRequest {

    private List<CardListingOverride> cards;
    private ListCardRequest listingParams;

    public BatchListRequest() {}

    public BatchListRequest(List<CardListingOverride> cards, ListCardRequest listingParams) {
        this.cards = cards;
        this.listingParams = listingParams;
    }

    public List<CardListingOverride> getCards() {
        return cards;
    }

    public void setCards(List<CardListingOverride> cards) {
        this.cards = cards;
    }

    public ListCardRequest getListingParams() {
        return listingParams;
    }

    public void setListingParams(ListCardRequest listingParams) {
        this.listingParams = listingParams;
    }
}
