package com.nft.backend.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.nft.backend.dto.episode.CreateEpisodeRequest;
import com.nft.backend.dto.generation.GenerationResponse;
import com.nft.backend.dto.photo.PhotoMetadataRequest;
import com.nft.backend.model.EpisodeStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiGenerationService {

    private final RestClient restClient;
    private final TravelCatalogService travelCatalogService;
    private final PhotoService photoService;
    private final EpisodeService episodeService;

    public AiGenerationService(
            @Value("${app.ai.service-url:http://localhost:8000}") String aiServiceUrl,
            TravelCatalogService travelCatalogService,
            PhotoService photoService,
            EpisodeService episodeService) {
        this.restClient = RestClient.builder().baseUrl(aiServiceUrl).build();
        this.travelCatalogService = travelCatalogService;
        this.photoService = photoService;
        this.episodeService = episodeService;
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

        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("title", valueOrDefault(title, "Mon voyage"));
        body.part("destination", valueOrDefault(destination, ""));
        body.part("preferences", valueOrDefault(preferences, "{}"));

        for (MultipartFile image : images) {
            if (image.isEmpty()) {
                continue;
            }

            try {
                body.part("images", multipartResource(image))
                        .filename(safeFilename(image.getOriginalFilename()))
                        .contentType(resolveContentType(image));
            } catch (IOException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de lire une photo envoyee.", exception);
            }
        }

        List<String> filenames = images.stream()
                .filter((image) -> !image.isEmpty())
                .map((image) -> safeFilename(image.getOriginalFilename()))
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
                        images.stream().map((image) -> safeFilename(image.getOriginalFilename())).toList());
            } else {
                registerTravelWorkflow(response, travelId, images);
            }

            return response;
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

    private ByteArrayResource multipartResource(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String filename = safeFilename(file.getOriginalFilename());
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private MediaType resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
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

    private void registerTravelWorkflow(GenerationResponse response, String travelId, List<MultipartFile> images) {
        if (travelId == null || travelId.isBlank()) {
            return;
        }

        UUID travelUuid = parseTravelId(travelId);
        List<PhotoMetadataRequest> metadata = images.stream()
                .filter((image) -> !image.isEmpty())
                .map((image) -> new PhotoMetadataRequest(
                        safeFilename(image.getOriginalFilename()),
                        image.getSize(),
                        resolveImageType(image)))
                .toList();

        if (!metadata.isEmpty()) {
            photoService.createAll(travelUuid, metadata);
        }

        Map<String, Object> episode = firstEpisode(response.script());
        episodeService.create(travelUuid, new CreateEpisodeRequest(
                numberValue(episode.get("episode_numero"), 1),
                cleanOrDefault(stringValue(episode.get("episode_titre")), "Episode genere"),
                cleanOrDefault(stringValue(episode.get("lieu")), ""),
                parseDate(stringValue(episode.get("date"))),
                cleanOrDefault(response.message(), ""),
                "Generation IA terminee.",
                cleanOrDefault(stringValue(response.script() == null ? null : response.script().get("preferences")), ""),
                EpisodeStatus.READY));
    }

    private UUID parseTravelId(String travelId) {
        try {
            return UUID.fromString(travelId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "travelId invalide.", exception);
        }
    }

    private String resolveImageType(MultipartFile image) {
        String type = image.getContentType();
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

    private int numberValue(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private String cleanOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
