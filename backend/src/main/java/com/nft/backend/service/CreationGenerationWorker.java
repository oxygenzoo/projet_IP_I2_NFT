package com.nft.backend.service;

import java.util.UUID;

import com.nft.backend.dto.generation.GenerationResponse;
import com.nft.backend.model.CreationSession;
import com.nft.backend.repository.CreationSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreationGenerationWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreationGenerationWorker.class);

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
            LOGGER.info("Generation worker started sessionId={} userId={} travelId={}", sessionId, ownerId, travelId);
            GenerationResponse response = aiGenerationService.generateEpisodeFromTravel(ownerId, travelId, preferences);
            updateSession(sessionId, "done", travelId, firstVideoUrl(response), null);
            LOGGER.info("Generation worker completed sessionId={} userId={} travelId={}", sessionId, ownerId, travelId);
        } catch (Exception exception) {
            LOGGER.warn("Generation worker failed sessionId={} userId={} travelId={}: {}", sessionId, ownerId, travelId, exception.getMessage());
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
