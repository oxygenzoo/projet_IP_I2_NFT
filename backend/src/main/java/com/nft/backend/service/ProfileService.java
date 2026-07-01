package com.nft.backend.service;

import java.util.Optional;
import java.util.UUID;

import com.nft.backend.dto.profile.CreateProfileRequest;
import com.nft.backend.dto.profile.ProfileResponse;
import com.nft.backend.model.Profile;
import com.nft.backend.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional
    public ProfileResponse createProfile(CreateProfileRequest request) {
        String email = normalizeEmail(request == null ? null : request.email());
        if (email == null) {
            throw new IllegalArgumentException("Email is required");
        }

        if (profileRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        Profile profile = new Profile(
                email,
                trimToNull(request.fullName()),
                trimToNull(request.avatarUrl()));

        return ProfileResponse.fromEntity(profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public Optional<ProfileResponse> getProfile(UUID id) {
        return profileRepository.findById(id)
                .map(ProfileResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Optional<ProfileResponse> getProfileByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            throw new IllegalArgumentException("Email is required");
        }

        return profileRepository.findByEmail(normalizedEmail)
                .map(ProfileResponse::fromEntity);
    }

    private String normalizeEmail(String email) {
        String normalizedEmail = trimToNull(email);
        return normalizedEmail == null ? null : normalizedEmail.toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
