package com.nft.backend.dto.contribution;

import java.time.Instant;
import java.util.UUID;

public record PublicContributionResponse(
        UUID travelId,
        String title,
        Instant expiresAt) {
}
