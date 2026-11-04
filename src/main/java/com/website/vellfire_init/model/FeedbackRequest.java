package com.website.vellfire_init.model;

public class FeedbackRequest {
    private String placeId;
    private String stationName;
    private String stationDistance;
    private String stationAddress;
    private Double googleRating;
    private Integer googleRatingCount;

    // Feedback fields
    private Integer serviceQuality;        // 1-5
    private Integer washroomCleanliness;   // 1-5
    private String washroomAvailable;      // "YES" or "NO"
    private String additionalComments;

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public String getStationDistance() { return stationDistance; }
    public void setStationDistance(String stationDistance) { this.stationDistance = stationDistance; }

    public String getStationAddress() { return stationAddress; }
    public void setStationAddress(String stationAddress) { this.stationAddress = stationAddress; }

    public Double getGoogleRating() { return googleRating; }
    public void setGoogleRating(Double googleRating) { this.googleRating = googleRating; }

    public Integer getGoogleRatingCount() { return googleRatingCount; }
    public void setGoogleRatingCount(Integer googleRatingCount) { this.googleRatingCount = googleRatingCount; }

    public Integer getServiceQuality() { return serviceQuality; }
    public void setServiceQuality(Integer serviceQuality) { this.serviceQuality = serviceQuality; }

    public Integer getWashroomCleanliness() { return washroomCleanliness; }
    public void setWashroomCleanliness(Integer washroomCleanliness) { this.washroomCleanliness = washroomCleanliness; }

    public String getWashroomAvailable() { return washroomAvailable; }
    public void setWashroomAvailable(String washroomAvailable) { this.washroomAvailable = washroomAvailable; }

    public String getAdditionalComments() { return additionalComments; }
    public void setAdditionalComments(String additionalComments) { this.additionalComments = additionalComments; }
}
