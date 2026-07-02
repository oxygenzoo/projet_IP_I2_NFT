package com.nft.backend.dto.creation;

import java.util.UUID;

public record CreationSessionRequest(
        String status,
        UUID travelId,
        UUID episodeId,
        String resultVideoUrl,
        String errorMessage) {
}
