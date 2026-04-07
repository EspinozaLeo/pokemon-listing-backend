package com.pokemonlisting.dto;

public class ConfirmCardRequest {
    private String cardName;
    private String setName;
    private String cardNumber;
    private String rarity;

    public ConfirmCardRequest() {
    }

    public ConfirmCardRequest(String cardName, String setName, String cardNumber, String rarity) {
        this.cardName = cardName;
        this.setName = setName;
        this.cardNumber = cardNumber;
        this.rarity = rarity;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getSetName() {
        return setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }
}
