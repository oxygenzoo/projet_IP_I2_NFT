package com.nft.backend.service;

import java.util.UUID;

import com.nft.backend.dto.preference.PreferenceResponse;
import com.nft.backend.dto.preference.SavePreferenceRequest;
import com.nft.backend.model.Preference;
import com.nft.backend.model.Travel;
import com.nft.backend.repository.PreferenceRepository;
import com.nft.backend.repository.TravelRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PreferenceService {

    private final PreferenceRepository preferenceRepository;
    private final TravelRepository travelRepository;

    public PreferenceService(PreferenceRepository preferenceRepository, TravelRepository travelRepository) {
        this.preferenceRepository = preferenceRepository;
        this.travelRepository = travelRepository;
    }

    @Transactional
    public PreferenceResponse save(UUID travelId, SavePreferenceRequest request) {
        Travel travel = findTravel(travelId);
        Preference preference = preferenceRepository.findByTravelId(travelId)
                .map((existingPreference) -> {
                    existingPreference.update(request.style(), request.people(), request.moments(), request.tone());
                    return existingPreference;
                })
                .orElseGet(() -> new Preference(
                        travel,
                        request.style(),
                        request.people(),
                        request.moments(),
                        request.tone()));

        return PreferenceResponse.fromEntity(preferenceRepository.save(preference));
    }

    @Transactional(readOnly = true)
    public PreferenceResponse getByTravel(UUID travelId) {
        if (!travelRepository.existsById(travelId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found");
        }

        return preferenceRepository.findByTravelId(travelId)
                .map(PreferenceResponse::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Preferences not found"));
    }

    private Travel findTravel(UUID travelId) {
        return travelRepository.findById(travelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found"));
    }
}
