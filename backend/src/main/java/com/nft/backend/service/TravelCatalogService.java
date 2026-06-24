package com.nft.backend.service;

import java.util.List;
import java.util.Optional;

import com.nft.backend.dto.travel.EpisodeDto;
import com.nft.backend.dto.travel.SceneDto;
import com.nft.backend.dto.travel.TravelDto;
import org.springframework.stereotype.Service;

@Service
public class TravelCatalogService {

    private static final String BALI_HERO = "https://images.unsplash.com/photo-1537996194471-e657df975ab4?auto=format&fit=crop&w=1800&q=85";
    private static final String BALI_BEACH = "https://images.unsplash.com/photo-1518548419970-58e3b4079ab2?auto=format&fit=crop&w=1200&q=85";
    private static final String BALI_RICE = "https://images.unsplash.com/photo-1555400038-63f5ba517a47?auto=format&fit=crop&w=1200&q=85";
    private static final String KYRGYZ_HERO = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1800&q=85";
    private static final String KYRGYZ_CAMP = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=85";
    private static final String ITALY_HERO = "https://images.unsplash.com/photo-1523906834658-6e24ef2386f9?auto=format&fit=crop&w=1800&q=85";
    private static final String ITALY_STREET = "https://images.unsplash.com/photo-1514890547357-a9ee288728e0?auto=format&fit=crop&w=1200&q=85";

    private final List<EpisodeDto> episodes = List.of(
            new EpisodeDto(
                    "1",
                    "bali-2025",
                    1,
                    1,
                    "L'arrivee a Bali",
                    "Les premieres lumieres de l ile",
                    "Le voyage commence dans l humidite douce de Denpasar, entre les scooters, les offrandes posees devant les portes et la premiere route vers Ubud.",
                    "18 min",
                    "Denpasar, Ubud",
                    "12 mai 2025",
                    127,
                    BALI_HERO,
                    BALI_BEACH,
                    62,
                    "7 min restantes",
                    List.of(
                            new SceneDto("scene-1", "Arrivee", "00:00", "Sortie de l aeroport et premiere image du voyage.", BALI_HERO),
                            new SceneDto("scene-2", "Decouverte", "03:20", "Les rues de Denpasar defilent comme une introduction de serie.", BALI_BEACH),
                            new SceneDto("scene-3", "Premiere randonnee", "07:10", "Les rizieres d Ubud donnent au recit son premier grand plan.", BALI_RICE))),
            new EpisodeDto(
                    "2",
                    "bali-2025",
                    1,
                    2,
                    "Rizieres et temples",
                    "Une journee entre silence et vertige",
                    "Les marches d Ubud, les temples caches et les terrasses de riz composent une journee plus lente.",
                    "22 min",
                    "Ubud",
                    "14 mai 2025",
                    186,
                    BALI_RICE,
                    BALI_RICE,
                    0,
                    "22 min restantes",
                    List.of(new SceneDto("scene-4", "Le sentier des rizieres", "02:05", "La camera traverse la brume et retrouve les meilleurs panoramas.", BALI_RICE))),
            new EpisodeDto(
                    "3",
                    "kyrgyzstan-2025",
                    1,
                    1,
                    "La route de Song-Kul",
                    "Pistes, yourtes et grands plateaux",
                    "Un episode de grands espaces, entre routes de terre, the chaud et chevaux au loin.",
                    "24 min",
                    "Song-Kul",
                    "3 juin 2025",
                    164,
                    KYRGYZ_HERO,
                    KYRGYZ_CAMP,
                    20,
                    "19 min restantes",
                    List.of(new SceneDto("scene-5", "Plateau", "05:30", "Les montagnes ouvrent la saison avec une respiration documentaire.", KYRGYZ_HERO))),
            new EpisodeDto(
                    "4",
                    "italy-2024",
                    1,
                    1,
                    "Venise au matin",
                    "Canaux silencieux et premiers cafes",
                    "Une archive europeenne elegante, entre architecture, nourriture et conversations tardives.",
                    "19 min",
                    "Venise",
                    "22 aout 2024",
                    148,
                    ITALY_HERO,
                    ITALY_STREET,
                    68,
                    "6 min restantes",
                    List.of(new SceneDto("scene-6", "Canaux", "01:15", "Les premiers plans posent une ambiance de cinema.", ITALY_HERO))));

    private final List<TravelDto> travels = List.of(
            new TravelDto(
                    "bali-2025",
                    "Bali 2025",
                    "Bali",
                    "Indonesie",
                    2025,
                    "Rizieres, plages et temples au rythme d une premiere grande saison.",
                    "Une serie solaire qui transforme les photos d Ubud, Canggu et Denpasar en recit de voyage premium.",
                    BALI_BEACH,
                    BALI_HERO,
                    BALI_RICE,
                    "56 min",
                    2,
                    313,
                    62,
                    "7 min restantes",
                    true,
                    List.of("Cinematographique", "Emotionnel", "Aventure"),
                    episodesForTravel("bali-2025")),
            new TravelDto(
                    "kyrgyzstan-2025",
                    "Kirghizistan 2025",
                    "Song-Kul",
                    "Kirghizistan",
                    2025,
                    "Une traversee nomade entre pistes, yourtes et grands plateaux.",
                    "Un voyage plus brut et humain, ideal pour montrer la reconstruction narrative de souvenirs disperses.",
                    KYRGYZ_HERO,
                    KYRGYZ_HERO,
                    KYRGYZ_CAMP,
                    "24 min",
                    1,
                    164,
                    20,
                    "19 min restantes",
                    false,
                    List.of("Documentaire", "Inspirant", "Nostalgique"),
                    episodesForTravel("kyrgyzstan-2025")),
            new TravelDto(
                    "italy-2024",
                    "Italie 2024",
                    "Venise",
                    "Italie",
                    2024,
                    "Canaux silencieux, diners tardifs et villes qui ressemblent a du cinema.",
                    "Une saison europeenne elegante, entre architecture, nourriture et conversations qui durent.",
                    ITALY_HERO,
                    ITALY_HERO,
                    ITALY_STREET,
                    "19 min",
                    1,
                    148,
                    68,
                    "6 min restantes",
                    false,
                    List.of("Nostalgique", "Culture", "Couple"),
                    episodesForTravel("italy-2024")));

    public List<TravelDto> getTravels() {
        return travels;
    }

    public Optional<TravelDto> getTravel(String id) {
        return travels.stream().filter((travel) -> travel.id().equals(id)).findFirst();
    }

    public List<EpisodeDto> getEpisodes() {
        return episodes;
    }

    public Optional<EpisodeDto> getEpisode(String id) {
        return episodes.stream().filter((episode) -> episode.id().equals(id)).findFirst();
    }

    private List<EpisodeDto> episodesForTravel(String travelId) {
        return episodes.stream().filter((episode) -> episode.travelId().equals(travelId)).toList();
    }
}
