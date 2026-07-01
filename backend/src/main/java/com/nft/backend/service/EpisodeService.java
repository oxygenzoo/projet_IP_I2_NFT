package com.nft.backend.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import com.nft.backend.dto.episode.CreateEpisodeRequest;
import com.nft.backend.dto.episode.EpisodeResponse;
import com.nft.backend.dto.episode.UpdateEpisodeRequest;
import com.nft.backend.model.Episode;
import com.nft.backend.model.EpisodeStatus;
import com.nft.backend.model.Travel;
import com.nft.backend.repository.EpisodeRepository;
import com.nft.backend.repository.TravelRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EpisodeService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EpisodeRepository episodeRepository;
    private final TravelRepository travelRepository;

    public EpisodeService(EpisodeRepository episodeRepository, TravelRepository travelRepository) {
        this.episodeRepository = episodeRepository;
        this.travelRepository = travelRepository;
    }

    @Transactional
    public EpisodeResponse create(UUID travelId, CreateEpisodeRequest request) {
        Travel travel = findTravel(travelId);
        Episode episode = new Episode(
                travel,
                request.episodeNumber(),
                request.title(),
                request.locationName(),
                request.episodeDate(),
                request.introText(),
                request.outroText(),
                request.musicMood(),
                request.status());

        return EpisodeResponse.fromEntity(episodeRepository.save(episode));
    }

    @Transactional(readOnly = true)
    public List<EpisodeResponse> getByTravel(UUID travelId) {
        if (!travelRepository.existsById(travelId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found");
        }

        return episodeRepository.findByTravelIdOrderByEpisodeNumberAsc(travelId)
                .stream()
                .map(EpisodeResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public EpisodeResponse get(UUID travelId, UUID episodeId) {
        return EpisodeResponse.fromEntity(findEpisodeForTravel(travelId, episodeId));
    }

    @Transactional
    public EpisodeResponse update(UUID travelId, UUID episodeId, UpdateEpisodeRequest request) {
        Episode episode = findEpisodeForTravel(travelId, episodeId);
        episode.update(
                request.episodeNumber(),
                request.title(),
                request.locationName(),
                request.episodeDate(),
                request.introText(),
                request.outroText(),
                request.musicMood(),
                request.status());

        return EpisodeResponse.fromEntity(episodeRepository.save(episode));
    }

    @Transactional
    public EpisodeResponse changeStatus(UUID travelId, UUID episodeId, EpisodeStatus status) {
        Episode episode = findEpisodeForTravel(travelId, episodeId);
        episode.changeStatus(status);
        return EpisodeResponse.fromEntity(episodeRepository.save(episode));
    }

    @Transactional
    public void delete(UUID travelId, UUID episodeId) {
        Episode episode = findEpisodeForTravel(travelId, episodeId);
        episodeRepository.delete(episode);
    }

    @Transactional
    public EpisodeResponse enableSharing(UUID travelId, UUID episodeId) {
        Episode episode = findEpisodeForTravel(travelId, episodeId);

        if (episode.getShareToken() == null || episode.getShareToken().isBlank()) {
            episode.enableSharing(newShareToken());
        }

        return EpisodeResponse.fromEntity(episodeRepository.save(episode));
    }

    @Transactional(readOnly = true)
    public EpisodeResponse getPublicEpisode(String shareToken) {
        if (shareToken == null || shareToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Shared episode not found");
        }

        return episodeRepository.findByShareToken(shareToken)
                .map(EpisodeResponse::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shared episode not found"));
    }

    @Transactional
    public EpisodeResponse export(UUID travelId, UUID episodeId, boolean fail) {
        Episode episode = findEpisodeForTravel(travelId, episodeId);
        String videoUrl = episode.getVideoUrl();
        episode.updateExport("pending", videoUrl);

        if (fail) {
            episode.updateExport("failed", null);
        } else if (videoUrl == null || videoUrl.isBlank()) {
            episode.updateExport("failed", null);
        } else {
            episode.updateExport("ready", videoUrl);
        }

        return EpisodeResponse.fromEntity(episodeRepository.save(episode));
    }

    private Travel findTravel(UUID travelId) {
        return travelRepository.findById(travelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found"));
    }

    private Episode findEpisodeForTravel(UUID travelId, UUID episodeId) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Episode not found"));

        if (!episode.getTravel().getId().equals(travelId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Episode not found for this travel");
        }

        return episode;
    }

    private String newShareToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
