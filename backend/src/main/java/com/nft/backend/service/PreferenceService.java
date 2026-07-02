package com.nft.backend.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.nft.backend.dto.preference.PreferenceResponse;
import com.nft.backend.dto.preference.SavePreferenceRequest;
import com.nft.backend.model.Travel;
import com.nft.backend.repository.TravelRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PreferenceService {

    private final Map<String, PreferenceResponse> preferencesByTravelId = new ConcurrentHashMap<>();
    private final TravelCatalogService travelCatalogService;
    private final TravelRepository travelRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public PreferenceService(
            TravelCatalogService travelCatalogService,
            TravelRepository travelRepository,
            AuthenticatedUserService authenticatedUserService) {
        this.travelCatalogService = travelCatalogService;
        this.travelRepository = travelRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    public PreferenceResponse save(String travelId, SavePreferenceRequest request) {
        ensureTravelAccess(travelId);

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
        ensureTravelAccess(travelId);

        return java.util.Optional.ofNullable(preferencesByTravelId.get(travelId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Preferences not found"));
    }

    private void ensureTravelAccess(String travelId) {
        AuthenticatedUserService.AuthenticatedUser user = authenticatedUserService.requireCurrentUser();
        if (travelCatalogService.getTravel(travelId).isPresent()) {
            return;
        }

        try {
            Travel travel = travelRepository.findById(java.util.UUID.fromString(travelId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found"));
            if (travel.getUser() != null && user.id().equals(travel.getUser().getId())) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Travel access denied");
        } catch (IllegalArgumentException exception) {
            // Keep generated catalog ids supported without making UUID parsing a controller concern.
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found");
    }
}
