package com.nft.backend.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "preferences",
        indexes = {
                @Index(name = "idx_preferences_travel", columnList = "travel_id")
        })
public class Preference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "travel_id", nullable = false, unique = true)
    private Travel travel;

    @Column(nullable = false, length = 80)
    private String style;

    @Column(nullable = false, length = 80)
    private String people;

    @Column(nullable = false, length = 80)
    private String moments;

    @Column(nullable = false, length = 80)
    private String tone;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected Preference() {
    }

    public Preference(Travel travel, String style, String people, String moments, String tone) {
        this.travel = travel;
        update(style, people, moments, tone);
    }

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Travel getTravel() {
        return travel;
    }

    public String getStyle() {
        return style;
    }

    public String getPeople() {
        return people;
    }

    public String getMoments() {
        return moments;
    }

    public String getTone() {
        return tone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(String style, String people, String moments, String tone) {
        this.style = style;
        this.people = people;
        this.moments = moments;
        this.tone = tone;
        touch();
    }
}
