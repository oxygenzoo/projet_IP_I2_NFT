package com.nft.backend.dto.creation;

import java.time.Instant;
import java.util.UUID;

import com.nft.backend.model.CreationSession;

public record CreationSessionResponse(
        UUID id,
        UUID ownerId,
        UUID travelId,
        UUID episodeId,
        String status,
        String resultVideoUrl,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt) {

    public static CreationSessionResponse fromEntity(CreationSession session) {
        return new CreationSessionResponse(
                session.getId(),
                session.getOwnerId(),
                session.getTravelId(),
                session.getEpisodeId(),
                session.getStatus(),
                session.getResultVideoUrl(),
                session.getErrorMessage(),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }
}
