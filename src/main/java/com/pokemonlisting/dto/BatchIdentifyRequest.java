package com.pokemonlisting.dto;

import java.util.List;

public class BatchIdentifyRequest {
    private List<Long> cardIds;

    public BatchIdentifyRequest() {
    }

    public BatchIdentifyRequest(List<Long> cardIds) {
        this.cardIds = cardIds;
    }

    public List<Long> getCardIds() {
        return cardIds;
    }

    public void setCardIds(List<Long> cardIds) {
        this.cardIds = cardIds;
    }
}
