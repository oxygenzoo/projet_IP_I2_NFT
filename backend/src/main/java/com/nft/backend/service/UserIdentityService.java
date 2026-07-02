package com.nft.backend.service;

import com.nft.backend.model.Profile;
import com.nft.backend.model.User;
import com.nft.backend.repository.ProfileRepository;
import com.nft.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserIdentityService {

    private static final String EXTERNAL_PASSWORD_HASH = "external";

    private final AuthenticatedUserService authenticatedUserService;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public UserIdentityService(
            AuthenticatedUserService authenticatedUserService,
            UserRepository userRepository,
            ProfileRepository profileRepository) {
        this.authenticatedUserService = authenticatedUserService;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public User ensureCurrentUser() {
        return ensureIdentity(authenticatedUserService.requireCurrentUser());
    }

    @Transactional
    public User updateLanguage(String language) {
        User user = ensureCurrentUser();
        user.updateLanguage(language);
        profileRepository.findById(user.getId()).ifPresent((profile) -> profile.updateLanguage(language));
        return userRepository.save(user);
    }

    private User ensureIdentity(AuthenticatedUserService.AuthenticatedUser current) {
        String email = cleanEmail(current);
        User user = userRepository.findById(current.id())
                .orElseGet(() -> userRepository.save(new User(
                        current.id(),
                        uniqueUserEmail(current, email),
                        EXTERNAL_PASSWORD_HASH,
                        true)));

        Profile profile = profileRepository.findById(current.id())
                .orElseGet(() -> profileRepository.save(new Profile(current.id(), email, user.getLanguage())));
        profile.syncEmail(email);
        profile.updateLanguage(user.getLanguage());
        return user;
    }

    private String uniqueUserEmail(AuthenticatedUserService.AuthenticatedUser current, String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .filter((existing) -> !existing.getId().equals(current.id()))
                .map((ignored) -> syntheticEmail(current))
                .orElse(email);
    }

    private String cleanEmail(AuthenticatedUserService.AuthenticatedUser current) {
        return current.email() == null || current.email().isBlank()
                ? syntheticEmail(current)
                : current.email().trim().toLowerCase();
    }

    private String syntheticEmail(AuthenticatedUserService.AuthenticatedUser current) {
        return current.id() + "@external.local";
    }
}
