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
@Table(name = "photos")
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "travel_id", nullable = false)
    private Travel travel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private Profile profile;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(nullable = false, updatable = false)
    private Instant uploadedAt = Instant.now();

    protected Photo() {
    }

    public Photo(Travel travel, String filename, long size, String type) {
        this.travel = travel;
        this.filename = filename;
        this.size = size;
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public Travel getTravel() {
        return travel;
    }

    public Profile getProfile() {
        return profile;
    }

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }

    public String getType() {
        return type;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
