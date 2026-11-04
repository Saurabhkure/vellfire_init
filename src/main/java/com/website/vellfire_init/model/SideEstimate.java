package com.website.vellfire_init.model;

public enum SideEstimate {
    LEFT_OF_FINAL_APPROACH,
    RIGHT_OF_FINAL_APPROACH,
    UNKNOWN;

    public String toDisplayLabel() {
        return switch (this) {
            case LEFT_OF_FINAL_APPROACH -> "Likely on your left";
            case RIGHT_OF_FINAL_APPROACH -> "Likely on your right";
            case UNKNOWN -> "Side not available";
        };
    }
}
