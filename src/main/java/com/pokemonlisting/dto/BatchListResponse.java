package com.pokemonlisting.dto;

import java.util.List;

public class BatchListResponse {

    private int total;
    private int succeeded;
    private int failed;
    private List<ListCardResponse> results;

    public BatchListResponse() {}

    public BatchListResponse(int total, int succeeded, int failed, List<ListCardResponse> results) {
        this.total = total;
        this.succeeded = succeeded;
        this.failed = failed;
        this.results = results;
    }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getSucceeded() { return succeeded; }
    public void setSucceeded(int succeeded) { this.succeeded = succeeded; }

    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }

    public List<ListCardResponse> getResults() { return results; }
    public void setResults(List<ListCardResponse> results) { this.results = results; }
}
