package com.website.vellfire_init.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.website.vellfire_init.dto.ola.OlaMatrixResponse;
import com.website.vellfire_init.model.StationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OlaMapsClient {

    private static final Logger log = LoggerFactory.getLogger(OlaMapsClient.class);

    private final ObjectMapper objectMapper;

    @Value("${app.ola.maps.api-key}")
    private String apiKey;

    @Value("${app.ola.maps.distance-matrix-url}")
    private String distanceMatrixUrl;

    public OlaMapsClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OlaMatrixResponse getDistanceMatrix(double originLat, double originLng,
                                               List<StationResult> routableStations) {
        if (routableStations == null || routableStations.isEmpty()) {
            return null;
        }

        String origins = originLat + "," + originLng;

        // Ola Maps requires literal pipe | as destination separator
        String destinations = routableStations.stream()
                .map(s -> s.getRoutingLatitude() + "," + s.getRoutingLongitude())
                .collect(Collectors.joining("|"));

        // Encode pipe as %7C — new URL() sends it as-is without double-encoding
        String encodedDestinations = destinations.replace("|", "%7C");

        String rawUrl = distanceMatrixUrl
                + "?origins=" + origins
                + "&destinations=" + encodedDestinations
                + "&api_key=" + apiKey;

        log.info("Ola Matrix URL (masked): {}", rawUrl.replace(apiKey, "***"));

        HttpURLConnection conn = null;
        try {
            URL url = new URL(rawUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);

            int status = conn.getResponseCode();
            log.info("Ola Maps HTTP status: {}", status);

            InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String rawBody = new String(is.readAllBytes());
            log.info("Ola Maps raw body: {}", rawBody);

            if (status >= 400) {
                log.error("Ola Maps returned error status {}: {}", status, rawBody);
                return null;
            }

            OlaMatrixResponse parsed = objectMapper.readValue(rawBody, OlaMatrixResponse.class);

            if (parsed.getRows() == null || parsed.getRows().isEmpty()) {
                log.warn("Ola Maps response has no rows. Body: {}", rawBody);
                return null;
            }

            return parsed;

        } catch (Exception ex) {
            log.error("OLA_EXCEPTION_CLASS: {}", ex.getClass().getName());
            log.error("OLA_EXCEPTION_MSG: {}", ex.getMessage());
            if (ex.getCause() != null) {
                log.error("OLA_CAUSED_BY: {}", ex.getCause().getClass().getName());
                log.error("OLA_CAUSED_MSG: {}", ex.getCause().getMessage());
            }
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
