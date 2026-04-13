package com.pokemonlisting.controller;

import com.pokemonlisting.dto.UsageStats;
import com.pokemonlisting.service.ApiUsageService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ApiUsageService apiUsageService;

    public AdminController(ApiUsageService apiUsageService) {
        this.apiUsageService = apiUsageService;
    }

    //getUsage() returns API usage statistics.
    //All params are optional.
    //Example: GET /api/admin/usage?service=GPT4V&startDate=2026-01-01T00:00:00&endDate=2026-04-01T00:00:00
    @GetMapping("/usage")
    public ResponseEntity<UsageStats> getUsage(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        return ResponseEntity.ok(apiUsageService.getUsageStats(startDate, endDate, service));
    }
}
