package com.nft.backend.service;

import java.util.List;
import java.util.UUID;

import com.nft.backend.dto.user.CreateUserRequest;
import com.nft.backend.dto.user.UpdateLanguageRequest;
import com.nft.backend.dto.user.UserDto;
import com.nft.backend.model.User;
import com.nft.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public UserService(UserRepository userRepository, AuthenticatedUserService authenticatedUserService) {
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Transactional
    public UserDto create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = new User(email, request.passwordHash().trim(), request.consentRgpd());
        return UserDto.fromEntity(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserDto get(UUID id) {
        return userRepository.findById(id)
                .map(UserDto::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional
    public UserDto me() {
        AuthenticatedUserService.AuthenticatedUser current = authenticatedUserService.currentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
        return userRepository.findById(current.id())
                .map(UserDto::fromEntity)
                .orElseGet(() -> UserDto.fromEntity(userRepository.save(new User(
                        current.id(),
                        current.email().isBlank() ? current.id() + "@external.local" : current.email(),
                        "external",
                        true))));
    }

    @Transactional
    public UserDto updateLanguage(UpdateLanguageRequest request) {
        AuthenticatedUserService.AuthenticatedUser current = authenticatedUserService.currentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
        String language = request == null ? "fr" : cleanLanguage(request.language());
        User user = userRepository.findById(current.id())
                .orElseGet(() -> userRepository.save(new User(
                        current.id(),
                        current.email().isBlank() ? current.id() + "@external.local" : current.email(),
                        "external",
                        true)));
        user.updateLanguage(language);
        return UserDto.fromEntity(userRepository.save(user));
    }

    private String cleanLanguage(String language) {
        String clean = language == null || language.isBlank() ? "fr" : language.trim().toLowerCase();
        if (!List.of("fr", "en", "es", "pt").contains(clean)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported language");
        }
        return clean;
    }
}
