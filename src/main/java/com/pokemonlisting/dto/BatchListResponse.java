package com.pokemonlisting.dto;

import java.util.List;

public class BatchListResponse {

    // TODO 3: Add three fields:
    //   - int total          — how many cards were attempted
    //   - int succeeded      — how many listed successfully
    //   - int failed         — how many failed
    //   - List<ListCardResponse> results  — one result per card (success or failure)
    // Hint: ListCardResponse already has status + errorReason, so reuse it here

    public BatchListResponse() {}

    // TODO 4: Add an all-args constructor, getters, and setters
    // Hint: succeeded and failed can be computed from the results list
    //   results.stream().filter(r -> "LISTED".equals(r.getStatus())).count()
}
