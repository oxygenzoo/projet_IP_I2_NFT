package com.nft.backend.dto.episode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.nft.backend.model.Episode;
import com.nft.backend.model.EpisodeStatus;

public record EpisodeResponse(
        UUID id,
        UUID travelId,
        int episodeNumber,
        String title,
        String locationName,
        LocalDate episodeDate,
        String introText,
        String outroText,
        String musicMood,
        EpisodeStatus status,
        String shareToken,
        String exportStatus,
        String videoUrl,
        boolean favorite,
        String sharedBy,
        Instant generatedAt) {

    public static EpisodeResponse fromEntity(Episode episode) {
        return new EpisodeResponse(
                episode.getId(),
                episode.getTravel().getId(),
                episode.getEpisodeNumber(),
                episode.getTitle(),
                episode.getLocationName(),
                episode.getEpisodeDate(),
                episode.getIntroText(),
                episode.getOutroText(),
                episode.getMusicMood(),
                episode.getStatus(),
                episode.getShareToken(),
                episode.getVideoUrl() == null || episode.getVideoUrl().isBlank() ? episode.getExportStatus() : "ready",
                episode.getVideoUrl(),
                episode.isFavorite(),
                sharedBy(episode),
                episode.getGeneratedAt());
    }

    private static String sharedBy(Episode episode) {
        if (episode.getTravel().getUser() == null) {
            return "";
        }

        String email = episode.getTravel().getUser().getEmail();
        if (email == null || email.isBlank()) {
            return "";
        }

        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }
}
