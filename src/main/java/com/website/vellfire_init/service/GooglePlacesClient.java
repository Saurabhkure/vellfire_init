package com.website.vellfire_init.service;

import com.website.vellfire_init.dto.google.GooglePlacesRequest;
import com.website.vellfire_init.dto.google.GooglePlacesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@Service
public class GooglePlacesClient {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesClient.class);

    private final RestClient restClient;

    @Value("${app.google.places.api-key}")
    private String apiKey;

    @Value("${app.google.places.nearby-search-url}")
    private String nearbySearchUrl;

    @Value("${app.google.places.field-mask}")
    private String fieldMask;

    @Value("${app.search.radius-meters:15000}")
    private double radiusMeters;

    @Value("${app.search.max-results:6}")
    private int maxResults;

    public GooglePlacesClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Calls Google Places Nearby Search and returns the raw response DTO.
     * Returns null on any provider error; caller is responsible for handling null.
     */
    public GooglePlacesResponse searchNearby(double latitude, double longitude) {
        GooglePlacesRequest requestBody = new GooglePlacesRequest(latitude, longitude, radiusMeters, maxResults);
        try {
            return restClient.post()
                    .uri(nearbySearchUrl)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", fieldMask)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(GooglePlacesResponse.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Google Places API error: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return null;
        } catch (Exception ex) {
            log.error("Google Places API call failed: {}", ex.getMessage(), ex);
            return null;
        }
    }
}
