package com.pokemonlisting.dto;

public class ShippingServiceOption {

    private String carrierCode;
    private String serviceCode;
    private Integer minShippingTime;
    private Integer maxShippingTime;

    public ShippingServiceOption(String carrierCode, String serviceCode, Integer minShippingTime, Integer maxShippingTime) {
        this.carrierCode = carrierCode;
        this.serviceCode = serviceCode;
        this.minShippingTime = minShippingTime;
        this.maxShippingTime = maxShippingTime;
    }

    public String getCarrierCode() { return carrierCode; }
    public String getServiceCode() { return serviceCode; }
    public Integer getMinShippingTime() { return minShippingTime; }
    public Integer getMaxShippingTime() { return maxShippingTime; }
}
