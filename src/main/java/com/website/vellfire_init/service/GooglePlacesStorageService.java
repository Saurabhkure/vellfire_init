package com.website.vellfire_init.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.website.vellfire_init.dto.google.GooglePlacesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GooglePlacesStorageService {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesStorageService.class);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final RestClient restClient;
    private final String supabaseUrl;
    private final String supabaseApiKey;

    public GooglePlacesStorageService(RestClient restClient,
                                      @Value("${app.supabase.url}") String supabaseUrl,
                                      @Value("${app.supabase.api-key}") String supabaseApiKey) {
        this.restClient = restClient;
        this.supabaseUrl = supabaseUrl;
        this.supabaseApiKey = supabaseApiKey;
    }

    @Async("placesStorageExecutor")
    public void saveNewPlacesAsync(List<GooglePlacesResponse.PlaceDto> places) {
        if (places == null || places.isEmpty()) return;

        OffsetDateTime fetchedAt = OffsetDateTime.now();
        int inserted = 0;
        int skipped = 0;

        for (GooglePlacesResponse.PlaceDto place : places) {
            if (place.getId() == null) continue;
            try {
                if (placeExists(place.getId())) {
                    skipped++;
                    log.debug("Skipping existing place: {}", place.getId());
                    continue;
                }
                insertPlace(place, fetchedAt);
                inserted++;
                log.debug("Inserted place: {}", place.getId());
            } catch (Exception e) {
                log.warn("Failed to save place {}: {}", place.getId(), e.getMessage());
            }
        }

        log.info("Google Places storage complete — inserted: {}, skipped: {}", inserted, skipped);
    }

    // ---- private helpers ----

    private boolean placeExists(String googlePlaceId) {
        // Use PostgREST head=true to check existence without fetching data
        try {
            restClient.get()
                    .uri(supabaseUrl + "/google_places?google_place_id=eq." + googlePlaceId + "&select=google_place_id")
                    .header("apikey", supabaseApiKey)
                    .header("Authorization", "Bearer " + supabaseApiKey)
                    .header("Prefer", "count=exact")
                    .retrieve()
                    .toBodilessEntity();

            // If we get here without exception the row exists (200 or 206)
            // Parse Content-Range header: "0-0/1" means 1 row
            var response = restClient.get()
                    .uri(supabaseUrl + "/google_places?google_place_id=eq." + googlePlaceId + "&select=google_place_id&limit=1")
                    .header("apikey", supabaseApiKey)
                    .header("Authorization", "Bearer " + supabaseApiKey)
                    .retrieve()
                    .body(List.class);
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            log.warn("Error checking place existence for {}: {}", googlePlaceId, e.getMessage());
            return false;
        }
    }

    private void insertPlace(GooglePlacesResponse.PlaceDto place, OffsetDateTime fetchedAt) {
        PlaceRow row = new PlaceRow();
        row.googlePlaceId = place.getId();

        if (place.getDisplayName() != null) {
            row.displayName = place.getDisplayName().getText();
            row.languageCode = place.getDisplayName().getLanguageCode();
        }
        row.formattedAddress = place.getFormattedAddress();
        row.latitude = place.getLocation().getLatitude();
        row.longitude = place.getLocation().getLongitude();

        if (place.getRating() != null) {
            row.rating = BigDecimal.valueOf(place.getRating());
        }
        row.userRatingCount = place.getUserRatingCount();
        row.businessStatus = place.getBusinessStatus();
        row.restroom = place.getRestroom();
        row.fetchedAt = fetchedAt.format(ISO);

        restClient.post()
                .uri(supabaseUrl + "/google_places")
                .header("apikey", supabaseApiKey)
                .header("Authorization", "Bearer " + supabaseApiKey)
                .header("Prefer", "return=minimal")
                .contentType(MediaType.APPLICATION_JSON)
                .body(row)
                .retrieve()
                .toBodilessEntity();

        // Insert opening hours if present
        List<OpeningHourRow> hourRows = buildOpeningHourRows(place);
        if (!hourRows.isEmpty()) {
            restClient.post()
                    .uri(supabaseUrl + "/google_place_opening_hours")
                    .header("apikey", supabaseApiKey)
                    .header("Authorization", "Bearer " + supabaseApiKey)
                    .header("Prefer", "return=minimal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(hourRows)
                    .retrieve()
                    .toBodilessEntity();
        }
    }

    private List<OpeningHourRow> buildOpeningHourRows(GooglePlacesResponse.PlaceDto place) {
        List<OpeningHourRow> result = new ArrayList<>();
        GooglePlacesResponse.OpeningHours regularHours = place.getRegularOpeningHours();
        if (regularHours == null || regularHours.getPeriods() == null) return result;

        int[] dayCounters = new int[7];
        for (GooglePlacesResponse.Period period : regularHours.getPeriods()) {
            GooglePlacesResponse.TimePoint open = period.getOpen();
            if (open == null || open.getDay() == null) continue;
            int day = open.getDay();
            if (day < 0 || day > 6) continue;

            dayCounters[day]++;
            OpeningHourRow row = new OpeningHourRow();
            row.googlePlaceId = place.getId();
            row.dayOfWeek = (short) day;
            row.periodOrder = (short) dayCounters[day];

            if (open.getHour() != null && open.getMinute() != null) {
                row.openTime = String.format("%02d:%02d:00", open.getHour(), open.getMinute());
            }

            GooglePlacesResponse.TimePoint close = period.getClose();
            if (close != null) {
                if (close.getDay() != null) row.closeDayOfWeek = close.getDay().shortValue();
                if (close.getHour() != null && close.getMinute() != null) {
                    row.closeTime = String.format("%02d:%02d:00", close.getHour(), close.getMinute());
                }
                row.is24Hours = open.getDay().equals(close.getDay())
                        && Integer.valueOf(0).equals(open.getHour())
                        && Integer.valueOf(0).equals(open.getMinute())
                        && Integer.valueOf(0).equals(close.getHour())
                        && Integer.valueOf(0).equals(close.getMinute());
            }

            result.add(row);
        }
        return result;
    }

    // ---- inner DTO classes for JSON serialization ----

    static class PlaceRow {
        @JsonProperty("google_place_id")  public String googlePlaceId;
        @JsonProperty("display_name")     public String displayName;
        @JsonProperty("language_code")    public String languageCode;
        @JsonProperty("formatted_address") public String formattedAddress;
        @JsonProperty("latitude")         public double latitude;
        @JsonProperty("longitude")        public double longitude;
        @JsonProperty("rating")           public BigDecimal rating;
        @JsonProperty("user_rating_count") public Integer userRatingCount;
        @JsonProperty("business_status")  public String businessStatus;
        @JsonProperty("restroom")         public Boolean restroom;
        @JsonProperty("fetched_at")       public String fetchedAt;
    }

    static class OpeningHourRow {
        @JsonProperty("google_place_id")    public String googlePlaceId;
        @JsonProperty("day_of_week")        public short dayOfWeek;
        @JsonProperty("period_order")       public short periodOrder;
        @JsonProperty("open_time")          public String openTime;
        @JsonProperty("close_day_of_week")  public Short closeDayOfWeek;
        @JsonProperty("close_time")         public String closeTime;
        @JsonProperty("is_24_hours")        public boolean is24Hours;
    }
}
