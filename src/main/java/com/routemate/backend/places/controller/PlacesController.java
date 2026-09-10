package com.routemate.backend.places.controller;

import com.routemate.backend.places.service.PlacesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for location search (autocomplete and reverse geocoding).
 * Endpoint paths match the existing Express.js routes exactly.
 */
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlacesController {

    private final PlacesService placesService;

    /**
     * GET /api/places/autocomplete?q=...
     * Autocomplete location search.
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<Map<String, Object>> autocomplete(
            @RequestParam("q") String query) {

        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Query parameter \"q\" is required"));
        }

        Map<String, Object> result = placesService.autocomplete(query);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/places/reverse?lat=...&lon=...
     * Reverse geocode coordinates to an address.
     */
    @GetMapping("/reverse")
    public ResponseEntity<Map<String, Object>> reverseGeocode(
            @RequestParam("lat") String lat,
            @RequestParam("lon") String lon) {

        if (lat == null || lon == null || lat.isBlank() || lon.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Latitude and Longitude are required"));
        }

        Map<String, Object> result = placesService.reverseGeocode(lat, lon);
        return ResponseEntity.ok(result);
    }
}
