package com.nft.backend.dto.travel;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTravelRequest(
        @NotNull UUID userId,
        @NotBlank String title,
        String destination,
        String description,
        LocalDate startDate,
        LocalDate endDate) {
}
