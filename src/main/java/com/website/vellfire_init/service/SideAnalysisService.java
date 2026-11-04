package com.website.vellfire_init.service;

import com.website.vellfire_init.model.SideEstimate;
import com.website.vellfire_init.model.StationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Estimates whether a petrol pump's main place coordinate is to the left or right
 * of the final approach vector derived from the Ola route polyline.
 *
 * This is explicitly a TENTATIVE estimate, not a verified entrance classification.
 * See PRD Section 8 for the full algorithm specification and limitations.
 */
@Service
public class SideAnalysisService {

    private static final String METHOD = "FINAL_APPROACH_POLYLINE_CROSS_PRODUCT";
    private static final double METRES_PER_DEGREE_LAT = 111_320.0;

    /**
     * Minimum lateral offset in metres before we classify left/right.
     * TBD: must be calibrated with real test locations.
     */
    @Value("${app.side-analysis.min-lateral-offset-meters:20.0}")
    private double minLateralOffsetMeters;

    private final PolylineDecoder polylineDecoder;

    public SideAnalysisService(PolylineDecoder polylineDecoder) {
        this.polylineDecoder = polylineDecoder;
    }

    /**
     * Runs the side-analysis algorithm and sets sideEstimate, sideMethod, sideReason on the station.
     */
    public void analyse(StationResult station) {
        String polyline = station.getRoutePolyline();
        if (polyline == null || polyline.isBlank()) {
            setUnknown(station, "No route polyline available");
            return;
        }

        List<PolylineDecoder.LatLng> points = polylineDecoder.decode(polyline);
        points = polylineDecoder.removeDuplicates(points);

        if (points.size() < 2) {
            setUnknown(station, "Fewer than two distinct route points after decoding");
            return;
        }

        PolylineDecoder.LatLng r1 = points.get(points.size() - 2);
        PolylineDecoder.LatLng r2 = points.get(points.size() - 1);

        double refLat = r2.lat();
        double cosLat = Math.cos(Math.toRadians(refLat));
        double metresPerDegreeLng = METRES_PER_DEGREE_LAT * cosLat;

        // Final approach vector (r1 → r2) in local metres
        double dx = (r2.lng() - r1.lng()) * metresPerDegreeLng;
        double dy = (r2.lat() - r1.lat()) * METRES_PER_DEGREE_LAT;

        double approachLengthM = Math.sqrt(dx * dx + dy * dy);
        if (approachLengthM < 1.0) {
            setUnknown(station, "Final approach vector too short or zero-length");
            return;
        }

        // Station offset vector (r2 → station place coordinate)
        double sx = (station.getPlaceLongitude() - r2.lng()) * metresPerDegreeLng;
        double sy = (station.getPlaceLatitude() - r2.lat()) * METRES_PER_DEGREE_LAT;

        double lateralOffsetM = Math.abs((dx * sy - dy * sx) / approachLengthM);

        if (lateralOffsetM < minLateralOffsetMeters) {
            setUnknown(station, "Lateral offset (" + String.format("%.1f", lateralOffsetM)
                    + " m) below minimum threshold (" + minLateralOffsetMeters + " m)");
            return;
        }

        double cross = (dx * sy) - (dy * sx);

        SideEstimate estimate;
        if (cross > 0) {
            estimate = SideEstimate.LEFT_OF_FINAL_APPROACH;
        } else if (cross < 0) {
            estimate = SideEstimate.RIGHT_OF_FINAL_APPROACH;
        } else {
            setUnknown(station, "Cross product is exactly zero — ambiguous");
            return;
        }

        station.setSideEstimate(estimate);
        station.setSideMethod(METHOD);
        station.setSideReason(null);
    }

    private void setUnknown(StationResult station, String reason) {
        station.setSideEstimate(SideEstimate.UNKNOWN);
        station.setSideMethod(METHOD);
        station.setSideReason(reason);
    }
}
