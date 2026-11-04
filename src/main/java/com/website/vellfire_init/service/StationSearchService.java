package com.website.vellfire_init.service;

import com.website.vellfire_init.dto.google.GooglePlacesResponse;
import com.website.vellfire_init.dto.ola.OlaMatrixResponse;
import com.website.vellfire_init.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Orchestrates the full petrol pump discovery flow:
 * Google Places → normalize → routing destination selection → Ola Matrix → side analysis → rank
 */
@Service
public class StationSearchService {

    private static final Logger log = LoggerFactory.getLogger(StationSearchService.class);

    private final GooglePlacesClient googlePlacesClient;
    private final OlaMapsClient olaMapsClient;
    private final SideAnalysisService sideAnalysisService;
    private final GooglePlacesStorageService googlePlacesStorageService;

    public StationSearchService(GooglePlacesClient googlePlacesClient,
                                OlaMapsClient olaMapsClient,
                                SideAnalysisService sideAnalysisService,
                                GooglePlacesStorageService googlePlacesStorageService) {
        this.googlePlacesClient = googlePlacesClient;
        this.olaMapsClient = olaMapsClient;
        this.sideAnalysisService = sideAnalysisService;
        this.googlePlacesStorageService = googlePlacesStorageService;
    }

    public SearchResponse search(double originLat, double originLng) {

        // Step 1: Google Places nearby search
        GooglePlacesResponse placesResponse = googlePlacesClient.searchNearby(originLat, originLng);
        if (placesResponse == null) {
            return new SearchResponse("Unable to reach Google Places. Please try again later.");
        }

        List<GooglePlacesResponse.PlaceDto> places = placesResponse.getPlaces();
        if (places == null || places.isEmpty()) {
            return new SearchResponse("No petrol pumps found in the search area.");
        }

        // Async fire-and-forget: persist new places to DB (insert-only, skip existing)
        googlePlacesStorageService.saveNewPlacesAsync(places);

        // Step 2: Normalize places into internal station model
        List<StationResult> stations = new ArrayList<>();
        for (GooglePlacesResponse.PlaceDto place : places) {
            StationResult station = normalizePlaceDto(place);
            if (station != null) {
                stations.add(station);
            }
        }

        if (stations.isEmpty()) {
            return new SearchResponse("No valid petrol pump records could be processed.");
        }

        // Step 3: Exclude UNKNOWN business status — only keep OPERATIONAL
        stations.removeIf(s -> {
            boolean unknown = !"OPERATIONAL".equalsIgnoreCase(
                    getOriginalBusinessStatus(places, s.getPlaceId()));
            if (unknown) {
                log.debug("Excluding station {} — businessStatus not OPERATIONAL", s.getPlaceId());
            }
            return unknown;
        });

        if (stations.isEmpty()) {
            return new SearchResponse("No operational petrol pumps found in the area.");
        }

        // Step 4: Select routing destination (navigation point or place location fallback)
        List<StationResult> routableStations = new ArrayList<>();
        for (StationResult station : stations) {
            selectRoutingDestination(station, places);
            if (station.getRoutingLatitude() != null) {
                routableStations.add(station);
            } else {
                log.debug("Station {} has no routing destination — excluding from Ola Matrix", station.getPlaceId());
            }
        }

        // Step 5: Ola Maps Distance Matrix
        boolean partialFailure = false;
        if (!routableStations.isEmpty()) {
            OlaMatrixResponse matrixResponse = olaMapsClient.getDistanceMatrix(originLat, originLng, routableStations);
            if (matrixResponse != null && matrixResponse.getRows() != null && !matrixResponse.getRows().isEmpty()) {
                List<OlaMatrixResponse.Element> elements = matrixResponse.getRows().get(0).getElements();
                for (int i = 0; i < routableStations.size(); i++) {
                    StationResult station = routableStations.get(i);
                    if (elements != null && i < elements.size()) {
                        OlaMatrixResponse.Element elem = elements.get(i);
                        applyMatrixElement(station, elem);
                        if (!"OK".equalsIgnoreCase(elem.getStatus())) {
                            partialFailure = true;
                        }
                    } else {
                        station.setMatrixStatus("FAILED");
                        partialFailure = true;
                    }
                }
            } else {
                // Ola call failed entirely
                for (StationResult s : routableStations) {
                    s.setMatrixStatus("FAILED");
                }
                partialFailure = true;
                log.warn("Ola Maps Distance Matrix call returned no usable response");
            }
        }

        // Step 6: Side analysis for stations with a successful polyline
        for (StationResult station : routableStations) {
            if (station.isRoutingSuccessful()) {
                sideAnalysisService.analyse(station);
            }
        }

        // Step 7: Set Maps URL (Google Maps place page using place ID)
        for (StationResult station : stations) {
            if (station.getPlaceId() != null) {
                station.setMapsUrl("https://www.google.com/maps/place/?q=place_id:" + station.getPlaceId());
            }
        }

        // Step 8: Rank by driving distance ascending; stations without routing go to the end
        List<StationResult> ranked = stations.stream()
                .sorted(Comparator
                        .comparing(StationResult::isRoutingSuccessful, Comparator.reverseOrder())
                        .thenComparing(s -> s.getDrivingDistanceMeters() != null
                                ? s.getDrivingDistanceMeters() : Long.MAX_VALUE))
                .toList();

        for (int i = 0; i < ranked.size(); i++) {
            ranked.get(i).setRank(i + 1);
        }

        SearchResponse response = new SearchResponse(ranked);
        response.setPartialFailure(partialFailure);
        return response;
    }

    // ---- Private helpers ----

    private StationResult normalizePlaceDto(GooglePlacesResponse.PlaceDto place) {
        if (place.getId() == null || place.getLocation() == null) {
            log.warn("Skipping place with missing id or location");
            return null;
        }

        StationResult station = new StationResult();
        station.setPlaceId(place.getId());

        // Name
        if (place.getDisplayName() != null && place.getDisplayName().getText() != null) {
            station.setName(place.getDisplayName().getText());
        } else {
            station.setName("Unknown Station");
        }

        station.setAddress(place.getFormattedAddress());
        station.setPlaceLatitude(place.getLocation().getLatitude());
        station.setPlaceLongitude(place.getLocation().getLongitude());
        station.setGoogleRating(place.getRating());
        station.setGoogleRatingCount(place.getUserRatingCount());

        // Washroom — only set to AVAILABLE/UNAVAILABLE if explicitly returned
        if (Boolean.TRUE.equals(place.getRestroom())) {
            station.setWashroomAvailability(WashroomAvailability.AVAILABLE);
        } else if (Boolean.FALSE.equals(place.getRestroom())) {
            station.setWashroomAvailability(WashroomAvailability.UNAVAILABLE);
        } else {
            station.setWashroomAvailability(WashroomAvailability.UNKNOWN);
        }

        // Fuel types: API does not return fuel type data in the current field mask — unknown
        station.setFuelTypes(null);

        // Open status from currentOpeningHours (preferred) or regularOpeningHours
        GooglePlacesResponse.OpeningHours hours = place.getCurrentOpeningHours() != null
                ? place.getCurrentOpeningHours()
                : place.getRegularOpeningHours();
        if (hours != null && hours.getOpenNow() != null) {
            station.setOpenStatus(Boolean.TRUE.equals(hours.getOpenNow()) ? "Open" : "Closed");
        } else {
            station.setOpenStatus(null);
        }

        return station;
    }

    private void selectRoutingDestination(StationResult station,
                                          List<GooglePlacesResponse.PlaceDto> places) {
        GooglePlacesResponse.PlaceDto place = findPlace(places, station.getPlaceId());
        if (place == null) return;

        List<GooglePlacesResponse.NavigationPoint> navPoints = place.getNavigationPoints();
        if (navPoints != null) {
            // Prefer a DRIVE nav point that is NOT a PARKING point
            for (GooglePlacesResponse.NavigationPoint np : navPoints) {
                if (isDriveMode(np) && !isParkingUsage(np)) {
                    station.setRoutingLatitude(np.getLocation().getLatitude());
                    station.setRoutingLongitude(np.getLocation().getLongitude());
                    station.setRoutingDestinationSource(RoutingDestinationSource.DRIVE_NAVIGATION_POINT);
                    return;
                }
            }
        }

        // Fallback: use main place location (approved per PRD Section 5, Step 4, item 4)
        station.setRoutingLatitude(station.getPlaceLatitude());
        station.setRoutingLongitude(station.getPlaceLongitude());
        station.setRoutingDestinationSource(RoutingDestinationSource.PLACE_LOCATION_FALLBACK);
    }

    private void applyMatrixElement(StationResult station, OlaMatrixResponse.Element elem) {
        station.setMatrixStatus(elem.getStatus());
        if ("OK".equalsIgnoreCase(elem.getStatus())) {
            station.setDrivingDistanceMeters(elem.getDistance());
            station.setEstimatedDurationSeconds(elem.getDuration());
            station.setRoutePolyline(elem.getPolyline());
        }
    }

    private boolean isDriveMode(GooglePlacesResponse.NavigationPoint np) {
        return np.getTravelModes() != null && np.getTravelModes().contains("DRIVE");
    }

    private boolean isParkingUsage(GooglePlacesResponse.NavigationPoint np) {
        return np.getUsages() != null && np.getUsages().contains("PARKING");
    }

    private GooglePlacesResponse.PlaceDto findPlace(List<GooglePlacesResponse.PlaceDto> places, String placeId) {
        if (places == null || placeId == null) return null;
        return places.stream().filter(p -> placeId.equals(p.getId())).findFirst().orElse(null);
    }

    private String getOriginalBusinessStatus(List<GooglePlacesResponse.PlaceDto> places, String placeId) {
        GooglePlacesResponse.PlaceDto place = findPlace(places, placeId);
        return place != null ? place.getBusinessStatus() : null;
    }
}
