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

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(name = "storage_path")
    private String storagePath;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "consent_rgpd")
    private boolean consentRgpd;

    @Column(name = "consent_date")
    private Instant consentDate;

    @Column(nullable = false, updatable = false)
    private Instant uploadedAt = Instant.now();

    protected Photo() {
    }

    public Photo(
            Travel travel,
            String filename,
            long size,
            String type,
            String storagePath,
            String imageUrl,
            boolean consentRgpd,
            Instant consentDate) {
        this.travel = travel;
        this.filename = filename;
        this.size = size;
        this.type = type;
        this.storagePath = storagePath;
        this.imageUrl = imageUrl;
        this.consentRgpd = consentRgpd;
        this.consentDate = consentDate;
    }

    public UUID getId() {
        return id;
    }

    public Travel getTravel() {
        return travel;
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

    public String getStoragePath() {
        return storagePath;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isConsentRgpd() {
        return consentRgpd;
    }

    public Instant getConsentDate() {
        return consentDate;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
