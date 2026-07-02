package com.nft.backend.dto.user;

import java.time.Instant;
import java.util.UUID;

import com.nft.backend.model.User;

public record UserDto(
        UUID id,
        String email,
        Instant lastLogin,
        Boolean consentRgpd,
        Instant consentDate,
        String language) {

    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getLastLogin(),
                user.getConsentRgpd(),
                user.getConsentDate(),
                user.getLanguage());
    }
}
