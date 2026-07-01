package com.nft.backend.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.nft.backend.dto.generation.GenerationResponse;
import com.nft.backend.dto.travel.EpisodeDto;
import com.nft.backend.dto.travel.SceneDto;
import com.nft.backend.dto.travel.TravelDto;
import org.springframework.stereotype.Service;

@Service
public class TravelCatalogService {

    private static final DateTimeFormatter FRENCH_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
    private static final String GENERATED_IMAGE = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1200 800'%3E%3Cdefs%3E%3ClinearGradient id='g' x1='0' x2='1' y1='0' y2='1'%3E%3Cstop stop-color='%23242b68'/%3E%3Cstop offset='0.55' stop-color='%23842f68'/%3E%3Cstop offset='1' stop-color='%23ffc533'/%3E%3C/linearGradient%3E%3C/defs%3E%3Crect width='1200' height='800' fill='url(%23g)'/%3E%3Ccircle cx='930' cy='150' r='190' fill='rgba(255,255,255,0.16)'/%3E%3Cpath d='M0 610 210 470 390 560 620 370 820 515 1200 295v505H0z' fill='rgba(5,5,5,0.32)'/%3E%3C/svg%3E";

    private final List<TravelDto> travels = new CopyOnWriteArrayList<>();
    private final List<EpisodeDto> episodes = new CopyOnWriteArrayList<>();

    public List<TravelDto> getTravels() {
        return List.copyOf(travels);
    }

    public Optional<TravelDto> getTravel(String id) {
        return travels.stream().filter((travel) -> travel.id().equals(id)).findFirst();
    }

    public List<EpisodeDto> getEpisodes() {
        return List.copyOf(episodes);
    }

    public Optional<EpisodeDto> getEpisode(String id) {
        return episodes.stream().filter((episode) -> episode.id().equals(id)).findFirst();
    }

    public void registerGeneration(
            GenerationResponse generation,
            String fallbackTitle,
            String destination,
            int photoCount,
            List<String> filenames) {
        String jobId = cleanOrDefault(generation.job_id(), UUID.randomUUID().toString());
        String travelId = "travel-" + jobId;
        String episodeId = "episode-" + jobId;
        Map<String, Object> script = generation.script() == null ? Map.of() : generation.script();
        Map<String, Object> firstEpisode = firstEpisode(script);
        String title = cleanOrDefault(stringValue(script.get("voyage")), cleanOrDefault(fallbackTitle, "Mon voyage"));
        String episodeTitle = cleanOrDefault(stringValue(firstEpisode.get("episode_titre")), "Episode 1 - " + title);
        String location = cleanOrDefault(stringValue(firstEpisode.get("lieu")), cleanOrDefault(destination, "Votre voyage"));
        String date = cleanOrDefault(stringValue(firstEpisode.get("date")), FRENCH_DATE.format(LocalDate.now()));
        List<SceneDto> scenes = scenesFromFilenames(filenames);

        EpisodeDto episode = new EpisodeDto(
                episodeId,
                travelId,
                1,
                numberValue(firstEpisode.get("episode_numero"), 1),
                episodeTitle,
                location,
                cleanOrDefault(generation.message(), "Episode cree depuis vos photos."),
                "2 min",
                location,
                date,
                photoCount,
                GENERATED_IMAGE,
                GENERATED_IMAGE,
                100,
                "Pret a regarder",
                scenes);

        TravelDto travel = new TravelDto(
                travelId,
                title,
                location,
                location,
                LocalDate.now().getYear(),
                "Votre episode souvenir est pret.",
                cleanOrDefault(generation.message(), "Une serie personnelle creee depuis vos photos."),
                GENERATED_IMAGE,
                GENERATED_IMAGE,
                GENERATED_IMAGE,
                "2 min",
                1,
                photoCount,
                100,
                "Pret a regarder",
                true,
                List.of("Personnel", "Souvenir"),
                List.of(episode),
                null,
                null,
                null,
                null);

        travels.add(0, travel);
        episodes.add(0, episode);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstEpisode(Map<String, Object> script) {
        Object episodesValue = script.get("episodes");

        if (episodesValue instanceof List<?> episodeList && !episodeList.isEmpty()
                && episodeList.get(0) instanceof Map<?, ?> episode) {
            return (Map<String, Object>) episode;
        }

        return Map.of();
    }

    private List<SceneDto> scenesFromFilenames(List<String> filenames) {
        List<SceneDto> scenes = new ArrayList<>();
        List<String> safeFilenames = filenames == null ? List.of() : filenames;

        for (int index = 0; index < Math.min(safeFilenames.size(), 6); index++) {
            scenes.add(new SceneDto(
                    "scene-" + (index + 1),
                    "Souvenir " + (index + 1),
                    "00:" + String.format(Locale.ROOT, "%02d", index * 12),
                    safeFilenames.get(index),
                    GENERATED_IMAGE));
        }

        return scenes;
    }

    private String stringValue(Object value) {
        return value instanceof String text ? text : "";
    }

    private int numberValue(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private String cleanOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
