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
import com.nft.backend.model.EpisodeScene;
import com.nft.backend.model.EpisodeStatus;
import com.nft.backend.model.Photo;
import com.nft.backend.model.Travel;
import com.nft.backend.repository.EpisodeRepository;
import com.nft.backend.repository.TravelRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TravelCatalogService {

    private final TravelRepository travelRepository;
    private final EpisodeRepository episodeRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public TravelCatalogService(
            TravelRepository travelRepository,
            EpisodeRepository episodeRepository,
            AuthenticatedUserService authenticatedUserService) {
        this.travelRepository = travelRepository;
        this.episodeRepository = episodeRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Transactional(readOnly = true)
    public List<TravelDto> getTravels() {
        return authenticatedUserService.currentUser()
                .map((user) -> travelRepository.findAccessibleByIdentity(user.id(), user.email()))
                .orElse(List.of())
                .stream()
                .map(this::toTravelDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TravelDto> getTravel(String id) {
        AuthenticatedUserService.AuthenticatedUser user = authenticatedUserService.currentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
        return parseUuid(id)
                .flatMap((uuid) -> travelRepository.findAccessibleByIdentity(user.id(), user.email())
                        .stream()
                        .filter((travel) -> travel.getId().equals(uuid))
                        .findFirst())
                .map(this::toTravelDto);
    }

    @Transactional(readOnly = true)
    public List<EpisodeDto> getEpisodes() {
        return authenticatedUserService.currentUser()
                .map((user) -> episodeRepository.findAccessibleByIdentity(user.id(), user.email()))
                .orElse(List.of())
                .stream()
                .map((episode) -> toEpisodeDto(episode, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<EpisodeDto> getEpisode(String id) {
        AuthenticatedUserService.AuthenticatedUser user = authenticatedUserService.currentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
        return parseUuid(id)
                .flatMap((uuid) -> episodeRepository.findAccessibleByIdentityAndId(uuid, user.id(), user.email()))
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
                response == null ? "" : valueOrDefault(response.message(), "Souvenir généré par IA.")));

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
        String videoUrl = firstVideoUrl(response);
        if (!videoUrl.isBlank()) {
            episode.updateExport("ready", videoUrl);
        }
        addGeneratedScenes(episode, firstEpisode(response));

        return toTravelDto(travelRepository.findById(episode.getTravel().getId()).orElse(travel));
    }

    private TravelDto toTravelDto(Travel travel) {
        List<EpisodeDto> episodes = episodeRepository.findByTravelIdOrderByEpisodeNumberAsc(travel.getId())
                .stream()
                .map((episode) -> toEpisodeDto(episode, false))
                .toList();

        int persistedPhotoCount = travel.getPhotos() == null ? 0 : travel.getPhotos().size();
        int photoCount = persistedPhotoCount > 0
                ? persistedPhotoCount
                : episodes.stream().mapToInt((episode) -> episode.scenes().size()).sum();
        String cover = travel.getPhotos() == null || travel.getPhotos().isEmpty()
                ? ""
                : travel.getPhotos().getFirst().getImageUrl();

        return new TravelDto(
                travel.getId().toString(),
                travel.getTitle(),
                valueOrDefault(travel.getDestination(), ""),
                valueOrDefault(travel.getDestination(), ""),
                Year.now().getValue(),
                "Voyage souvenir",
                valueOrDefault(travel.getDescription(), ""),
                cover,
                cover,
                cover,
                episodes.size() + " souvenir(s)",
                episodes.size(),
                photoCount,
                episodes.isEmpty() ? 0 : 100,
                episodes.isEmpty() ? "À générer" : "Prêt",
                false,
                List.of("IA", "Souvenirs"),
                episodes,
                travel.getUser() == null ? null : travel.getUser().getId().toString(),
                travel.getStartDate(),
                travel.getEndDate(),
                travel.getCreatedAt());
    }

    private EpisodeDto toEpisodeDto(Episode episode) {
        return toEpisodeDto(episode, true);
    }

    private EpisodeDto toEpisodeDto(Episode episode, boolean includeScenes) {
        int sceneCount = episode.getScenes() == null ? 0 : episode.getScenes().size();
        List<Photo> photos = episode.getTravel().getPhotos() == null ? List.of() : episode.getTravel().getPhotos();
        int photoCount = photos.isEmpty() ? sceneCount : photos.size();
        String cover = firstDisplayablePhotoUrl(photos);

        return new EpisodeDto(
                episode.getId().toString(),
                episode.getTravel().getId().toString(),
                1,
                episode.getEpisodeNumber(),
                episode.getTitle(),
                valueOrDefault(episode.getMusicMood(), ""),
                valueOrDefault(episode.getIntroText(), ""),
                durationFor(episode),
                valueOrDefault(episode.getLocationName(), ""),
                formatDate(episode.getEpisodeDate()),
                photoCount,
                cover,
                cover,
                episode.getStatus() == EpisodeStatus.READY ? 100 : 0,
                "",
                exportStatusFor(episode),
                valueOrDefault(episode.getVideoUrl(), ""),
                episode.isFavorite(),
                List.of(),
                includeScenes
                        ? episode.getScenes().stream()
                                .map((scene) -> toSceneDto(scene, fallbackPhotoUrl(photos, scene.getOrder())))
                                .toList()
                        : List.of());
    }

    private SceneDto toSceneDto(EpisodeScene scene, String fallbackImageUrl) {
        return new SceneDto(
                scene.getId().toString(),
                scene.getOrder(),
                "Scene " + scene.getOrder(),
                "",
                scene.getVoiceOverText(),
                scene.getType(),
                scene.getGenerationStatus(),
                valueOrDefault(scene.getPhotoUrl(), fallbackImageUrl),
                scene.isAiReconstructed(),
                scene.getAiPrompt());
    }

    private String fallbackPhotoUrl(List<Photo> photos, int sceneOrder) {
        if (photos == null || photos.isEmpty()) {
            return "";
        }

        int index = Math.max(0, sceneOrder - 1) % photos.size();
        return displayablePhotoUrl(photos.get(index));
    }

    private String firstDisplayablePhotoUrl(List<Photo> photos) {
        if (photos == null || photos.isEmpty()) {
            return "";
        }

        return photos.stream()
                .map(this::displayablePhotoUrl)
                .filter((url) -> !url.isBlank())
                .findFirst()
                .orElse("");
    }

    private String displayablePhotoUrl(Photo photo) {
        String imageUrl = photo == null ? "" : valueOrDefault(photo.getImageUrl(), "");
        if (imageUrl.isBlank() || imageUrl.startsWith("metadata-only/")) {
            return "";
        }
        return imageUrl;
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
        return value instanceof String title && !title.isBlank() ? title : "Souvenir généré";
    }

    private String firstEpisodeSummary(GenerationResponse response) {
        Map<String, Object> episode = firstEpisode(response);
        Object value = episode.get("resume");
        if (value instanceof String summary && !summary.isBlank()) {
            return summary;
        }

        Object intro = episode.get("intro");
        return intro instanceof String introText ? introText : "";
    }

    @SuppressWarnings("unchecked")
    private void addGeneratedScenes(Episode episode, Map<String, Object> generatedEpisode) {
        Object scenesValue = generatedEpisode.get("scenes");
        if (!(scenesValue instanceof List<?> scenes)) {
            return;
        }

        for (Object sceneValue : scenes) {
            if (sceneValue instanceof Map<?, ?> rawScene) {
                Map<String, Object> scene = (Map<String, Object>) rawScene;
                episode.addScene(new EpisodeScene(
                        episode,
                        numberValue(scene.get("scene_numero"), episode.getScenes().size() + 1),
                        null,
                        null,
                        stringValue(scene.get("voix_off")),
                        "souvenir",
                        "completed",
                        false,
                        null));
            }
        }
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

    private String firstVideoUrl(GenerationResponse response) {
        if (response == null || response.videos() == null || response.videos().isEmpty()) {
            return "";
        }

        return valueOrDefault(response.videos().getFirst(), "");
    }

    private int numberValue(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private String stringValue(Object value) {
        return value instanceof String text ? text : "";
    }

    private String exportStatusFor(Episode episode) {
        if (episode.getVideoUrl() != null && !episode.getVideoUrl().isBlank()) {
            return "ready";
        }

        return valueOrDefault(episode.getExportStatus(), "idle");
    }

    private String durationFor(Episode episode) {
        int sceneCount = episode.getScenes() == null ? 0 : Math.min(episode.getScenes().size(), 6);
        int seconds = Math.round(2.5f + textCardSeconds(episode.getIntroText()) + sceneCount * 4f + textCardSeconds(episode.getOutroText()));
        if (seconds < 60) {
            return seconds + " s";
        }

        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return remainingSeconds == 0 ? minutes + " min" : minutes + " min " + remainingSeconds + " s";
    }

    private float textCardSeconds(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        return Math.max(1, (int) Math.ceil(text.trim().length() / 58.0)) * 1.8f;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
