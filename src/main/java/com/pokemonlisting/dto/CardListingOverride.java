package com.pokemonlisting.dto;

public class CardListingOverride {

    private Long cardId;
    private Double price;
    private String condition;
    private String format;

    public CardListingOverride() {}

    public CardListingOverride(Long cardId, Double price, String condition, String format) {
        this.cardId = cardId;
        this.price = price;
        this.condition = condition;
        this.format = format;
    }

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}
