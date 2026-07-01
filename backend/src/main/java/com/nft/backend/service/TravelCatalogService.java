package com.nft.backend.service;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.nft.backend.dto.generation.GenerationResponse;
import com.nft.backend.dto.travel.EpisodeDto;
import com.nft.backend.dto.travel.SceneDto;
import com.nft.backend.dto.travel.TravelDto;
import com.nft.backend.model.Episode;
import com.nft.backend.model.EpisodeStatus;
import com.nft.backend.model.Travel;
import com.nft.backend.repository.EpisodeRepository;
import com.nft.backend.repository.TravelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TravelCatalogService {

    private final TravelRepository travelRepository;
    private final EpisodeRepository episodeRepository;

    public TravelCatalogService(TravelRepository travelRepository, EpisodeRepository episodeRepository) {
        this.travelRepository = travelRepository;
        this.episodeRepository = episodeRepository;
    }

    @Transactional(readOnly = true)
    public List<TravelDto> getTravels() {
        return travelRepository.findAll().stream()
                .map(this::toTravelDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TravelDto> getTravel(String id) {
        return parseUuid(id)
                .flatMap(travelRepository::findById)
                .map(this::toTravelDto);
    }

    @Transactional(readOnly = true)
    public List<EpisodeDto> getEpisodes() {
        return episodeRepository.findAll().stream()
                .map(this::toEpisodeDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<EpisodeDto> getEpisode(String id) {
        return parseUuid(id)
                .flatMap(episodeRepository::findById)
                .map(this::toEpisodeDto);
    }

    @Transactional
    public TravelDto registerGeneration(
            GenerationResponse response,
            String title,
            String destination,
            int photoCount,
            List<String> imageUrls) {
        Travel travel = travelRepository.save(new Travel(
                valueOrDefault(title, "Mon voyage"),
                valueOrDefault(destination, ""),
                response == null ? "" : valueOrDefault(response.message(), "Episode genere par IA.")));

        Episode episode = episodeRepository.save(new Episode(
                travel,
                firstEpisodeNumber(response),
                firstEpisodeTitle(response),
                valueOrDefault(destination, ""),
                null,
                firstEpisodeSummary(response),
                "",
                "Cinematographique",
                EpisodeStatus.READY));

        return toTravelDto(travelRepository.findById(episode.getTravel().getId()).orElse(travel));
    }

    private TravelDto toTravelDto(Travel travel) {
        List<EpisodeDto> episodes = episodeRepository.findByTravelIdOrderByEpisodeNumberAsc(travel.getId())
                .stream()
                .map(this::toEpisodeDto)
                .toList();

        int photoCount = travel.getPhotos() == null ? 0 : travel.getPhotos().size();
        String cover = travel.getPhotos() == null || travel.getPhotos().isEmpty()
                ? ""
                : travel.getPhotos().getFirst().getImageUrl();

        return new TravelDto(
                travel.getId().toString(),
                travel.getTitle(),
                valueOrDefault(travel.getDestination(), ""),
                valueOrDefault(travel.getDestination(), ""),
                Year.now().getValue(),
                "Serie souvenir",
                valueOrDefault(travel.getDescription(), ""),
                cover,
                cover,
                cover,
                episodes.size() + " episode(s)",
                episodes.size(),
                photoCount,
                episodes.isEmpty() ? 0 : 100,
                episodes.isEmpty() ? "A generer" : "Pret",
                false,
                List.of("IA", "Souvenirs"),
                episodes);
    }

    private EpisodeDto toEpisodeDto(Episode episode) {
        return new EpisodeDto(
                episode.getId().toString(),
                episode.getTravel().getId().toString(),
                1,
                episode.getEpisodeNumber(),
                episode.getTitle(),
                valueOrDefault(episode.getMusicMood(), ""),
                valueOrDefault(episode.getIntroText(), ""),
                "3 min",
                valueOrDefault(episode.getLocationName(), ""),
                formatDate(episode.getEpisodeDate()),
                0,
                "",
                valueOrDefault(episode.getVideoUrl(), ""),
                episode.getStatus() == EpisodeStatus.READY ? 100 : 0,
                valueOrDefault(episode.getExportStatus(), "idle"),
                List.<SceneDto>of());
    }

    private Optional<UUID> parseUuid(String id) {
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private int firstEpisodeNumber(GenerationResponse response) {
        Map<String, Object> episode = firstEpisode(response);
        Object value = episode.get("episode_numero");
        return value instanceof Number number ? number.intValue() : 1;
    }

    private String firstEpisodeTitle(GenerationResponse response) {
        Map<String, Object> episode = firstEpisode(response);
        Object value = episode.get("episode_titre");
        return value instanceof String title && !title.isBlank() ? title : "Episode genere";
    }

    private String firstEpisodeSummary(GenerationResponse response) {
        Map<String, Object> episode = firstEpisode(response);
        Object value = episode.get("resume");
        return value instanceof String summary ? summary : "";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstEpisode(GenerationResponse response) {
        if (response == null || response.script() == null) {
            return Map.of();
        }

        Object episodes = response.script().get("episodes");
        if (episodes instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }

        return Map.of();
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
