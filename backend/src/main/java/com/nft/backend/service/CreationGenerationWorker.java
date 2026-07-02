package com.nft.backend.service;

import java.util.UUID;

import com.nft.backend.dto.generation.GenerationResponse;
import com.nft.backend.model.CreationSession;
import com.nft.backend.repository.CreationSessionRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreationGenerationWorker {

    private final CreationSessionRepository creationSessionRepository;
    private final AiGenerationService aiGenerationService;

    public CreationGenerationWorker(
            CreationSessionRepository creationSessionRepository,
            AiGenerationService aiGenerationService) {
        this.creationSessionRepository = creationSessionRepository;
        this.aiGenerationService = aiGenerationService;
    }

    @Async
    @Transactional
    public void generate(UUID sessionId, UUID ownerId, UUID travelId, String preferences) {
        try {
            GenerationResponse response = aiGenerationService.generateEpisodeFromTravel(ownerId, travelId, preferences);
            updateSession(sessionId, "done", travelId, firstVideoUrl(response), null);
        } catch (Exception exception) {
            updateSession(sessionId, "error", travelId, null, cleanMessage(exception));
        }
    }

    private void updateSession(UUID sessionId, String status, UUID travelId, String resultVideoUrl, String errorMessage) {
        creationSessionRepository.findById(sessionId)
                .ifPresent((session) -> {
                    session.update(status, travelId, null, resultVideoUrl, errorMessage);
                    creationSessionRepository.save(session);
                });
    }

    private String firstVideoUrl(GenerationResponse response) {
        if (response == null || response.videos() == null || response.videos().isEmpty()) {
            return null;
        }
        return response.videos().getFirst();
    }

    private String cleanMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "La génération IA a échoué.";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
