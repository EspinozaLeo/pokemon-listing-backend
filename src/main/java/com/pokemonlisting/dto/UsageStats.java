package com.pokemonlisting.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class UsageStats {

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int totalCalls;
    private Double totalCost;
    private Map<String, ServiceStats> breakdown;

    public UsageStats() {
    }

    public UsageStats(LocalDateTime startDate, LocalDateTime endDate, int totalCalls,
                      Double totalCost, Map<String, ServiceStats> breakdown) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalCalls = totalCalls;
        this.totalCost = totalCost;
        this.breakdown = breakdown;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public int getTotalCalls() {
        return totalCalls;
    }

    public void setTotalCalls(int totalCalls) {
        this.totalCalls = totalCalls;
    }

    public Double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(Double totalCost) {
        this.totalCost = totalCost;
    }

    public Map<String, ServiceStats> getBreakdown() {
        return breakdown;
    }

    public void setBreakdown(Map<String, ServiceStats> breakdown) {
        this.breakdown = breakdown;
    }
}
