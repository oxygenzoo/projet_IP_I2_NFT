package com.nft.backend.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "travels")
public class Travel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String destination;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Profile user;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "travel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Episode> episodes = new ArrayList<>();

    @OneToMany(mappedBy = "travel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Photo> photos = new ArrayList<>();

    protected Travel() {
    }

    public Travel(String title, String destination, String description) {
        this(null, title, destination, description, null, null);
    }

    public Travel(
            Profile user,
            String title,
            String destination,
            String description,
            LocalDate startDate,
            LocalDate endDate) {
        this.user = user;
        this.title = title;
        this.destination = destination;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDestination() {
        return destination;
    }

    public String getDescription() {
        return description;
    }

    public Profile getUser() {
        return user;
    }

    public Profile getProfile() {
        return user;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public void setUser(Profile user) {
        this.user = user;
    }

    public void update(
            String title,
            String destination,
            String description,
            LocalDate startDate,
            LocalDate endDate) {
        this.title = title;
        this.destination = destination;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
