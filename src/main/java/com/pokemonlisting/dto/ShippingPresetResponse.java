package com.pokemonlisting.dto;

import com.pokemonlisting.model.ShippingPreset;

public class ShippingPresetResponse {

    private Long id;
    private String name;
    private String shippingCarrierCode;
    private String shippingServiceCode;
    private Double shippingCost;
    private Boolean freeShipping;
    private Integer handlingTimeDays;
    private String costType;
    private Double insuranceAmount;
    private String ebayPolicyId;

    public ShippingPresetResponse(ShippingPreset preset) {
        this.id = preset.getId();
        this.name = preset.getName();
        this.shippingCarrierCode = preset.getShippingCarrierCode();
        this.shippingServiceCode = preset.getShippingServiceCode();
        this.shippingCost = preset.getShippingCost();
        this.freeShipping = preset.getFreeShipping();
        this.handlingTimeDays = preset.getHandlingTimeDays();
        this.costType = preset.getCostType();
        this.insuranceAmount = preset.getInsuranceAmount();
        this.ebayPolicyId = preset.getEbayPolicyId();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getShippingCarrierCode() { return shippingCarrierCode; }
    public String getShippingServiceCode() { return shippingServiceCode; }
    public Double getShippingCost() { return shippingCost; }
    public Boolean getFreeShipping() { return freeShipping; }
    public Integer getHandlingTimeDays() { return handlingTimeDays; }
    public String getCostType() { return costType; }
    public Double getInsuranceAmount() { return insuranceAmount; }
    public String getEbayPolicyId() { return ebayPolicyId; }
}
