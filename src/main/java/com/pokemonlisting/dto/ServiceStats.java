package com.pokemonlisting.dto;

public class ServiceStats {

    private String serviceName;
    private int callCount;
    private Double totalCost;
    private Double averageCostPerCall;

    public ServiceStats() {
    }

    public ServiceStats(String serviceName, int callCount, Double totalCost, Double averageCostPerCall) {
        this.serviceName = serviceName;
        this.callCount = callCount;
        this.totalCost = totalCost;
        this.averageCostPerCall = averageCostPerCall;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public int getCallCount() {
        return callCount;
    }

    public void setCallCount(int callCount) {
        this.callCount = callCount;
    }

    public Double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(Double totalCost) {
        this.totalCost = totalCost;
    }

    public Double getAverageCostPerCall() {
        return averageCostPerCall;
    }

    public void setAverageCostPerCall(Double averageCostPerCall) {
        this.averageCostPerCall = averageCostPerCall;
    }
}
