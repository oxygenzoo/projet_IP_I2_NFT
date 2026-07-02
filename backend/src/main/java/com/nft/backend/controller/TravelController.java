package com.nft.backend.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.nft.backend.dto.travel.EpisodeDto;
import com.nft.backend.dto.travel.SaveTravelRequest;
import com.nft.backend.dto.travel.TravelDto;
import com.nft.backend.service.TravelCatalogService;
import com.nft.backend.service.TravelService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TravelController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TravelController.class);

    private final TravelCatalogService travelCatalogService;
    private final TravelService travelService;

    public TravelController(TravelCatalogService travelCatalogService, TravelService travelService) {
        this.travelCatalogService = travelCatalogService;
        this.travelService = travelService;
    }

    @GetMapping("/travels")
    public List<TravelDto> getTravels() {
        LOGGER.info("GET /api/travels");
        return travelService.getTravels();
    }

    @GetMapping("/travels/{id}")
    public ResponseEntity<TravelDto> getTravel(@PathVariable String id) {
        try {
            return ResponseEntity.ok(travelService.getTravel(UUID.fromString(id)));
        } catch (IllegalArgumentException exception) {
            // Non-UUID ids are generated catalog ids kept for the frontend V1 flow.
        }

        return ResponseEntity.of(travelCatalogService.getTravel(id));
    }

    @PostMapping("/travels")
    public ResponseEntity<TravelDto> createTravel(@Valid @RequestBody SaveTravelRequest request) {
        TravelDto travel = travelService.create(request);
        return ResponseEntity.created(URI.create("/api/travels/" + travel.id())).body(travel);
    }

    @PutMapping("/travels/{id}")
    public TravelDto updateTravel(@PathVariable UUID id, @Valid @RequestBody SaveTravelRequest request) {
        return travelService.update(id, request);
    }

    @DeleteMapping("/travels/{id}")
    public ResponseEntity<Void> deleteTravel(@PathVariable UUID id) {
        travelService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/episodes")
    public List<EpisodeDto> getEpisodes() {
        return travelCatalogService.getEpisodes();
    }

    @GetMapping("/episodes/{id}")
    public ResponseEntity<EpisodeDto> getEpisode(@PathVariable String id) {
        return ResponseEntity.of(travelCatalogService.getEpisode(id));
    }
}
