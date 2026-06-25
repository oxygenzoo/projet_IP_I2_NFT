package com.nft.backend.service;

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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public PhotoService(PhotoRepository photoRepository, TravelRepository travelRepository) {
        this.photoRepository = photoRepository;
        this.travelRepository = travelRepository;
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

    @Transactional(readOnly = true)
    public List<PhotoResponse> getByTravel(UUID travelId) {
        if (!travelRepository.existsById(travelId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found");
        }

        return photoRepository.findByTravelIdOrderByUploadedAtAsc(travelId)
                .stream()
                .map(PhotoResponse::fromEntity)
                .toList();
    }

    private Travel findTravel(UUID travelId) {
        return travelRepository.findById(travelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found"));
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

        return new Photo(travel, request.filename().trim(), request.size(), type);
    }

    private String normalizeType(String type) {
        return type.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
