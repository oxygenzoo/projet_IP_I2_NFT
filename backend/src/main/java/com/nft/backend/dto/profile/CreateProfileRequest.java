package com.nft.backend.dto.profile;

import jakarta.validation.constraints.NotBlank;

public record CreateProfileRequest(
        @NotBlank String email,
        String fullName,
        String avatarUrl) {
}
