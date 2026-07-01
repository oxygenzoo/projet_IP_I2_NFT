package com.nft.backend.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "episodes")
public class Episode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "travel_id", nullable = false)
    private Travel travel;

    @Column(name = "episode_number", nullable = false)
    private int episodeNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "location_name", length = 255)
    private String locationName;

    @Column(name = "episode_date")
    private LocalDate episodeDate;

    @Column(name = "intro_text", columnDefinition = "TEXT")
    private String introText;

    @Column(name = "outro_text", columnDefinition = "TEXT")
    private String outroText;

    @Column(name = "music_mood", length = 200)
    private String musicMood;

    @Transient
    private String shareToken;

    @Transient
    private String exportStatus = "idle";

    @Column(name = "video_path")
    private String videoUrl;

    @Convert(converter = EpisodeStatusConverter.class)
    @Column(name = "video_status", nullable = false, length = 20)
    private EpisodeStatus status = EpisodeStatus.DRAFT;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt = Instant.now();

    @OneToMany(mappedBy = "episode", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EpisodeScene> scenes = new ArrayList<>();

    protected Episode() {
    }

    public Episode(
            Travel travel,
            int episodeNumber,
            String title,
            String locationName,
            LocalDate episodeDate,
            String introText,
            String outroText,
            String musicMood,
            EpisodeStatus status) {
        this.travel = travel;
        this.episodeNumber = episodeNumber;
        this.title = title;
        this.locationName = locationName;
        this.episodeDate = episodeDate;
        this.introText = introText;
        this.outroText = outroText;
        this.musicMood = musicMood;
        this.status = status == null ? EpisodeStatus.DRAFT : status;
    }

    public UUID getId() {
        return id;
    }

    public Travel getTravel() {
        return travel;
    }

    public int getEpisodeNumber() {
        return episodeNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getLocationName() {
        return locationName;
    }

    public LocalDate getEpisodeDate() {
        return episodeDate;
    }

    public String getIntroText() {
        return introText;
    }

    public String getOutroText() {
        return outroText;
    }

    public String getMusicMood() {
        return musicMood;
    }

    public String getShareToken() {
        return shareToken;
    }

    public String getExportStatus() {
        return exportStatus;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public EpisodeStatus getStatus() {
        return status;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public List<EpisodeScene> getScenes() {
        return scenes;
    }

    public void update(
            int episodeNumber,
            String title,
            String locationName,
            LocalDate episodeDate,
            String introText,
            String outroText,
            String musicMood,
            EpisodeStatus status) {
        this.episodeNumber = episodeNumber;
        this.title = title;
        this.locationName = locationName;
        this.episodeDate = episodeDate;
        this.introText = introText;
        this.outroText = outroText;
        this.musicMood = musicMood;
        this.status = status == null ? EpisodeStatus.DRAFT : status;
    }

    public void changeStatus(EpisodeStatus status) {
        this.status = status;
    }

    public void enableSharing(String shareToken) {
        this.shareToken = shareToken;
    }

    public void updateExport(String exportStatus, String videoUrl) {
        this.exportStatus = exportStatus;
        this.videoUrl = videoUrl;
    }
}
