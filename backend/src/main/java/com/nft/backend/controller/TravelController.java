package com.nft.backend.controller;

import java.util.List;

import com.nft.backend.dto.travel.EpisodeDto;
import com.nft.backend.dto.travel.TravelDto;
import com.nft.backend.service.TravelCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TravelController {

    private final TravelCatalogService travelCatalogService;

    public TravelController(TravelCatalogService travelCatalogService) {
        this.travelCatalogService = travelCatalogService;
    }

    @GetMapping("/travels")
    public List<TravelDto> getTravels() {
        return travelCatalogService.getTravels();
    }

    @GetMapping("/travels/{id}")
    public ResponseEntity<TravelDto> getTravel(@PathVariable String id) {
        return ResponseEntity.of(travelCatalogService.getTravel(id));
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
