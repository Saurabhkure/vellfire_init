package com.website.vellfire_init.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Decodes a Google/Ola encoded polyline string into a list of lat/lng points.
 * Uses the standard encoded polyline algorithm (precision 1e-5).
 */
@Component
public class PolylineDecoder {

    public record LatLng(double lat, double lng) {}

    /**
     * Decodes an encoded polyline string.
     * Returns an empty list if the polyline is null, blank, or cannot be decoded.
     */
    public List<LatLng> decode(String encoded) {
        List<LatLng> result = new ArrayList<>();
        if (encoded == null || encoded.isBlank()) {
            return result;
        }

        int index = 0;
        int len = encoded.length();
        int lat = 0;
        int lng = 0;

        try {
            while (index < len) {
                int shift = 0;
                int b;
                int result_int = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result_int |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result_int & 1) != 0 ? ~(result_int >> 1) : (result_int >> 1));
                lat += dlat;

                shift = 0;
                result_int = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result_int |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result_int & 1) != 0 ? ~(result_int >> 1) : (result_int >> 1));
                lng += dlng;

                result.add(new LatLng(lat / 1e5, lng / 1e5));
            }
        } catch (Exception ex) {
            // Malformed polyline — return what we have so far (may be empty)
        }

        return result;
    }

    /**
     * Removes consecutive duplicate points from the decoded sequence.
     */
    public List<LatLng> removeDuplicates(List<LatLng> points) {
        if (points == null || points.isEmpty()) return points;
        List<LatLng> cleaned = new ArrayList<>();
        cleaned.add(points.get(0));
        for (int i = 1; i < points.size(); i++) {
            LatLng prev = cleaned.get(cleaned.size() - 1);
            LatLng curr = points.get(i);
            if (prev.lat() != curr.lat() || prev.lng() != curr.lng()) {
                cleaned.add(curr);
            }
        }
        return cleaned;
    }
}
