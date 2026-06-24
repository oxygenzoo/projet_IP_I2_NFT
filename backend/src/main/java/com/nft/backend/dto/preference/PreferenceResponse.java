package com.nft.backend.dto.preference;

import java.time.Instant;

public record PreferenceResponse(
        String id,
        String travelId,
        String style,
        String people,
        String moments,
        String tone,
        Instant createdAt,
        Instant updatedAt) {
}
