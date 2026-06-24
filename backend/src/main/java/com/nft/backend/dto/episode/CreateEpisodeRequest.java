package com.nft.backend.dto.episode;

import java.time.LocalDate;

import com.nft.backend.model.EpisodeStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEpisodeRequest(
        @NotNull @Min(1) Integer episodeNumber,
        @NotBlank String title,
        String locationName,
        LocalDate episodeDate,
        String introText,
        String outroText,
        String musicMood,
        EpisodeStatus status) {
}
