package com.nft.backend.controller;

import java.util.UUID;

import com.nft.backend.dto.preference.PreferenceResponse;
import com.nft.backend.dto.preference.SavePreferenceRequest;
import com.nft.backend.service.PreferenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/travels/{travelId}/preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @PostMapping
    public PreferenceResponse savePreferences(
            @PathVariable UUID travelId,
            @Valid @RequestBody SavePreferenceRequest request) {
        return preferenceService.save(travelId, request);
    }

    @GetMapping
    public PreferenceResponse getPreferences(@PathVariable UUID travelId) {
        return preferenceService.getByTravel(travelId);
    }
}
