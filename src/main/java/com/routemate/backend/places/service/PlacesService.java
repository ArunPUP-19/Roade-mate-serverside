package com.routemate.backend.places.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * Service for location autocomplete and reverse geocoding.
 * Proxies to Google Maps API with OpenStreetMap Nominatim fallback.
 * Replicates the exact logic from the Express.js server.
 */
@Service
@Slf4j
public class PlacesService {

    @Value("${app.google-maps.api-key:}")
    private String googleMapsApiKey;

    private final RestTemplate restTemplate;

    public PlacesService() {
        this.restTemplate = new RestTemplate();
    }

    private boolean isGoogleKeyConfigured() {
        return googleMapsApiKey != null
            && !googleMapsApiKey.isBlank()
            && !googleMapsApiKey.equals("YOUR_GOOGLE_MAPS_API_KEY_HERE");
    }

    /**
     * Autocomplete location search.
     * Tries Google Places API first, falls back to OSM Nominatim.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> autocomplete(String query) {
        if (!isGoogleKeyConfigured()) {
            return osmAutocomplete(query);
        }

        try {
            String url = UriComponentsBuilder
                .fromHttpUrl("https://maps.googleapis.com/maps/api/place/autocomplete/json")
                .queryParam("input", query)
                .queryParam("types", "(cities)")
                .queryParam("key", googleMapsApiKey)
                .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null) {
                String status = (String) response.get("status");
                if ("REQUEST_DENIED".equals(status) || "OVER_QUERY_LIMIT".equals(status)) {
                    return osmAutocomplete(query);
                }
            }

            return response != null ? response : osmAutocomplete(query);
        } catch (Exception e) {
            log.warn("Google Places API failed, falling back to OSM: {}", e.getMessage());
            return osmAutocomplete(query);
        }
    }

    /**
     * Reverse geocode coordinates to an address.
     * Tries Google Geocoding API first, falls back to OSM Nominatim.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> reverseGeocode(String lat, String lon) {
        if (!isGoogleKeyConfigured()) {
            return osmReverseGeocode(lat, lon);
        }

        try {
            String url = UriComponentsBuilder
                .fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                .queryParam("latlng", lat + "," + lon)
                .queryParam("key", googleMapsApiKey)
                .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null) {
                String status = (String) response.get("status");
                if ("REQUEST_DENIED".equals(status) || "OVER_QUERY_LIMIT".equals(status)) {
                    return osmReverseGeocode(lat, lon);
                }
            }

            return response != null ? response : osmReverseGeocode(lat, lon);
        } catch (Exception e) {
            log.warn("Google Geocoding API failed, falling back to OSM: {}", e.getMessage());
            return osmReverseGeocode(lat, lon);
        }
    }

    /**
     * OpenStreetMap Nominatim autocomplete fallback.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> osmAutocomplete(String query) {
        try {
            String url = UriComponentsBuilder
                .fromHttpUrl("https://nominatim.openstreetmap.org/search")
                .queryParam("format", "json")
                .queryParam("q", query)
                .queryParam("limit", 5)
                .toUriString();

            // Nominatim requires a User-Agent header
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "RouteMate-App");
            org.springframework.http.HttpEntity<Void> entity =
                new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<List> responseEntity =
                restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, List.class);

            List<Map<String, Object>> osmResults = responseEntity.getBody();

            if (osmResults == null) {
                return Map.of("predictions", List.of());
            }

            List<Map<String, String>> predictions = osmResults.stream()
                .map(item -> Map.of(
                    "place_id", String.valueOf(item.get("place_id")),
                    "description", String.valueOf(item.get("display_name"))
                ))
                .toList();

            return Map.of("predictions", predictions);
        } catch (Exception e) {
            log.error("OSM Autocomplete fallback failed: {}", e.getMessage());
            return Map.of("predictions", List.of());
        }
    }

    /**
     * OpenStreetMap Nominatim reverse geocoding fallback.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> osmReverseGeocode(String lat, String lon) {
        try {
            String url = UriComponentsBuilder
                .fromHttpUrl("https://nominatim.openstreetmap.org/reverse")
                .queryParam("format", "json")
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .toUriString();

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "RouteMate-App");
            org.springframework.http.HttpEntity<Void> entity =
                new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<Map> responseEntity =
                restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, Map.class);

            Map<String, Object> data = responseEntity.getBody();

            if (data != null && data.containsKey("display_name")) {
                Map<String, Object> address = (Map<String, Object>) data.getOrDefault("address", Map.of());
                String city = (String) address.getOrDefault("city",
                    address.getOrDefault("town",
                        address.getOrDefault("village",
                            address.getOrDefault("county", null))));
                String state = (String) address.get("state");

                String formattedAddress = (city != null && state != null)
                    ? city + ", " + state
                    : (String) data.get("display_name");

                return Map.of("results", List.of(
                    Map.of("formatted_address", formattedAddress)
                ));
            }

            return Map.of("results", List.of());
        } catch (Exception e) {
            log.error("OSM Reverse Geocode fallback failed: {}", e.getMessage());
            return Map.of("results", List.of());
        }
    }
}
