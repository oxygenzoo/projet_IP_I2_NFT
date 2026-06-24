package com.nft.backend.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.nft.backend.dto.photo.PhotoMetadataRequest;
import com.nft.backend.dto.photo.PhotoResponse;
import com.nft.backend.service.PhotoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/travels/{travelId}/photos")
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @PostMapping("/metadata")
    public ResponseEntity<PhotoResponse> createPhotoMetadata(
            @PathVariable UUID travelId,
            @Valid @RequestBody PhotoMetadataRequest request) {
        PhotoResponse photo = photoService.create(travelId, request);
        return ResponseEntity
                .created(URI.create("/api/travels/" + travelId + "/photos/" + photo.id()))
                .body(photo);
    }

    @PostMapping("/metadata/batch")
    public ResponseEntity<List<PhotoResponse>> createPhotoMetadataBatch(
            @PathVariable UUID travelId,
            @Valid @RequestBody List<@Valid PhotoMetadataRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one photo metadata item is required");
        }

        List<PhotoResponse> photos = photoService.createAll(travelId, requests);
        return ResponseEntity.created(URI.create("/api/travels/" + travelId + "/photos")).body(photos);
    }

    @GetMapping
    public List<PhotoResponse> getPhotosForTravel(@PathVariable UUID travelId) {
        return photoService.getByTravel(travelId);
    }
}
