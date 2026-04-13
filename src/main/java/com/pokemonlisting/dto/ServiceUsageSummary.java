package com.pokemonlisting.dto;

// Projection interface used to map the GROUP BY query result in ApiUsageRepository.
// Spring Data JPA maps each column alias (service, callCount, totalCost) to these getters.
public interface ServiceUsageSummary {
    String getService();
    int getCallCount();
    Double getTotalCost();
}
