package com.nft.backend.dto.contribution;

import java.time.Instant;
import java.util.UUID;

import com.nft.backend.model.ContributionLink;

public record ContributionLinkResponse(
        UUID id,
        UUID travelId,
        String token,
        String url,
        Instant expiresAt,
        Instant createdAt,
        Instant openedAt,
        Integer uploadCount,
        Instant lastUploadAt) {

    public static ContributionLinkResponse fromEntity(ContributionLink link, String publicBaseUrl) {
        String baseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
        return new ContributionLinkResponse(
                link.getId(),
                link.getTravel().getId(),
                link.getToken(),
                baseUrl + "/contribute/" + link.getToken(),
                link.getExpiresAt(),
                link.getCreatedAt(),
                link.getOpenedAt(),
                link.getUploadCount(),
                link.getLastUploadAt());
    }
}
