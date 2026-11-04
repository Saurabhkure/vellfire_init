package com.website.vellfire_init.dto.google;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class GooglePlacesRequest {

    @JsonProperty("includedTypes")
    private List<String> includedTypes = List.of("gas_station");

    @JsonProperty("maxResultCount")
    private int maxResultCount;

    @JsonProperty("rankPreference")
    private String rankPreference = "DISTANCE";

    @JsonProperty("locationRestriction")
    private LocationRestriction locationRestriction;

    public GooglePlacesRequest(double latitude, double longitude, double radiusMeters, int maxResults) {
        this.maxResultCount = maxResults;
        Center center = new Center(latitude, longitude);
        Circle circle = new Circle(center, radiusMeters);
        this.locationRestriction = new LocationRestriction(circle);
    }

    public List<String> getIncludedTypes() { return includedTypes; }
    public int getMaxResultCount() { return maxResultCount; }
    public String getRankPreference() { return rankPreference; }
    public LocationRestriction getLocationRestriction() { return locationRestriction; }

    public record Center(double latitude, double longitude) {}
    public record Circle(Center center, double radius) {}
    public record LocationRestriction(Circle circle) {}
}
