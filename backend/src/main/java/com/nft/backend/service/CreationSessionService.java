package com.nft.backend.service;

import java.util.List;
import java.util.UUID;

import com.nft.backend.dto.creation.CreationSessionRequest;
import com.nft.backend.dto.creation.CreationSessionResponse;
import com.nft.backend.model.CreationSession;
import com.nft.backend.model.Travel;
import com.nft.backend.repository.CreationSessionRepository;
import com.nft.backend.repository.PhotoRepository;
import com.nft.backend.repository.TravelRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreationSessionService {

    private static final List<String> ACTIVE_STATUSES = List.of("uploading", "preferences", "generating", "done", "error");

    private final CreationSessionRepository creationSessionRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final TravelRepository travelRepository;
    private final PhotoRepository photoRepository;
    private final CreationGenerationWorker generationWorker;

    public CreationSessionService(
            CreationSessionRepository creationSessionRepository,
            AuthenticatedUserService authenticatedUserService,
            TravelRepository travelRepository,
            PhotoRepository photoRepository,
            CreationGenerationWorker generationWorker) {
        this.creationSessionRepository = creationSessionRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.travelRepository = travelRepository;
        this.photoRepository = photoRepository;
        this.generationWorker = generationWorker;
    }

    @Transactional(readOnly = true)
    public CreationSessionResponse current() {
        UUID ownerId = authenticatedUserService.requireCurrentUserId();
        return creationSessionRepository.findTopByOwnerIdAndStatusInOrderByUpdatedAtDesc(ownerId, ACTIVE_STATUSES)
                .map(CreationSessionResponse::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active creation"));
    }

    @Transactional
    public CreationSessionResponse start(CreationSessionRequest request) {
        UUID ownerId = authenticatedUserService.requireCurrentUserId();
        CreationSession session = new CreationSession(ownerId, request.travelId(), cleanStatus(request.status(), "uploading"));
        return CreationSessionResponse.fromEntity(creationSessionRepository.save(session));
    }

    @Transactional
    public CreationSessionResponse update(UUID id, CreationSessionRequest request) {
        UUID ownerId = authenticatedUserService.requireCurrentUserId();
        CreationSession session = creationSessionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Creation not found"));
        if (!ownerId.equals(session.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Creation access denied");
        }
        session.update(cleanStatus(request.status(), null), request.travelId(), request.episodeId(), request.resultVideoUrl(), request.errorMessage());
        return CreationSessionResponse.fromEntity(creationSessionRepository.save(session));
    }

    @Transactional
    public CreationSessionResponse launchGeneration(UUID id, UUID travelId, String preferences) {
        UUID ownerId = authenticatedUserService.requireCurrentUserId();
        CreationSession session = creationSessionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Creation not found"));
        if (!ownerId.equals(session.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Creation access denied");
        }

        UUID resolvedTravelId = travelId == null ? session.getTravelId() : travelId;
        if (resolvedTravelId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Travel is required before generation");
        }

        Travel travel = travelRepository.findById(resolvedTravelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found"));
        if (travel.getUser() == null || !ownerId.equals(travel.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Travel access denied");
        }
        if (photoRepository.findByTravelIdOrderByIdAsc(resolvedTravelId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ajoutez au moins une photo avant la génération.");
        }

        session.update("generating", resolvedTravelId, null, null, null);
        CreationSession saved = creationSessionRepository.save(session);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                generationWorker.generate(saved.getId(), ownerId, resolvedTravelId, preferences);
            }
        });
        return CreationSessionResponse.fromEntity(saved);
    }

    private String cleanStatus(String status, String fallback) {
        if (status == null || status.isBlank()) {
            return fallback;
        }
        String clean = status.trim().toLowerCase();
        if (!ACTIVE_STATUSES.contains(clean)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid creation status");
        }
        return clean;
    }
}
