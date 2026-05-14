package com.pokemonlisting.dto;

public class ShippingPresetRequest {

    private String name;
    private String shippingCarrierCode;
    private String shippingServiceCode;
    private Double shippingCost;
    private Boolean freeShipping;
    private Integer handlingTimeDays;
    private String costType;
    private Double insuranceAmount;

    public ShippingPresetRequest() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShippingCarrierCode() { return shippingCarrierCode; }
    public void setShippingCarrierCode(String shippingCarrierCode) { this.shippingCarrierCode = shippingCarrierCode; }

    public String getShippingServiceCode() { return shippingServiceCode; }
    public void setShippingServiceCode(String shippingServiceCode) { this.shippingServiceCode = shippingServiceCode; }

    public Double getShippingCost() { return shippingCost; }
    public void setShippingCost(Double shippingCost) { this.shippingCost = shippingCost; }

    public Boolean getFreeShipping() { return freeShipping; }
    public void setFreeShipping(Boolean freeShipping) { this.freeShipping = freeShipping; }

    public Integer getHandlingTimeDays() { return handlingTimeDays; }
    public void setHandlingTimeDays(Integer handlingTimeDays) { this.handlingTimeDays = handlingTimeDays; }

    public String getCostType() { return costType; }
    public void setCostType(String costType) { this.costType = costType; }

    public Double getInsuranceAmount() { return insuranceAmount; }
    public void setInsuranceAmount(Double insuranceAmount) { this.insuranceAmount = insuranceAmount; }
}
