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
@Table(name = "contribution_links")
public class ContributionLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "travel_id", nullable = false)
    private Travel travel;

    @Column(nullable = false, unique = true, length = 96)
    private String token;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ContributionLink() {
    }

    public ContributionLink(Travel travel, String token, UUID ownerId, Instant expiresAt) {
        this.travel = travel;
        this.token = token;
        this.ownerId = ownerId;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public Travel getTravel() {
        return travel;
    }

    public String getToken() {
        return token;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
}
