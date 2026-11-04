package com.website.vellfire_init.dto.ola;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OlaMatrixResponse {

    @JsonProperty("rows")
    private List<Row> rows;

    @JsonProperty("status")
    private String status;

    public List<Row> getRows() { return rows; }
    public void setRows(List<Row> rows) { this.rows = rows; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Row {
        @JsonProperty("elements")
        private List<Element> elements;

        public List<Element> getElements() { return elements; }
        public void setElements(List<Element> elements) { this.elements = elements; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Element {
        @JsonProperty("status")
        private String status;

        // Ola returns distance and duration as plain numbers (metres and seconds)
        @JsonProperty("distance")
        private Long distance;

        @JsonProperty("duration")
        private Long duration;

        @JsonProperty("polyline")
        private String polyline;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Long getDistance() { return distance; }
        public void setDistance(Long distance) { this.distance = distance; }

        public Long getDuration() { return duration; }
        public void setDuration(Long duration) { this.duration = duration; }

        public String getPolyline() { return polyline; }
        public void setPolyline(String polyline) { this.polyline = polyline; }
    }
}
