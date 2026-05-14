package com.pokemonlisting.dto;

public class UpdateListingRequest {

    private Double price;
    private String condition;

    public UpdateListingRequest() {
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
