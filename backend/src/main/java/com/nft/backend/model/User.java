package com.nft.backend.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    private String passwordHash;

    @Column(name = "last_login")
    private Instant lastLogin;

    @Column(name = "consent_rgpd")
    private Boolean consentRgpd;

    @Column(name = "consent_date")
    private Instant consentDate;

    @Column(nullable = false, length = 8)
    private String language = "fr";

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Travel> travels = new ArrayList<>();

    protected User() {
    }

    public User(String email, String passwordHash, Boolean consentRgpd) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.consentRgpd = consentRgpd;
        this.consentDate = Boolean.TRUE.equals(consentRgpd) ? Instant.now() : null;
    }

    public User(UUID id, String email, String passwordHash, Boolean consentRgpd) {
        this(email, passwordHash, consentRgpd);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getLastLogin() {
        return lastLogin;
    }

    public Boolean getConsentRgpd() {
        return consentRgpd;
    }

    public Instant getConsentDate() {
        return consentDate;
    }

    public String getLanguage() {
        return language;
    }

    public List<Travel> getTravels() {
        return travels;
    }

    public void updateLanguage(String language) {
        if (language != null && !language.isBlank()) {
            this.language = language.trim().toLowerCase();
        }
    }
}
