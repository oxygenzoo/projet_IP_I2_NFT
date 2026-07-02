package com.nft.backend.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "creation_sessions")
public class CreationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "travel_id")
    private UUID travelId;

    @Column(name = "episode_id")
    private UUID episodeId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "result_video_url")
    private String resultVideoUrl;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected CreationSession() {
    }

    public CreationSession(UUID ownerId, UUID travelId, String status) {
        this.ownerId = ownerId;
        this.travelId = travelId;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getTravelId() {
        return travelId;
    }

    public UUID getEpisodeId() {
        return episodeId;
    }

    public String getStatus() {
        return status;
    }

    public String getResultVideoUrl() {
        return resultVideoUrl;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(String status, UUID travelId, UUID episodeId, String resultVideoUrl, String errorMessage) {
        if (status != null && !status.isBlank()) {
            this.status = status;
        }
        if (travelId != null) {
            this.travelId = travelId;
        }
        if (episodeId != null) {
            this.episodeId = episodeId;
        }
        if (resultVideoUrl != null) {
            this.resultVideoUrl = resultVideoUrl;
        }
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }
}
