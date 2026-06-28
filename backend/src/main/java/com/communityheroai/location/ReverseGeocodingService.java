package com.communityheroai.location;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

@Service
public class ReverseGeocodingService {
    private static final Logger log = LoggerFactory.getLogger(ReverseGeocodingService.class);
    private final RestClient restClient;
    private final Map<String, ReverseGeocodeResponse> cache = new ConcurrentHashMap<>();

    public ReverseGeocodingService(
            RestClient.Builder builder,
            @Value("${app.geocoding.base-url}") String baseUrl,
            @Value("${app.geocoding.user-agent}") String userAgent) {
        this.restClient = builder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .build();
    }

    public ReverseGeocodeResponse reverse(double latitude, double longitude) {
        String cacheKey = String.format(Locale.ROOT, "%.5f,%.5f", latitude, longitude);
        ReverseGeocodeResponse cached = cache.get(cacheKey);
        if (cached != null) return cached;
        ReverseGeocodeResponse response = requestLocation(latitude, longitude);
        if (response.resolved()) cache.put(cacheKey, response);
        return response;
    }

    public List<LocationSearchResponse> search(String query) {
        try {
            JsonNode results = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/search")
                            .queryParam("format", "jsonv2")
                            .queryParam("addressdetails", 1)
                            .queryParam("limit", 5)
                            .queryParam("countrycodes", "in")
                            .queryParam("q", query)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            if (results == null || !results.isArray()) return List.of();
            return StreamSupport.stream(results.spliterator(), false)
                    .map(this::toSearchResponse)
                    .filter(result -> result.latitude() != null && result.longitude() != null)
                    .toList();
        } catch (RuntimeException ex) {
            log.warn("Location search failed for '{}': {}", query, ex.getMessage());
            return List.of();
        }
    }

    private LocationSearchResponse toSearchResponse(JsonNode result) {
        JsonNode address = result.path("address");
        return new LocationSearchResponse(
                number(result, "lat"),
                number(result, "lon"),
                text(result, "display_name"),
                text(address, "country"),
                text(address, "state"),
                firstText(address, "state_district", "county", "district"),
                firstText(address, "city", "town", "municipality", "village"),
                firstText(address, "suburb", "neighbourhood", "quarter", "hamlet"),
                text(address, "postcode")
        );
    }

    private ReverseGeocodeResponse requestLocation(double latitude, double longitude) {
        try {
            JsonNode result = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/reverse")
                            .queryParam("format", "jsonv2")
                            .queryParam("addressdetails", 1)
                            .queryParam("zoom", 18)
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            if (result == null || result.path("error").isTextual()) {
                return unresolved(latitude, longitude, "No address was found for these coordinates.");
            }
            JsonNode address = result.path("address");
            return new ReverseGeocodeResponse(
                    latitude, longitude,
                    text(address, "country"),
                    text(address, "state"),
                    firstText(address, "state_district", "county", "district"),
                    firstText(address, "city", "town", "municipality", "village"),
                    firstText(address, "suburb", "neighbourhood", "quarter", "hamlet"),
                    text(address, "ward"),
                    text(address, "postcode"),
                    firstText(address, "road", "pedestrian", "footway"),
                    text(result, "display_name"),
                    true,
                    "Address detected. Please confirm the map pin and details."
            );
        } catch (RuntimeException ex) {
            log.warn("Reverse geocoding failed for {}, {}: {}", latitude, longitude, ex.getMessage());
            return unresolved(latitude, longitude,
                    "Coordinates were captured, but address lookup is temporarily unavailable.");
        }
    }

    private ReverseGeocodeResponse unresolved(double latitude, double longitude, String message) {
        return new ReverseGeocodeResponse(latitude, longitude, null, null, null, null,
                null, null, null, null, null, false, message);
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null) return value;
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }

    private Double number(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) return null;
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
