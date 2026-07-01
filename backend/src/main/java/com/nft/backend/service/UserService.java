package com.nft.backend.service;

import java.util.UUID;

import com.nft.backend.dto.user.CreateUserRequest;
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

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}
