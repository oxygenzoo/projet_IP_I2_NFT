package com.nft.backend.controller;

import com.nft.backend.dto.episode.EpisodeResponse;
import com.nft.backend.service.EpisodeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicEpisodeController {

    private final EpisodeService episodeService;

    public PublicEpisodeController(EpisodeService episodeService) {
        this.episodeService = episodeService;
    }

    @GetMapping("/episodes/{shareToken}")
    public EpisodeResponse getPublicEpisode(@PathVariable String shareToken) {
        return episodeService.getPublicEpisode(shareToken);
    }
}
