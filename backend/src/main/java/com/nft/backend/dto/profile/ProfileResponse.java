package com.nft.backend.dto.profile;

import java.time.Instant;
import java.util.UUID;

import com.nft.backend.model.Profile;

public record ProfileResponse(
        UUID id,
        String email,
        String fullName,
        String avatarUrl,
        boolean consentRgpd,
        Instant createdAt) {

    public static ProfileResponse fromEntity(Profile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getEmail(),
                profile.getFullName(),
                profile.getAvatarUrl(),
                profile.isConsentRgpd(),
                profile.getCreatedAt());
    }
}
