package com.nft.backend.dto.photo;

import java.time.Instant;
import java.util.UUID;

import com.nft.backend.model.Photo;

public record PhotoResponse(
        UUID id,
        UUID travelId,
        String filename,
        long size,
        String type,
        Instant uploadedAt) {

    public static PhotoResponse fromEntity(Photo photo) {
        return new PhotoResponse(
                photo.getId(),
                photo.getTravel().getId(),
                photo.getFilename(),
                photo.getSize(),
                photo.getType(),
                photo.getUploadedAt());
    }
}
