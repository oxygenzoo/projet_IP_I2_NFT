package com.nft.backend.dto.photo;

import java.time.Instant;
import java.util.UUID;

import com.nft.backend.model.Photo;

public record PhotoResponse(
        UUID id,
        UUID travelId,
        String filename,
        long size,
        String storageUrl,
        String format,
        double sizeMb,
        String type,
        String imageUrl,
        boolean consentRgpd,
        Instant consentDate,
        Instant uploadedAt) {

    public static PhotoResponse fromEntity(Photo photo) {
        return new PhotoResponse(
                photo.getId(),
                photo.getTravel().getId(),
                photo.getFilename(),
                photo.getSize(),
                photo.getImageUrl(),
                photo.getType(),
                Math.round((photo.getSize() / (1024d * 1024d)) * 100d) / 100d,
                photo.getType(),
                photo.getImageUrl(),
                photo.isConsentRgpd(),
                photo.getConsentDate(),
                photo.getUploadedAt());
    }
}
