package com.nft.backend.model;

import java.time.Instant;
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

@Entity
@Table(name = "episode_collaborators")
public class EpisodeCollaborator {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @Column(name = "user_id")
    private UUID userId;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 32)
    private String role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected EpisodeCollaborator() {
    }

    public EpisodeCollaborator(Episode episode, UUID userId, String email, String role) {
        this.episode = episode;
        this.userId = userId;
        this.email = email == null ? null : email.trim().toLowerCase();
        this.role = role == null || role.isBlank() ? "viewer" : role.trim().toLowerCase();
    }

    public UUID getId() {
        return id;
    }

    public Episode getEpisode() {
        return episode;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
