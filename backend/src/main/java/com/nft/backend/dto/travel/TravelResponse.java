package com.nft.backend.dto.travel;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.nft.backend.model.Profile;
import com.nft.backend.model.Travel;

public record TravelResponse(
        UUID id,
        UUID userId,
        String title,
        String destination,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt,
        int episodeCount,
        int photoCount) {

    public static TravelResponse fromEntity(Travel travel) {
        Profile user = travel.getUser();
        return new TravelResponse(
                travel.getId(),
                user == null ? null : user.getId(),
                travel.getTitle(),
                travel.getDestination(),
                travel.getDescription(),
                travel.getStartDate(),
                travel.getEndDate(),
                travel.getCreatedAt(),
                travel.getEpisodes().size(),
                travel.getPhotos().size());
    }
}
