package com.website.vellfire_init.model;

public enum WashroomAvailability {
    AVAILABLE,
    UNAVAILABLE,
    UNKNOWN;

    public String toDisplayLabel() {
        return switch (this) {
            case AVAILABLE -> "Available";
            case UNAVAILABLE -> "Not available";
            case UNKNOWN -> "Unknown";
        };
    }
}
