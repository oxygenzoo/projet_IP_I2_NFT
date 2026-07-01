package com.nft.backend.dto.travel;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record SaveTravelRequest(
        UUID userId,
        @NotBlank String title,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        String description) {
}
