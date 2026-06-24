package com.nft.backend.dto.preference;

import java.time.Instant;
import java.util.UUID;

import com.nft.backend.model.Preference;

public record PreferenceResponse(
        UUID id,
        UUID travelId,
        String style,
        String people,
        String moments,
        String tone,
        Instant createdAt,
        Instant updatedAt) {

    public static PreferenceResponse fromEntity(Preference preference) {
        return new PreferenceResponse(
                preference.getId(),
                preference.getTravel().getId(),
                preference.getStyle(),
                preference.getPeople(),
                preference.getMoments(),
                preference.getTone(),
                preference.getCreatedAt(),
                preference.getUpdatedAt());
    }
}
