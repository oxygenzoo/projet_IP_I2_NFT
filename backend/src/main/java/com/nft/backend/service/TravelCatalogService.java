package com.nft.backend.service;

import java.util.List;
import java.util.Optional;

import com.nft.backend.dto.travel.EpisodeDto;
import com.nft.backend.dto.travel.TravelDto;
import org.springframework.stereotype.Service;

@Service
public class TravelCatalogService {

    public List<TravelDto> getTravels() {
        return List.of();
    }

    public Optional<TravelDto> getTravel(String id) {
        return Optional.empty();
    }

    public List<EpisodeDto> getEpisodes() {
        return List.of();
    }

    public Optional<EpisodeDto> getEpisode(String id) {
        return Optional.empty();
    }
}
