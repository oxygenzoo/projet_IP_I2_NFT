package com.nft.backend.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    private UUID id;

    @Column(length = 255)
    private String email;

    @Column(name = "full_name", columnDefinition = "TEXT")
    private String fullName;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "last_login")
    private Instant lastLogin;

    @Column(name = "consent_rgpd")
    private Boolean consentRgpd;

    @Column(name = "consent_date")
    private Instant consentDate;

    @Column(nullable = false, length = 8)
    private String language = "fr";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Profile() {
    }

    public Profile(UUID id, String email, String language) {
        this.id = id;
        this.email = email;
        this.language = cleanLanguage(language);
        this.consentRgpd = true;
        this.consentDate = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getLanguage() {
        return language;
    }

    public void syncEmail(String email) {
        if (email != null && !email.isBlank() && !email.equals(this.email)) {
            this.email = email;
            touch();
        }
    }

    public void updateLanguage(String language) {
        String clean = cleanLanguage(language);
        if (!clean.equals(this.language)) {
            this.language = clean;
            touch();
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private String cleanLanguage(String language) {
        return language == null || language.isBlank() ? "fr" : language.trim().toLowerCase();
    }
}
