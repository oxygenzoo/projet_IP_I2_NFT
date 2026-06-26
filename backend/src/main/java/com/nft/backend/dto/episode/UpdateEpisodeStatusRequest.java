package com.nft.backend.dto.episode;

import com.nft.backend.model.EpisodeStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEpisodeStatusRequest(@NotNull EpisodeStatus status) {
}
