package com.website.vellfire_init.model;

import java.util.List;

public class SearchResponse {

    private List<StationResult> stations;
    private String errorMessage;
    private boolean partialFailure;

    public SearchResponse() {}

    public SearchResponse(List<StationResult> stations) {
        this.stations = stations;
    }

    public SearchResponse(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<StationResult> getStations() { return stations; }
    public void setStations(List<StationResult> stations) { this.stations = stations; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public boolean isPartialFailure() { return partialFailure; }
    public void setPartialFailure(boolean partialFailure) { this.partialFailure = partialFailure; }

    public boolean hasError() { return errorMessage != null && !errorMessage.isBlank(); }
}
