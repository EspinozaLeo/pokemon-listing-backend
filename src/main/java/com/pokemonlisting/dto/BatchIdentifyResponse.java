package com.pokemonlisting.dto;

import java.util.List;

public class BatchIdentifyResponse {
    private int totalRequested;
    private int identifiedCount;
    private int failedCount;
    private int skippedCount;
    private List<BatchCardResult> results;

    public BatchIdentifyResponse() {
    }

    public BatchIdentifyResponse(int totalRequested, int identifiedCount, int failedCount,
                                  int skippedCount, List<BatchCardResult> results) {
        this.totalRequested = totalRequested;
        this.identifiedCount = identifiedCount;
        this.failedCount = failedCount;
        this.skippedCount = skippedCount;
        this.results = results;
    }

    public int getTotalRequested() {
        return totalRequested;
    }

    public void setTotalRequested(int totalRequested) {
        this.totalRequested = totalRequested;
    }

    public int getIdentifiedCount() {
        return identifiedCount;
    }

    public void setIdentifiedCount(int identifiedCount) {
        this.identifiedCount = identifiedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public List<BatchCardResult> getResults() {
        return results;
    }

    public void setResults(List<BatchCardResult> results) {
        this.results = results;
    }
}
