package com.nft.backend.dto.travel;

import java.util.List;

public record EpisodeDto(
        String id,
        String travelId,
        int seasonNumber,
        int episodeNumber,
        String title,
        String subtitle,
        String summary,
        String duration,
        String location,
        String date,
        int photoCount,
        String coverImage,
        String videoStill,
        int progress,
        String remaining,
        List<String> keyMoments,
        List<SceneDto> scenes) {
}
