package com.nft.backend.service;

import java.util.List;
import java.util.Locale;

import com.nft.backend.dto.travel.SynopsisDto;
import org.springframework.stereotype.Service;

@Service
public class EpisodeSynopsisService {

    public SynopsisDto generate(String travel, String preferences, List<String> photos) {
        String place = valueOrDefault(travel, "votre voyage");
        String tone = valueOrDefault(preferences, "inspirant");
        List<String> keyMoments = keyMoments(photos);

        String title = "Souvenirs de " + place;
        String summary = "Un episode " + tone.toLowerCase(Locale.ROOT)
                + " a " + place
                + ", construit autour de " + String.join(", ", keyMoments) + ".";

        return new SynopsisDto(title, summary, keyMoments);
    }

    private List<String> keyMoments(List<String> photos) {
        if (photos == null || photos.isEmpty()) {
            return List.of("arrivee", "decouverte", "conclusion");
        }

        return photos.stream()
                .filter((photo) -> photo != null && !photo.isBlank())
                .map((photo) -> photo.replaceAll("\\.[^.]+$", "").replace('-', ' ').replace('_', ' ').trim())
                .filter((photo) -> !photo.isBlank())
                .limit(3)
                .toList();
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
