package com.pokemonlisting.dto;

import com.pokemonlisting.model.Card;

public class ReadyToListCardResponse {

    private Long cardId;
    private String cardName;
    private String cardNumber;
    private String setName;
    private String rarity;
    private int imageCount;
    private Boolean needsReview;
    private Double price; // null until TLS-44 adds TCGPlayer suggested price

    public ReadyToListCardResponse(Card card, int imageCount) {
        this.cardId = card.getId();
        this.cardName = card.getCardName();
        this.cardNumber = card.getCardNumber();
        this.setName = card.getSetName();
        this.rarity = card.getRarity();
        this.imageCount = imageCount;
        this.needsReview = card.getNeedsReview();
        this.price = null;
    }

    public Long getCardId() { return cardId; }
    public String getCardName() { return cardName; }
    public String getCardNumber() { return cardNumber; }
    public String getSetName() { return setName; }
    public String getRarity() { return rarity; }
    public int getImageCount() { return imageCount; }
    public Boolean getNeedsReview() { return needsReview; }
    public Double getPrice() { return price; }
}
