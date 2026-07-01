package com.nft.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.nft.backend.dto.travel.EpisodeDto;
import com.nft.backend.dto.travel.TravelDto;
import com.nft.backend.model.Travel;
import com.nft.backend.repository.TravelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TravelCatalogService {

    private final TravelRepository travelRepository;

    public TravelCatalogService(TravelRepository travelRepository) {
        this.travelRepository = travelRepository;
    }

    @Transactional(readOnly = true)
    public List<TravelDto> getTravels() {
        return travelRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TravelDto> getTravel(String id) {
        return parseUuid(id)
                .flatMap(travelRepository::findById)
                .map(this::toDto);
    }

    public List<EpisodeDto> getEpisodes() {
        return List.of();
    }

    public Optional<EpisodeDto> getEpisode(String id) {
        return Optional.empty();
    }

    private Optional<UUID> parseUuid(String id) {
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private TravelDto toDto(Travel travel) {
        return new TravelDto(
                travel.getId().toString(),
                travel.getTitle(),
                travel.getDestination(),
                null,
                yearOf(travel.getStartDate()),
                travel.getDestination(),
                travel.getDescription(),
                null,
                null,
                null,
                null,
                travel.getEpisodes().size(),
                travel.getPhotos().size(),
                0,
                null,
                false,
                List.of(),
                List.of());
    }

    private int yearOf(LocalDate date) {
        return date == null ? 0 : date.getYear();
    }
}
