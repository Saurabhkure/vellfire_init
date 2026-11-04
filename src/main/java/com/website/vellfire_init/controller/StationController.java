package com.website.vellfire_init.controller;

import com.website.vellfire_init.model.FeedbackRequest;
import com.website.vellfire_init.model.SearchRequest;
import com.website.vellfire_init.model.SearchResponse;
import com.website.vellfire_init.service.StationSearchService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StationController {

    private final StationSearchService stationSearchService;

    public StationController(StationSearchService stationSearchService) {
        this.stationSearchService = stationSearchService;
    }

    /** Landing page */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("searchRequest", new SearchRequest());
        return "index";
    }

    /** Trigger search → show results page */
    @PostMapping("/search")
    public String search(@Valid @ModelAttribute("searchRequest") SearchRequest searchRequest,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("locationError", "Invalid coordinates. Please try again.");
            return "index";
        }

        SearchResponse response = stationSearchService.search(
                searchRequest.getLatitude(),
                searchRequest.getLongitude()
        );

        model.addAttribute("response", response);
        model.addAttribute("latitude", searchRequest.getLatitude());
        model.addAttribute("longitude", searchRequest.getLongitude());
        return "results";
    }

    /** Feedback form for a specific station */
    @GetMapping("/feedback")
    public String feedbackForm(
            @RequestParam String placeId,
            @RequestParam String stationName,
            @RequestParam(required = false) String stationDistance,
            @RequestParam(required = false) String stationAddress,
            @RequestParam(required = false) Double googleRating,
            @RequestParam(required = false) Integer googleRatingCount,
            Model model) {

        FeedbackRequest feedback = new FeedbackRequest();
        feedback.setPlaceId(placeId);
        feedback.setStationName(stationName);
        feedback.setStationDistance(stationDistance);
        feedback.setStationAddress(stationAddress);
        feedback.setGoogleRating(googleRating);
        feedback.setGoogleRatingCount(googleRatingCount);

        model.addAttribute("feedback", feedback);
        return "feedback";
    }

    /** Submit feedback (stub — logs and returns confirmation) */
    @PostMapping("/feedback")
    public String submitFeedback(@ModelAttribute("feedback") FeedbackRequest feedback, Model model) {
        // Feedback persistence is out of scope for this release (PRD §4).
        // This stub accepts the form and shows a thank-you state.
        model.addAttribute("submitted", true);
        model.addAttribute("feedback", feedback);
        return "feedback";
    }
}
