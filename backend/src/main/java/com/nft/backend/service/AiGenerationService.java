package com.nft.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.nft.backend.dto.episode.CreateEpisodeRequest;
import com.nft.backend.dto.generation.GenerationResponse;
import com.nft.backend.dto.photo.PhotoMetadataRequest;
import com.nft.backend.model.EpisodeStatus;
import com.nft.backend.model.Photo;
import com.nft.backend.model.Travel;
import com.nft.backend.repository.PhotoRepository;
import com.nft.backend.repository.TravelRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiGenerationService {

    private final RestClient restClient;
    private final TravelCatalogService travelCatalogService;
    private final PhotoService photoService;
    private final EpisodeService episodeService;
    private final PhotoRepository photoRepository;
    private final TravelRepository travelRepository;

    private record GenerationImage(String filename, String contentType, byte[] bytes, long size) {
    }

    public AiGenerationService(
            @Value("${app.ai.service-url:http://localhost:8000}") String aiServiceUrl,
            TravelCatalogService travelCatalogService,
            PhotoService photoService,
            EpisodeService episodeService,
            PhotoRepository photoRepository,
            TravelRepository travelRepository) {
        this.restClient = RestClient.builder().baseUrl(aiServiceUrl).build();
        this.travelCatalogService = travelCatalogService;
        this.photoService = photoService;
        this.episodeService = episodeService;
        this.photoRepository = photoRepository;
        this.travelRepository = travelRepository;
    }

    public GenerationResponse generateEpisode(
            List<MultipartFile> images,
            String title,
            String destination,
            String preferences,
            String travelId) {

        if (images == null || images.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ajoutez au moins une photo.");
        }

        List<GenerationImage> generationImages = images.stream()
                .filter((image) -> !image.isEmpty())
                .map(this::toGenerationImage)
                .toList();
        return generateEpisodeImages(generationImages, title, destination, preferences, travelId, true, null);
    }

    public GenerationResponse generateEpisodeFromTravel(UUID ownerId, UUID travelId, String preferences) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found"));

        if (travel.getUser() == null || !ownerId.equals(travel.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Travel access denied");
        }

        List<Photo> photos = photoRepository.findByTravelIdOrderByIdAsc(travelId);
        List<GenerationImage> images = photos.stream()
                .map(this::toGenerationImage)
                .toList();

        return generateEpisodeImages(
                images,
                valueOrDefault(travel.getTitle(), "Mon voyage"),
                valueOrDefault(travel.getDestination(), ""),
                preferences,
                travelId.toString(),
                false,
                ownerId);
    }

    private GenerationResponse generateEpisodeImages(
            List<GenerationImage> images,
            String title,
            String destination,
            String preferences,
            String travelId,
            boolean persistPhotoMetadata,
            UUID ownerId) {
        if (images == null || images.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ajoutez au moins une photo.");
        }

        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("title", valueOrDefault(title, "Mon voyage"));
        body.part("destination", valueOrDefault(destination, ""));
        body.part("preferences", valueOrDefault(preferences, "{}"));

        for (GenerationImage image : images) {
            body.part("images", multipartResource(image))
                    .filename(image.filename())
                    .contentType(resolveContentType(image.contentType()));
        }

        List<String> filenames = images.stream()
                .map(GenerationImage::filename)
                .toList();

        if (filenames.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ajoutez au moins une photo lisible.");
        }

        try {
            GenerationResponse response = restClient.post()
                    .uri("/ai/episodes")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body.build())
                    .retrieve()
                    .body(GenerationResponse.class);

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Le service IA n'a pas renvoye de resultat.");
            }

            if (travelId == null || travelId.isBlank()) {
                travelCatalogService.registerGeneration(
                        response,
                        valueOrDefault(title, "Mon voyage"),
                        valueOrDefault(destination, ""),
                        images.size(),
                        filenames);
            } else {
                registerTravelWorkflow(response, travelId, images, persistPhotoMetadata, ownerId);
            }

            return response;
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Erreur du service IA: " + cleanOrDefault(exception.getResponseBodyAsString(), exception.getMessage()),
                    exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Le service IA est indisponible. Aucune generation locale n'a ete creee.",
                    exception);
        }
    }

    public GenerationResponse generateEpisode(
            List<MultipartFile> images,
            String title,
            String destination,
            String preferences) {
        return generateEpisode(images, title, destination, preferences, null);
    }

    private ByteArrayResource multipartResource(GenerationImage image) {
        return new ByteArrayResource(image.bytes()) {
            @Override
            public String getFilename() {
                return image.filename();
            }
        };
    }

    private MediaType resolveContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(contentType);
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "photo.jpg";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private GenerationImage toGenerationImage(MultipartFile image) {
        try {
            return new GenerationImage(
                    safeFilename(image.getOriginalFilename()),
                    resolveImageType(image.getContentType()),
                    image.getBytes(),
                    image.getSize());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de lire une photo envoyée.", exception);
        }
    }

    private GenerationImage toGenerationImage(Photo photo) {
        if (photo == null || photo.getStoragePath() == null || photo.getStoragePath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une photo importée n'a pas de fichier exploitable.");
        }

        try {
            Path path = Path.of(photo.getStoragePath()).toAbsolutePath().normalize();
            if (!Files.exists(path)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Une photo importée est introuvable.");
            }
            return new GenerationImage(
                    safeFilename(photo.getFilename()),
                    resolveImageType(photo.getType()),
                    Files.readAllBytes(path),
                    photo.getSize());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de lire une photo importée.", exception);
        }
    }

    private void registerTravelWorkflow(
            GenerationResponse response,
            String travelId,
            List<GenerationImage> images,
            boolean persistPhotoMetadata,
            UUID ownerId) {
        if (travelId == null || travelId.isBlank()) {
            return;
        }

        UUID travelUuid = parseTravelId(travelId);
        if (persistPhotoMetadata) {
            List<PhotoMetadataRequest> metadata = images.stream()
                    .map((image) -> new PhotoMetadataRequest(image.filename(), image.size(), resolveImageType(image.contentType())))
                    .toList();
            photoService.createAll(travelUuid, metadata);
        }

        Map<String, Object> episode = firstEpisode(response.script());
        List<Photo> photos = photoRepository.findByTravelIdOrderByIdAsc(travelUuid);
        CreateEpisodeRequest request = new CreateEpisodeRequest(
                numberValue(episode.get("episode_numero"), 1),
                cleanOrDefault(stringValue(episode.get("episode_titre")), "Souvenir généré"),
                cleanOrDefault(stringValue(episode.get("lieu")), ""),
                parseDate(stringValue(episode.get("date"))),
                cleanOrDefault(response.message(), ""),
                "Génération IA terminée.",
                cleanOrDefault(stringValue(response.script() == null ? null : response.script().get("preferences")), ""),
                EpisodeStatus.READY);

        if (ownerId == null) {
            episodeService.create(travelUuid, request, firstVideoUrl(response), generatedScenes(episode), photos);
        } else {
            episodeService.createForOwner(ownerId, travelUuid, request, firstVideoUrl(response), generatedScenes(episode), photos);
        }
    }

    private UUID parseTravelId(String travelId) {
        try {
            return UUID.fromString(travelId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "travelId invalide.", exception);
        }
    }

    private String resolveImageType(String type) {
        return type == null || type.isBlank() ? "image/jpeg" : type;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstEpisode(Map<String, Object> script) {
        Object episodesValue = script == null ? null : script.get("episodes");

        if (episodesValue instanceof List<?> episodeList && !episodeList.isEmpty()
                && episodeList.get(0) instanceof Map<?, ?> episode) {
            return (Map<String, Object>) episode;
        }

        return Map.of();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value instanceof String text ? text : "";
    }

    private String firstVideoUrl(GenerationResponse response) {
        if (response == null || response.videos() == null || response.videos().isEmpty()) {
            return "";
        }

        return cleanOrDefault(response.videos().getFirst(), "");
    }

    @SuppressWarnings("unchecked")
    private List<EpisodeService.GeneratedSceneRequest> generatedScenes(Map<String, Object> episode) {
        Object scenesValue = episode.get("scenes");
        if (!(scenesValue instanceof List<?> scenes)) {
            return List.of();
        }

        return scenes.stream()
                .filter((scene) -> scene instanceof Map<?, ?>)
                .map((scene) -> (Map<String, Object>) scene)
                .map((scene) -> new EpisodeService.GeneratedSceneRequest(
                        numberValue(scene.get("scene_numero"), 1),
                        safeFilename(stringValue(scene.get("photo_fichier"))),
                        cleanOrDefault(stringValue(scene.get("voix_off")), stringValue(scene.get("texte_ecran")))))
                .toList();
    }

    private int numberValue(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private String cleanOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
