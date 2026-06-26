package com.nft.backend.service;

import java.io.IOException;
import java.util.List;

import com.nft.backend.dto.generation.GenerationResponse;
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

    public AiGenerationService(@Value("${app.ai.service-url:http://localhost:8000}") String aiServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(aiServiceUrl).build();
    }

    public GenerationResponse generateEpisode(
            List<MultipartFile> images,
            String title,
            String destination,
            String preferences) {

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

            return response;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Le service IA est indisponible. Verifiez AI_SERVICE_URL et le service Python.",
                    exception);
        }
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
}
