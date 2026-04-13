package com.pokemonlisting.dto;

public class PokemonCard {
    private String name;
    private String setName;
    private String cardNumber;
    private String rarity;

    public PokemonCard(){
    }

    public PokemonCard(String name, String setName, String cardNumber, String rarity) {
        this.name = name;
        this.setName = setName;
        this.cardNumber = cardNumber;
        this.rarity = rarity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public String toString() {
        return "PokemonCard{" +
                "name='" + name + '\'' +
                ", setName='" + setName + '\'' +
                ", cardNumber='" + cardNumber + '\'' +
                ", rarity='" + rarity + '\'' +
                '}';
    }
}
