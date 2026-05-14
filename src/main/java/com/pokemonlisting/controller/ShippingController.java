package com.pokemonlisting.controller;

import com.pokemonlisting.dto.ShippingPresetRequest;
import com.pokemonlisting.dto.ShippingPresetResponse;
import com.pokemonlisting.service.ShippingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {

    private final ShippingService shippingService;

    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @GetMapping("/services")
    public ResponseEntity<?> getAvailableServices() {
        return ResponseEntity.ok(shippingService.getAvailableServices());
    }

    @GetMapping("/presets")
    public ResponseEntity<List<ShippingPresetResponse>> getPresets() {
        return ResponseEntity.ok(shippingService.getPresets());
    }

    @PostMapping("/presets")
    public ResponseEntity<ShippingPresetResponse> createPreset(@RequestBody ShippingPresetRequest request) {
        return ResponseEntity.ok(shippingService.createPreset(request));
    }

    @DeleteMapping("/presets/{id}")
    public ResponseEntity<Void> deletePreset(@PathVariable Long id) {
        try {
            shippingService.deletePreset(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
