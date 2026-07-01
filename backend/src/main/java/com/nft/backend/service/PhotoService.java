package com.nft.backend.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.nft.backend.dto.photo.PhotoMetadataRequest;
import com.nft.backend.dto.photo.PhotoResponse;
import com.nft.backend.model.Photo;
import com.nft.backend.model.Travel;
import com.nft.backend.repository.PhotoRepository;
import com.nft.backend.repository.TravelRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PhotoService {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/heic",
            "image/heif");

    private final PhotoRepository photoRepository;
    private final TravelRepository travelRepository;
    private final Path uploadRoot;
    private final long maxFileSize;

    public PhotoService(
            PhotoRepository photoRepository,
            TravelRepository travelRepository,
            @Value("${app.upload.dir:uploads/}") String uploadDir,
            @Value("${app.upload.max-file-size-bytes:10485760}") long maxFileSize) {
        this.photoRepository = photoRepository;
        this.travelRepository = travelRepository;
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
    }

    @Transactional
    public PhotoResponse create(UUID travelId, PhotoMetadataRequest request) {
        Travel travel = findTravel(travelId);
        Photo photo = toPhoto(travel, request);
        return PhotoResponse.fromEntity(photoRepository.save(photo));
    }

    @Transactional
    public List<PhotoResponse> createAll(UUID travelId, List<PhotoMetadataRequest> requests) {
        Travel travel = findTravel(travelId);
        List<Photo> photos = requests.stream()
                .map((request) -> toPhoto(travel, request))
                .toList();

        return photoRepository.saveAll(photos)
                .stream()
                .map(PhotoResponse::fromEntity)
                .toList();
    }

    @Transactional
    public PhotoResponse upload(UUID travelId, MultipartFile file, boolean consentRgpd) {
        Travel travel = findTravel(travelId);
        validateConsent(consentRgpd);
        validateFile(file);

        String type = normalizeType(file.getContentType());
        String storedFilename = UUID.randomUUID() + "-" + safeFilename(file.getOriginalFilename());
        Path travelDir = uploadRoot.resolve(travelId.toString()).normalize();
        Path target = travelDir.resolve(storedFilename).normalize();

        if (!target.startsWith(uploadRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid upload path");
        }

        try {
            Files.createDirectories(travelDir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Photo upload failed", exception);
        }

        Photo photo = new Photo(
                travel,
                safeFilename(file.getOriginalFilename()),
                file.getSize(),
                type,
                target.toString(),
                "",
                true,
                Instant.now());
        photo = photoRepository.save(photo);
        photo.updateImageUrl("/api/travels/" + travelId + "/photos/" + photo.getId() + "/file");

        return PhotoResponse.fromEntity(photoRepository.save(photo));
    }

    @Transactional(readOnly = true)
    public List<PhotoResponse> getByTravel(UUID travelId) {
        if (!travelRepository.existsById(travelId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found");
        }

        return photoRepository.findByTravelIdOrderByIdAsc(travelId)
                .stream()
                .map(PhotoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Resource getFile(UUID travelId, UUID photoId) {
        Photo photo = findPhotoForTravel(travelId, photoId);
        if (isBlank(photo.getStoragePath())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo file not found");
        }

        Path path = Path.of(photo.getStoragePath()).toAbsolutePath().normalize();

        if (!path.startsWith(uploadRoot) || !Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo file not found");
        }

        try {
            return new UrlResource(path.toUri());
        } catch (MalformedURLException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo file not found", exception);
        }
    }

    @Transactional(readOnly = true)
    public String getFileType(UUID travelId, UUID photoId) {
        return findPhotoForTravel(travelId, photoId).getType();
    }

    @Transactional
    public void delete(UUID travelId, UUID photoId) {
        Photo photo = findPhotoForTravel(travelId, photoId);

        try {
            if (!isBlank(photo.getStoragePath())) {
                Path path = Path.of(photo.getStoragePath()).toAbsolutePath().normalize();
                if (!path.startsWith(uploadRoot)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid photo path");
                }

                Files.deleteIfExists(path);
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Photo deletion failed", exception);
        }

        photoRepository.delete(photo);
    }

    private Travel findTravel(UUID travelId) {
        return travelRepository.findById(travelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found"));
    }

    private Photo findPhotoForTravel(UUID travelId, UUID photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));

        if (!photo.getTravel().getId().equals(travelId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found for this travel");
        }

        return photo;
    }

    private Photo toPhoto(Travel travel, PhotoMetadataRequest request) {
        if (request == null
                || isBlank(request.filename())
                || request.size() == null
                || request.size() <= 0
                || isBlank(request.type())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid photo metadata");
        }

        String type = normalizeType(request.type());
        if (!SUPPORTED_TYPES.contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported photo type");
        }

        return new Photo(
                travel,
                request.filename().trim(),
                request.size(),
                type,
                "metadata-only/" + UUID.randomUUID(),
                "",
                false,
                null);
    }

    private void validateConsent(boolean consentRgpd) {
        if (!consentRgpd) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consentement requis avant l'upload.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo file is required");
        }

        if (file.getSize() > maxFileSize) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Photo file is too large");
        }

        String type = normalizeType(file.getContentType());
        if (!SUPPORTED_TYPES.contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported photo type");
        }
    }

    private String normalizeType(String type) {
        if (type == null) {
            return "";
        }

        return type.trim().toLowerCase(Locale.ROOT);
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "photo.jpg";
        }

        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
