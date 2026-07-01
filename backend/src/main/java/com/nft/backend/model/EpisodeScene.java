package com.nft.backend.model;

import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "episode_scenes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_episode_scene_order",
                columnNames = {"episode_id", "scene_order"}))
public class EpisodeScene {

    private static final Set<String> TYPES = Set.of("intro", "souvenir", "transition", "conclusion");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @Column(name = "scene_order", nullable = false)
    private int order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_id")
    private Photo photo;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "voice_over_text", nullable = false, columnDefinition = "TEXT")
    private String voiceOverText;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(name = "generation_status", nullable = false, length = 64)
    private String generationStatus;

    @Column(name = "is_ai_reconstructed", nullable = false)
    private boolean isAiReconstructed;

    @Column(name = "ai_prompt", columnDefinition = "TEXT")
    private String aiPrompt;

    protected EpisodeScene() {
    }

    public EpisodeScene(
            Episode episode,
            int order,
            Photo photo,
            String photoUrl,
            String voiceOverText,
            String type,
            String generationStatus,
            boolean isAiReconstructed,
            String aiPrompt) {
        this.episode = episode;
        this.order = order;
        this.photo = photo;
        this.photoUrl = photoUrl;
        this.voiceOverText = voiceOverText;
        this.type = normalizeType(type);
        this.generationStatus = generationStatus == null || generationStatus.isBlank() ? "generated" : generationStatus;
        this.isAiReconstructed = isAiReconstructed;
        this.aiPrompt = aiPrompt;
    }

    public UUID getId() {
        return id;
    }

    public Episode getEpisode() {
        return episode;
    }

    public int getOrder() {
        return order;
    }

    public Photo getPhoto() {
        return photo;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getVoiceOverText() {
        return voiceOverText;
    }

    public String getType() {
        return type;
    }

    public String getGenerationStatus() {
        return generationStatus;
    }

    public boolean isAiReconstructed() {
        return isAiReconstructed;
    }

    public String getAiPrompt() {
        return aiPrompt;
    }

    private String normalizeType(String value) {
        String typeValue = value == null ? "" : value.trim().toLowerCase();
        if (!TYPES.contains(typeValue)) {
            throw new IllegalArgumentException("Invalid scene type");
        }
        return typeValue;
    }
}
