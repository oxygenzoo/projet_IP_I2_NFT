package com.nft.backend.controller;

import java.net.URI;
import java.util.UUID;

import com.nft.backend.dto.profile.CreateProfileRequest;
import com.nft.backend.dto.profile.ProfileResponse;
import com.nft.backend.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping
    public ResponseEntity<ProfileResponse> createProfile(@Valid @RequestBody CreateProfileRequest request) {
        try {
            ProfileResponse profile = profileService.createProfile(request);
            return ResponseEntity
                    .created(URI.create("/api/profiles/" + profile.id()))
                    .body(profile);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable UUID id) {
        return ResponseEntity.of(profileService.getProfile(id));
    }

    @GetMapping("/by-email")
    public ResponseEntity<ProfileResponse> getProfileByEmail(@RequestParam String email) {
        try {
            return ResponseEntity.of(profileService.getProfileByEmail(email));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
