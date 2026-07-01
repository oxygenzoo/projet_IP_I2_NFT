package com.nft.backend.dto.travel;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

public record UpdateTravelRequest(
        @NotBlank String title,
        String destination,
        String description,
        LocalDate startDate,
        LocalDate endDate) {
}
