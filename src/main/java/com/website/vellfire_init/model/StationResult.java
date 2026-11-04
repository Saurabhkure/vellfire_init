package com.website.vellfire_init.model;

import java.util.List;

/**
 * Normalized station result returned to the frontend.
 * All optional provider fields are preserved as-is; missing values are represented
 * by null (numbers) or UNKNOWN (enums) — never fabricated.
 */
public class StationResult {

    // ---- Identity ----
    private String placeId;
    private String name;
    private String address;

    // ---- Place coordinates ----
    private double placeLatitude;
    private double placeLongitude;

    // ---- Routing coordinates ----
    private Double routingLatitude;
    private Double routingLongitude;
    private RoutingDestinationSource routingDestinationSource;

    // ---- Ola Matrix results ----
    private Long drivingDistanceMeters;
    private Long estimatedDurationSeconds;
    private String routePolyline;
    private String matrixStatus;

    // ---- Side analysis ----
    private SideEstimate sideEstimate = SideEstimate.UNKNOWN;
    private String sideMethod;
    private String sideReason;

    // ---- Station details ----
    private WashroomAvailability washroomAvailability = WashroomAvailability.UNKNOWN;
    private List<String> fuelTypes;
    private Double googleRating;
    private Integer googleRatingCount;
    private String openStatus;

    // ---- Maps hand-off ----
    private String mapsUrl;

    // ---- Ranking ----
    private int rank;

    // Getters and setters

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getPlaceLatitude() { return placeLatitude; }
    public void setPlaceLatitude(double placeLatitude) { this.placeLatitude = placeLatitude; }

    public double getPlaceLongitude() { return placeLongitude; }
    public void setPlaceLongitude(double placeLongitude) { this.placeLongitude = placeLongitude; }

    public Double getRoutingLatitude() { return routingLatitude; }
    public void setRoutingLatitude(Double routingLatitude) { this.routingLatitude = routingLatitude; }

    public Double getRoutingLongitude() { return routingLongitude; }
    public void setRoutingLongitude(Double routingLongitude) { this.routingLongitude = routingLongitude; }

    public RoutingDestinationSource getRoutingDestinationSource() { return routingDestinationSource; }
    public void setRoutingDestinationSource(RoutingDestinationSource routingDestinationSource) { this.routingDestinationSource = routingDestinationSource; }

    public Long getDrivingDistanceMeters() { return drivingDistanceMeters; }
    public void setDrivingDistanceMeters(Long drivingDistanceMeters) { this.drivingDistanceMeters = drivingDistanceMeters; }

    public Long getEstimatedDurationSeconds() { return estimatedDurationSeconds; }
    public void setEstimatedDurationSeconds(Long estimatedDurationSeconds) { this.estimatedDurationSeconds = estimatedDurationSeconds; }

    public String getRoutePolyline() { return routePolyline; }
    public void setRoutePolyline(String routePolyline) { this.routePolyline = routePolyline; }

    public String getMatrixStatus() { return matrixStatus; }
    public void setMatrixStatus(String matrixStatus) { this.matrixStatus = matrixStatus; }

    public SideEstimate getSideEstimate() { return sideEstimate; }
    public void setSideEstimate(SideEstimate sideEstimate) { this.sideEstimate = sideEstimate; }

    public String getSideMethod() { return sideMethod; }
    public void setSideMethod(String sideMethod) { this.sideMethod = sideMethod; }

    public String getSideReason() { return sideReason; }
    public void setSideReason(String sideReason) { this.sideReason = sideReason; }

    public WashroomAvailability getWashroomAvailability() { return washroomAvailability; }
    public void setWashroomAvailability(WashroomAvailability washroomAvailability) { this.washroomAvailability = washroomAvailability; }

    public List<String> getFuelTypes() { return fuelTypes; }
    public void setFuelTypes(List<String> fuelTypes) { this.fuelTypes = fuelTypes; }

    public Double getGoogleRating() { return googleRating; }
    public void setGoogleRating(Double googleRating) { this.googleRating = googleRating; }

    public Integer getGoogleRatingCount() { return googleRatingCount; }
    public void setGoogleRatingCount(Integer googleRatingCount) { this.googleRatingCount = googleRatingCount; }

    public String getOpenStatus() { return openStatus; }
    public void setOpenStatus(String openStatus) { this.openStatus = openStatus; }

    public String getMapsUrl() { return mapsUrl; }
    public void setMapsUrl(String mapsUrl) { this.mapsUrl = mapsUrl; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    // Convenience helpers used in the template

    public String getFormattedDistance() {
        if (drivingDistanceMeters == null) return "Not available";
        if (drivingDistanceMeters >= 1000) {
            return String.format("%.1f km", drivingDistanceMeters / 1000.0);
        }
        return drivingDistanceMeters + " m";
    }

    public String getFormattedDuration() {
        if (estimatedDurationSeconds == null) return "Not available";
        long mins = estimatedDurationSeconds / 60;
        if (mins < 1) return "< 1 min";
        if (mins < 60) return mins + " min";
        return (mins / 60) + " hr " + (mins % 60) + " min";
    }

    public String getSideDisplayLabel() {
        return sideEstimate != null ? sideEstimate.toDisplayLabel() : SideEstimate.UNKNOWN.toDisplayLabel();
    }

    public String getWashroomDisplayLabel() {
        return washroomAvailability != null ? washroomAvailability.toDisplayLabel() : WashroomAvailability.UNKNOWN.toDisplayLabel();
    }

    public boolean isRoutingSuccessful() {
        return "OK".equalsIgnoreCase(matrixStatus) && drivingDistanceMeters != null;
    }
}
