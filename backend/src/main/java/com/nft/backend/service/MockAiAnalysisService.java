package com.nft.backend.service;

import java.util.List;

import com.nft.backend.dto.ai.PhotoAnalysisDto;
import org.springframework.stereotype.Service;

@Service
public class MockAiAnalysisService {

    private static final List<String> TAGS = List.of("paysage", "groupe", "monument");
    private static final List<String> LOCATIONS = List.of("Denpasar", "Ubud", "Canggu");

    public List<PhotoAnalysisDto> analyzePhotos(List<String> photoIds) {
        if (photoIds == null || photoIds.isEmpty()) {
            return List.of();
        }

        return photoIds.stream()
                .filter((photoId) -> photoId != null && !photoId.isBlank())
                .map((photoId) -> new PhotoAnalysisDto(
                        photoId,
                        qualityScore(photoId),
                        TAGS,
                        LOCATIONS.get(Math.floorMod(photoId.hashCode(), LOCATIONS.size()))))
                .toList();
    }

    private int qualityScore(String photoId) {
        return 60 + Math.floorMod(photoId.hashCode(), 36);
    }
}
