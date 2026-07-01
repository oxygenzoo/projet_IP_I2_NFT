package com.nft.backend.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.nft.backend.dto.travel.CreateTravelRequest;
import com.nft.backend.dto.travel.EpisodeDto;
import com.nft.backend.dto.travel.TravelResponse;
import com.nft.backend.dto.travel.UpdateTravelRequest;
import com.nft.backend.service.TravelCatalogService;
import com.nft.backend.service.TravelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class TravelController {

    private final TravelCatalogService travelCatalogService;
    private final TravelService travelService;

    public TravelController(TravelCatalogService travelCatalogService, TravelService travelService) {
        this.travelCatalogService = travelCatalogService;
        this.travelService = travelService;
    }

    @GetMapping("/travels")
    public List<TravelResponse> getTravels(@RequestParam(required = false) UUID userId) {
        if (userId == null) {
            return travelService.getAll();
        }

        return travelService.getByUser(userId);
    }

    @PostMapping("/travels")
    public ResponseEntity<TravelResponse> createTravel(@Valid @RequestBody CreateTravelRequest request) {
        TravelResponse travel = travelService.create(request);
        return ResponseEntity
                .created(URI.create("/api/travels/" + travel.id()))
                .body(travel);
    }

    @GetMapping("/travels/{id}")
    public TravelResponse getTravel(@PathVariable String id) {
        return travelService.get(parseTravelId(id));
    }

    @PutMapping("/travels/{id}")
    public TravelResponse updateTravel(
            @PathVariable String id,
            @Valid @RequestBody UpdateTravelRequest request) {
        return travelService.update(parseTravelId(id), request);
    }

    @DeleteMapping("/travels/{id}")
    public ResponseEntity<Void> deleteTravel(@PathVariable String id) {
        travelService.delete(parseTravelId(id));
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

    private UUID parseTravelId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found");
        }
    }
}
