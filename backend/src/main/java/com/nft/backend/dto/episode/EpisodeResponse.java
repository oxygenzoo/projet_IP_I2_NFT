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
                episode.getExportStatus(),
                episode.getVideoUrl(),
                episode.getGeneratedAt());
    }
}
