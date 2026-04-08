package com.pokemonlisting.service;

import com.pokemonlisting.dto.ServiceStats;
import com.pokemonlisting.dto.ServiceUsageSummary;
import com.pokemonlisting.dto.UsageStats;
import com.pokemonlisting.model.ApiUsage;
import com.pokemonlisting.repository.ApiUsageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApiUsageService {

    private final ApiUsageRepository apiUsageRepository;

    public ApiUsageService(ApiUsageRepository apiUsageRepository) {
        this.apiUsageRepository = apiUsageRepository;
    }

    /**
     * logApiCall(service, cost, cardId) logs a single API call to the database.
     *
     * @param service  The service name e.g. "GOOGLE_VISION", "GPT4V", "TCGDEX"
     * @param cost     Cost in USD for this call
     * @param cardId   The card this call was made for (can be null)
     */
    public void logApiCall(String service, double cost, Long cardId) {
        ApiUsage apiUsage = new ApiUsage(service, cost, cardId);
        apiUsageRepository.save(apiUsage);
    }

    /**
     * getUsageStats(startDate, endDate) returns usage statistics for the given
     * date range, broken down by service.
     * If startDate or endDate is null, defaults to last 30 days.
     *
     * @param startDate Start of date range (inclusive)
     * @param endDate   End of date range (inclusive)
     * @return UsageStats DTO with totals and per-service breakdown
     */
    public UsageStats getUsageStats(LocalDateTime startDate, LocalDateTime endDate, String service) {
        if(startDate == null) startDate = LocalDateTime.now().minusDays(30);
        if(endDate == null) endDate = LocalDateTime.now();

        List<ServiceUsageSummary> summaries = apiUsageRepository.summarizeByDateRange(
                service != null && !service.isEmpty() ? service : null, startDate, endDate);

        Map<String, ServiceStats> breakdown = new HashMap<>();
        int totalCalls = 0;
        double totalCost = 0.0;

        for (ServiceUsageSummary summary : summaries) {
            int callCount = summary.getCallCount();
            double cost = summary.getTotalCost() != null ? summary.getTotalCost() : 0.0;
            double averageCostPerCall = callCount > 0 ? cost / callCount : 0.0;
            breakdown.put(summary.getService(), new ServiceStats(summary.getService(), callCount, cost, averageCostPerCall));
            totalCalls += callCount;
            totalCost += cost;
        }

        return new UsageStats(startDate, endDate, totalCalls, totalCost, breakdown);
    }
}
