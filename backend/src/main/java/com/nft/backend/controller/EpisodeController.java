package com.nft.backend.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.nft.backend.dto.episode.CreateEpisodeRequest;
import com.nft.backend.dto.episode.EpisodeResponse;
import com.nft.backend.dto.episode.UpdateEpisodeRequest;
import com.nft.backend.dto.episode.UpdateEpisodeStatusRequest;
import com.nft.backend.service.EpisodeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/travels/{travelId}/episodes")
public class EpisodeController {

    private final EpisodeService episodeService;

    public EpisodeController(EpisodeService episodeService) {
        this.episodeService = episodeService;
    }

    @PostMapping
    public ResponseEntity<EpisodeResponse> createEpisode(
            @PathVariable UUID travelId,
            @Valid @RequestBody CreateEpisodeRequest request) {
        EpisodeResponse episode = episodeService.create(travelId, request);
        return ResponseEntity
                .created(URI.create("/api/travels/" + travelId + "/episodes/" + episode.id()))
                .body(episode);
    }

    @GetMapping
    public List<EpisodeResponse> getEpisodesForTravel(@PathVariable UUID travelId) {
        return episodeService.getByTravel(travelId);
    }

    @GetMapping("/{episodeId}")
    public EpisodeResponse getEpisode(@PathVariable UUID travelId, @PathVariable UUID episodeId) {
        return episodeService.get(travelId, episodeId);
    }

    @PutMapping("/{episodeId}")
    public EpisodeResponse updateEpisode(
            @PathVariable UUID travelId,
            @PathVariable UUID episodeId,
            @Valid @RequestBody UpdateEpisodeRequest request) {
        return episodeService.update(travelId, episodeId, request);
    }

    @PatchMapping("/{episodeId}/status")
    public EpisodeResponse changeEpisodeStatus(
            @PathVariable UUID travelId,
            @PathVariable UUID episodeId,
            @Valid @RequestBody UpdateEpisodeStatusRequest request) {
        return episodeService.changeStatus(travelId, episodeId, request.status());
    }

    @PatchMapping("/{episodeId}/favorite")
    public EpisodeResponse changeEpisodeFavorite(
            @PathVariable UUID travelId,
            @PathVariable UUID episodeId,
            @RequestParam boolean favorite) {
        return episodeService.changeFavorite(travelId, episodeId, favorite);
    }

    @DeleteMapping("/{episodeId}")
    public ResponseEntity<Void> deleteEpisode(@PathVariable UUID travelId, @PathVariable UUID episodeId) {
        episodeService.delete(travelId, episodeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{episodeId}/share")
    public EpisodeResponse shareEpisode(@PathVariable UUID travelId, @PathVariable UUID episodeId) {
        return episodeService.enableSharing(travelId, episodeId);
    }

    @PostMapping("/{episodeId}/export")
    public EpisodeResponse exportEpisode(
            @PathVariable UUID travelId,
            @PathVariable UUID episodeId,
            @RequestParam(defaultValue = "false") boolean fail) {
        return episodeService.export(travelId, episodeId, fail);
    }
}
