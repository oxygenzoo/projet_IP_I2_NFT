package com.nft.backend.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.nft.backend.dto.photo.PhotoMetadataRequest;
import com.nft.backend.dto.photo.PhotoResponse;
import com.nft.backend.service.PhotoService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/travels/{travelId}/photos")
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoResponse> uploadPhoto(
            @PathVariable UUID travelId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean consentRgpd) {
        PhotoResponse photo = photoService.upload(travelId, file, consentRgpd);
        return ResponseEntity
                .created(URI.create(photo.imageUrl()))
                .body(photo);
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

    @GetMapping("/{photoId}/file")
    public ResponseEntity<Resource> getPhotoFile(@PathVariable UUID travelId, @PathVariable UUID photoId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photoService.getFileType(travelId, photoId)))
                .body(photoService.getFile(travelId, photoId));
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable UUID travelId, @PathVariable UUID photoId) {
        photoService.delete(travelId, photoId);
        return ResponseEntity.noContent().build();
    }
}
