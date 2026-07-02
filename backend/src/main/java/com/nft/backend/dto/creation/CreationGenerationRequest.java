package com.nft.backend.dto.creation;

import java.util.UUID;

public record CreationGenerationRequest(
        UUID travelId,
        String preferences) {
}
