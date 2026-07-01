package com.nft.backend.dto.travel;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TravelDto(
        String id,
        String title,
        String destination,
        String country,
        int year,
        String tagline,
        String description,
        String coverImage,
        String heroImage,
        String posterImage,
        String duration,
        int episodeCount,
        int photoCount,
        int progress,
        String remaining,
        boolean featured,
        List<String> moodTags,
        List<EpisodeDto> episodes,
        String userId,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt) {
}
