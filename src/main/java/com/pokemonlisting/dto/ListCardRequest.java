package com.pokemonlisting.dto;

public class ListCardRequest {

    private Double price;
    private String condition;
    private String format;

    public ListCardRequest(){
    }

    public ListCardRequest(Double price, String condition, String format) {
        this.price = price;
        this.condition = condition;
        this.format = format;
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

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
