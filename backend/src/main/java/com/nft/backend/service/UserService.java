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
    private final UserIdentityService userIdentityService;

    public UserService(UserRepository userRepository, UserIdentityService userIdentityService) {
        this.userRepository = userRepository;
        this.userIdentityService = userIdentityService;
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
        return UserDto.fromEntity(userIdentityService.ensureCurrentUser());
    }

    @Transactional
    public UserDto updateLanguage(UpdateLanguageRequest request) {
        String language = request == null ? "fr" : cleanLanguage(request.language());
        return UserDto.fromEntity(userIdentityService.updateLanguage(language));
    }

    private String cleanLanguage(String language) {
        String clean = language == null || language.isBlank() ? "fr" : language.trim().toLowerCase();
        if (!List.of("fr", "en", "es", "pt").contains(clean)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported language");
        }
        return clean;
    }
}
