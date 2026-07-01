package com.nft.backend.service;

import java.util.Optional;

import com.nft.backend.dto.profile.CreateProfileRequest;
import com.nft.backend.dto.profile.ProfileResponse;
import com.nft.backend.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ProfileServiceTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private ProfileRepository profileRepository;

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
    }

    @Test
    void createProfile_shouldCreateUser_whenEmailIsValid() {
        ProfileResponse profile = profileService.createProfile(new CreateProfileRequest(
                "test@example.com",
                "Test User",
                "https://example.com/avatar.png"));

        assertNotNull(profile.id());
        assertEquals("test@example.com", profile.email());
        assertEquals("Test User", profile.fullName());
        assertEquals("https://example.com/avatar.png", profile.avatarUrl());
        assertFalse(profile.consentRgpd());
        assertNotNull(profile.createdAt());
    }

    @Test
    void createProfile_shouldRejectEmptyEmail() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> profileService.createProfile(new CreateProfileRequest("   ", "Test User", null)));

        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    void getProfile_shouldReturnExistingUser() {
        ProfileResponse createdProfile = profileService.createProfile(new CreateProfileRequest(
                "reader@example.com",
                "Reader User",
                null));

        Optional<ProfileResponse> profile = profileService.getProfile(createdProfile.id());

        assertTrue(profile.isPresent());
        assertEquals(createdProfile.id(), profile.get().id());
        assertEquals("reader@example.com", profile.get().email());
        assertEquals("Reader User", profile.get().fullName());
    }
}
