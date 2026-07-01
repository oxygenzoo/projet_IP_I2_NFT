package com.nft.backend.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.nft.backend.dto.travel.EpisodeDto;
import com.nft.backend.dto.travel.SceneDto;
import com.nft.backend.dto.travel.TravelDto;
import org.springframework.stereotype.Service;

@Service
public class TravelCatalogService {

    private static final String BALI_HERO =
            "https://images.unsplash.com/photo-1537996194471-e657df975ab4?auto=format&fit=crop&w=1800&q=85";
    private static final String BALI_BEACH =
            "https://images.unsplash.com/photo-1518548419970-58e3b4079ab2?auto=format&fit=crop&w=1200&q=85";
    private static final String BALI_RICE =
            "https://images.unsplash.com/photo-1555400038-63f5ba517a47?auto=format&fit=crop&w=1200&q=85";
    private static final String BALI_TEMPLE =
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=85";

    private static final List<EpisodeDto> EPISODES = List.of(
            episode(
                    "1",
                    1,
                    "Arrivee et premieres lumieres",
                    "L'arrivee, puis la decouverte",
                    "Un episode inspirant a Bali, de Denpasar aux premieres rizieres d'Ubud.",
                    "18 min",
                    "Denpasar, Ubud",
                    "12 mai 2025",
                    127,
                    BALI_HERO,
                    BALI_BEACH,
                    62,
                    "7 min restantes",
                    List.of("arrivee a Denpasar", "route vers Ubud", "premiere riziere"),
                    List.of(
                            scene("scene-1", 1, "Arrivee", "00:00", "La chaleur de Denpasar ouvre le voyage.", "intro", BALI_HERO),
                            scene("scene-2", 2, "Route vers Ubud", "03:20", "Les scooters et les offrandes donnent le rythme.", "souvenir", BALI_BEACH),
                            scene("scene-3", 3, "Premiere riziere", "07:10", "Les lignes vertes d'Ubud deviennent le premier grand plan.", "souvenir", BALI_RICE),
                            scene("scene-4", 4, "Marche du soir", "11:40", "Les voix et les sourires replacent les photos dans le recit.", "transition", BALI_TEMPLE),
                            scene("scene-5", 5, "Promesse de saison", "15:05", "Le coucher du soleil ferme l'episode comme une invitation.", "conclusion", BALI_BEACH))),
            episode(
                    "2",
                    2,
                    "Immersion et souvenir fort",
                    "Temples, rizieres et image reconstruite",
                    "Un episode emotionnel a Bali qui rassemble les temples, les marches et un souvenir manquant reconstruit.",
                    "21 min",
                    "Ubud, Canggu",
                    "14 mai 2025",
                    156,
                    BALI_RICE,
                    BALI_RICE,
                    0,
                    "21 min restantes",
                    List.of("temple d'Ubud", "diner partage", "souvenir reconstruit"),
                    List.of(
                            scene("scene-6", 1, "Matin calme", "00:00", "Ubud se reveille lentement autour du temple.", "intro", BALI_TEMPLE),
                            scene("scene-7", 2, "Immersion", "04:15", "Les portraits et les gestes quotidiens prennent le dessus.", "souvenir", BALI_RICE),
                            aiScene(
                                    "scene-8",
                                    3,
                                    "Photo manquante",
                                    "08:35",
                                    "L'IA signale clairement une reconstitution du souvenir absent.",
                                    "transition",
                                    "Reconstituer une ruelle d'Ubud au crepuscule, style carnet de voyage, sans personne identifiable."),
                            scene("scene-9", 4, "Diner partage", "13:20", "Le repas devient le moment fort de l'episode.", "souvenir", BALI_BEACH),
                            scene("scene-10", 5, "Souvenir ancre", "18:45", "La conclusion relie l'immersion au reste du voyage.", "conclusion", BALI_HERO))));

    private static final TravelDto BALI_TRAVEL = new TravelDto(
            "bali-2025",
            "Bali 2025",
            "Bali",
            "Indonesie",
            2025,
            "Arrivee, decouverte et immersion dans une saison de demonstration.",
            "Une demo V2 avec synopsis, timelines ordonnees et scene IA explicite.",
            BALI_BEACH,
            BALI_HERO,
            BALI_RICE,
            "39 min",
            EPISODES.size(),
            283,
            62,
            "7 min restantes",
            true,
            List.of("Cinematographique", "Emotionnel", "Aventure"),
            EPISODES);

    public List<TravelDto> getTravels() {
        return List.of(BALI_TRAVEL);
    }

    public Optional<TravelDto> getTravel(String id) {
        return getTravels().stream()
                .filter((travel) -> travel.id().equals(id))
                .findFirst();
    }

    public List<EpisodeDto> getEpisodes() {
        return EPISODES;
    }

    public Optional<EpisodeDto> getEpisode(String id) {
        return EPISODES.stream()
                .filter((episode) -> episode.id().equals(id))
                .findFirst();
    }

    private static EpisodeDto episode(
            String id,
            int episodeNumber,
            String title,
            String subtitle,
            String summary,
            String duration,
            String location,
            String date,
            int photoCount,
            String coverImage,
            String videoStill,
            int progress,
            String remaining,
            List<String> keyMoments,
            List<SceneDto> scenes) {
        List<SceneDto> orderedScenes = scenes.stream()
                .sorted(Comparator.comparingInt(SceneDto::order))
                .toList();
        validateTimeline(orderedScenes);

        return new EpisodeDto(
                id,
                "bali-2025",
                1,
                episodeNumber,
                title,
                subtitle,
                summary,
                duration,
                location,
                date,
                photoCount,
                coverImage,
                videoStill,
                progress,
                remaining,
                keyMoments,
                orderedScenes);
    }

    private static SceneDto scene(
            String id,
            int order,
            String title,
            String timecode,
            String voiceOverText,
            String type,
            String imageUrl) {
        return new SceneDto(id, order, title, timecode, voiceOverText, type, "generated", imageUrl, false, null);
    }

    private static SceneDto aiScene(
            String id,
            int order,
            String title,
            String timecode,
            String voiceOverText,
            String type,
            String aiPrompt) {
        return new SceneDto(id, order, title, timecode, voiceOverText, type, "ai_reconstructed", null, true, aiPrompt);
    }

    private static void validateTimeline(List<SceneDto> scenes) {
        Set<Integer> orders = new HashSet<>();
        boolean hasIntro = false;
        boolean hasConclusion = false;

        for (SceneDto scene : scenes) {
            if (!orders.add(scene.order())) {
                throw new IllegalStateException("Duplicate scene order");
            }
            hasIntro = hasIntro || "intro".equals(scene.type());
            hasConclusion = hasConclusion || "conclusion".equals(scene.type());
        }

        if (!hasIntro || !hasConclusion) {
            throw new IllegalStateException("Timeline must contain intro and conclusion");
        }
    }
}
