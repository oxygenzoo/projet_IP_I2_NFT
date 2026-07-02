package com.nft.backend.controller;

import java.net.URI;
import java.util.UUID;

import com.nft.backend.dto.creation.CreationGenerationRequest;
import com.nft.backend.dto.creation.CreationSessionRequest;
import com.nft.backend.dto.creation.CreationSessionResponse;
import com.nft.backend.service.CreationSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/creations")
public class CreationSessionController {

    private final CreationSessionService creationSessionService;

    public CreationSessionController(CreationSessionService creationSessionService) {
        this.creationSessionService = creationSessionService;
    }

    @GetMapping("/current")
    public CreationSessionResponse current() {
        return creationSessionService.current();
    }

    @PostMapping
    public ResponseEntity<CreationSessionResponse> start(@RequestBody CreationSessionRequest request) {
        CreationSessionResponse session = creationSessionService.start(request);
        return ResponseEntity.created(URI.create("/api/creations/" + session.id())).body(session);
    }

    @PatchMapping("/{id}")
    public CreationSessionResponse update(@PathVariable UUID id, @RequestBody CreationSessionRequest request) {
        return creationSessionService.update(id, request);
    }

    @PostMapping("/{id}/generate")
    public CreationSessionResponse generate(@PathVariable UUID id, @RequestBody CreationGenerationRequest request) {
        return creationSessionService.launchGeneration(id, request.travelId(), request.preferences());
    }
}
