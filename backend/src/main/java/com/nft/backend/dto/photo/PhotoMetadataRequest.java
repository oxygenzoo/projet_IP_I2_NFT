package com.nft.backend.dto.photo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PhotoMetadataRequest(
        @NotBlank String filename,
        @NotNull @Positive Long size,
        @NotBlank String type) {
}
