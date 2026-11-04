package com.website.vellfire_init.dto.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GooglePlacesResponse {

    @JsonProperty("places")
    private List<PlaceDto> places;

    public List<PlaceDto> getPlaces() { return places; }
    public void setPlaces(List<PlaceDto> places) { this.places = places; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlaceDto {

        @JsonProperty("id")
        private String id;

        @JsonProperty("displayName")
        private DisplayName displayName;

        @JsonProperty("formattedAddress")
        private String formattedAddress;

        @JsonProperty("location")
        private LatLng location;

        @JsonProperty("businessStatus")
        private String businessStatus;

        @JsonProperty("rating")
        private Double rating;

        @JsonProperty("userRatingCount")
        private Integer userRatingCount;

        @JsonProperty("regularOpeningHours")
        private OpeningHours regularOpeningHours;

        @JsonProperty("currentOpeningHours")
        private OpeningHours currentOpeningHours;

        @JsonProperty("restroom")
        private Boolean restroom;

        @JsonProperty("navigationPoints")
        private List<NavigationPoint> navigationPoints;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public DisplayName getDisplayName() { return displayName; }
        public void setDisplayName(DisplayName displayName) { this.displayName = displayName; }

        public String getFormattedAddress() { return formattedAddress; }
        public void setFormattedAddress(String formattedAddress) { this.formattedAddress = formattedAddress; }

        public LatLng getLocation() { return location; }
        public void setLocation(LatLng location) { this.location = location; }

        public String getBusinessStatus() { return businessStatus; }
        public void setBusinessStatus(String businessStatus) { this.businessStatus = businessStatus; }

        public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }

        public Integer getUserRatingCount() { return userRatingCount; }
        public void setUserRatingCount(Integer userRatingCount) { this.userRatingCount = userRatingCount; }

        public OpeningHours getRegularOpeningHours() { return regularOpeningHours; }
        public void setRegularOpeningHours(OpeningHours regularOpeningHours) { this.regularOpeningHours = regularOpeningHours; }

        public OpeningHours getCurrentOpeningHours() { return currentOpeningHours; }
        public void setCurrentOpeningHours(OpeningHours currentOpeningHours) { this.currentOpeningHours = currentOpeningHours; }

        public Boolean getRestroom() { return restroom; }
        public void setRestroom(Boolean restroom) { this.restroom = restroom; }

        public List<NavigationPoint> getNavigationPoints() { return navigationPoints; }
        public void setNavigationPoints(List<NavigationPoint> navigationPoints) { this.navigationPoints = navigationPoints; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DisplayName {
        @JsonProperty("text")
        private String text;

        @JsonProperty("languageCode")
        private String languageCode;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public String getLanguageCode() { return languageCode; }
        public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LatLng {
        @JsonProperty("latitude")
        private double latitude;

        @JsonProperty("longitude")
        private double longitude;

        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpeningHours {
        @JsonProperty("openNow")
        private Boolean openNow;

        @JsonProperty("weekdayDescriptions")
        private List<String> weekdayDescriptions;

        @JsonProperty("periods")
        private List<Period> periods;

        public Boolean getOpenNow() { return openNow; }
        public void setOpenNow(Boolean openNow) { this.openNow = openNow; }

        public List<String> getWeekdayDescriptions() { return weekdayDescriptions; }
        public void setWeekdayDescriptions(List<String> weekdayDescriptions) { this.weekdayDescriptions = weekdayDescriptions; }

        public List<Period> getPeriods() { return periods; }
        public void setPeriods(List<Period> periods) { this.periods = periods; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Period {
        @JsonProperty("open")
        private TimePoint open;

        @JsonProperty("close")
        private TimePoint close;

        public TimePoint getOpen() { return open; }
        public void setOpen(TimePoint open) { this.open = open; }

        public TimePoint getClose() { return close; }
        public void setClose(TimePoint close) { this.close = close; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimePoint {
        @JsonProperty("day")
        private Integer day;

        @JsonProperty("hour")
        private Integer hour;

        @JsonProperty("minute")
        private Integer minute;

        public Integer getDay() { return day; }
        public void setDay(Integer day) { this.day = day; }

        public Integer getHour() { return hour; }
        public void setHour(Integer hour) { this.hour = hour; }

        public Integer getMinute() { return minute; }
        public void setMinute(Integer minute) { this.minute = minute; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NavigationPoint {
        @JsonProperty("location")
        private LatLng location;

        @JsonProperty("travelModes")
        private List<String> travelModes;

        @JsonProperty("usages")
        private List<String> usages;

        public LatLng getLocation() { return location; }
        public void setLocation(LatLng location) { this.location = location; }

        public List<String> getTravelModes() { return travelModes; }
        public void setTravelModes(List<String> travelModes) { this.travelModes = travelModes; }

        public List<String> getUsages() { return usages; }
        public void setUsages(List<String> usages) { this.usages = usages; }
    }
}
