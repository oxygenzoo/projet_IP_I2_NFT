package com.nft.backend.dto.preference;

import jakarta.validation.constraints.NotBlank;

public record SavePreferenceRequest(
        @NotBlank String style,
        @NotBlank String people,
        @NotBlank String moments,
        @NotBlank String tone) {
}
