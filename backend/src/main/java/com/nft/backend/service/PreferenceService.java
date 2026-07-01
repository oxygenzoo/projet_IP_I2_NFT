package com.nft.backend.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.nft.backend.dto.preference.PreferenceResponse;
import com.nft.backend.dto.preference.SavePreferenceRequest;
import com.nft.backend.repository.TravelRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PreferenceService {

    private final Map<String, PreferenceResponse> preferencesByTravelId = new ConcurrentHashMap<>();
    private final TravelCatalogService travelCatalogService;
    private final TravelRepository travelRepository;

    public PreferenceService(TravelCatalogService travelCatalogService, TravelRepository travelRepository) {
        this.travelCatalogService = travelCatalogService;
        this.travelRepository = travelRepository;
    }

    public PreferenceResponse save(String travelId, SavePreferenceRequest request) {
        ensureTravelExists(travelId);

        PreferenceResponse current = preferencesByTravelId.get(travelId);
        Instant now = Instant.now();
        PreferenceResponse saved = new PreferenceResponse(
                current == null ? travelId + "-preferences" : current.id(),
                travelId,
                request.style(),
                request.people(),
                request.moments(),
                request.tone(),
                current == null ? now : current.createdAt(),
                now);

        preferencesByTravelId.put(travelId, saved);
        return saved;
    }

    public PreferenceResponse getByTravel(String travelId) {
        ensureTravelExists(travelId);

        return java.util.Optional.ofNullable(preferencesByTravelId.get(travelId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Preferences not found"));
    }

    private void ensureTravelExists(String travelId) {
        if (travelCatalogService.getTravel(travelId).isPresent()) {
            return;
        }

        try {
            if (travelRepository.existsById(java.util.UUID.fromString(travelId))) {
                return;
            }
        } catch (IllegalArgumentException exception) {
            // Keep generated catalog ids supported without making UUID parsing a controller concern.
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found");
    }
}
