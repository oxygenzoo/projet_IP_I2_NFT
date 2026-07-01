package com.nft.backend;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.nft.backend.dto.travel.EpisodeDto;
import com.nft.backend.dto.travel.SceneDto;
import com.nft.backend.dto.travel.SynopsisDto;
import com.nft.backend.model.Episode;
import com.nft.backend.model.EpisodeStatus;
import com.nft.backend.model.Travel;
import com.nft.backend.model.User;
import com.nft.backend.repository.EpisodeRepository;
import com.nft.backend.repository.PhotoRepository;
import com.nft.backend.repository.TravelRepository;
import com.nft.backend.repository.UserRepository;
import com.nft.backend.service.EpisodeSynopsisService;
import com.nft.backend.service.TravelCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TravelRepository travelRepository;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TravelCatalogService travelCatalogService;

    @Autowired
    private EpisodeSynopsisService episodeSynopsisService;

    @BeforeEach
    void cleanDatabase() {
        photoRepository.deleteAll();
        episodeRepository.deleteAll();
        travelRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void corsAllowsAngularDevOrigin() throws Exception {
        mockMvc.perform(options("/health")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")));
    }

    @Test
    void corsAllowsVercelPreviewOrigins() throws Exception {
        String origin = "https://projet-ip-i2-5faavc4mb-oxygenzoos-projects.vercel.app";

        mockMvc.perform(options("/api/episodes")
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")));
    }

    @Test
    void travelsEndpointReturnsBackendData() throws Exception {
        mockMvc.perform(get("/api/travels"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void episodesEndpointReturnsBackendData() throws Exception {
        mockMvc.perform(get("/api/episodes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void episodeDetailExposesVideoUrlForPlayer() throws Exception {
        Episode episode = new Episode(
                travelRepository.save(new Travel("Bali", "Bali", "Production")),
                1,
                "Arrivee",
                "Ubud",
                null,
                "Resume",
                "",
                "Cinematographique",
                EpisodeStatus.READY);
        episode.updateExport("ready", "https://cdn.example.com/videos/episode.mp4");
        Episode savedEpisode = episodeRepository.save(episode);

        mockMvc.perform(get("/api/episodes/{id}", savedEpisode.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportStatus").value("ready"))
                .andExpect(jsonPath("$.videoUrl").value("https://cdn.example.com/videos/episode.mp4"));
    }

    @Test
    void travelDetailReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/travels/{id}", "bali-2025"))
                .andExpect(status().isNotFound());
    }

    @Test
    void episodeDetailReturnsTimelineInOrder() throws Exception {
        mockMvc.perform(get("/api/episodes/{id}", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void episodeDetailReturnsAiReconstructionData() throws Exception {
        mockMvc.perform(get("/api/episodes/{id}", "2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void timelinesHaveUniqueOrderAndRequiredBounds() {
        for (EpisodeDto episode : travelCatalogService.getEpisodes()) {
            Set<Integer> orders = new HashSet<>();
            for (SceneDto scene : episode.scenes()) {
                assertThat(orders.add(scene.order())).isTrue();
            }

            assertThat(episode.scenes()).extracting(SceneDto::type).contains("intro", "conclusion");
        }
    }

    @Test
    void synopsisServiceGeneratesStoredShape() {
        SynopsisDto synopsis = episodeSynopsisService.generate("Bali", "Emotionnel", List.of("temple-ubud.jpg"));

        assertThat(synopsis.title()).contains("Bali");
        assertThat(synopsis.summary()).contains("Bali", "emotionnel");
        assertThat(synopsis.keyMoments()).isNotEmpty();
    }

    @Test
    void refusesQuestionnaireForUnknownTravel() throws Exception {
        mockMvc.perform(post("/api/travels/{travelId}/preferences", "unknown-travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "style": "Cinematographique",
                                  "people": "Tout le monde",
                                  "moments": "Paysages",
                                  "tone": "Inspirant"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void refusesEmptyQuestionnaire() throws Exception {
        mockMvc.perform(post("/api/travels/{travelId}/preferences", "kyrgyzstan-2025")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void preferencesReturnNotFoundWithoutTravel() throws Exception {
        mockMvc.perform(get("/api/travels/{travelId}/preferences", "italy-2024"))
                .andExpect(status().isNotFound());
    }

    @Test
    void preferencesAreStoredForPersistedTravels() throws Exception {
        String userId = createUserAndReturnId("preferences-owner@example.com");
        String travelId = createTravelAndReturnId(userId, "Voyage preferences");

        mockMvc.perform(post("/api/travels/{travelId}/preferences", travelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "style": "Emotionnel",
                                  "people": "Famille",
                                  "moments": "Rencontres",
                                  "tone": "Fun"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.travelId").value(travelId))
                .andExpect(jsonPath("$.tone").value("Fun"));

        mockMvc.perform(get("/api/travels/{travelId}/preferences", travelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moments").value("Rencontres"));
    }

    @Test
    void createsValidUser() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "passwordHash": "hashed-password",
                                  "consentRgpd": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.consentRgpd").value(true));
    }

    @Test
    void refusesUserWithEmptyEmail() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": " ",
                                  "passwordHash": "hashed-password"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readsExistingUser() throws Exception {
        User user = userRepository.save(new User("reader@example.com", "hashed-password", false));

        mockMvc.perform(get("/api/users/{id}", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value("reader@example.com"));
    }

    @Test
    void createsAndListsTravel() throws Exception {
        String userId = createUserAndReturnId("travel-owner@example.com");

        mockMvc.perform(post("/api/travels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "title": "Road trip",
                                  "destination": "Lisbonne",
                                  "startDate": "2026-06-01",
                                  "endDate": "2026-06-08",
                                  "description": "Une semaine au Portugal"
                                }
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Road trip"))
                .andExpect(jsonPath("$.destination").value("Lisbonne"))
                .andExpect(jsonPath("$.startDate").value("2026-06-01"));

        mockMvc.perform(get("/api/travels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Road trip"));
    }

    @Test
    void refusesTravelWithoutTitle() throws Exception {
        String userId = createUserAndReturnId("missing-title@example.com");

        mockMvc.perform(post("/api/travels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "title": " ",
                                  "destination": "Rome"
                                }
                                """.formatted(userId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesTravel() throws Exception {
        String userId = createUserAndReturnId("update-owner@example.com");
        MvcResult created = mockMvc.perform(post("/api/travels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "title": "Ancien titre",
                                  "destination": "Lyon",
                                  "description": "Avant"
                                }
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andReturn();
        String travelId = createdIdFromLocation(created);

        mockMvc.perform(put("/api/travels/{id}", travelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "title": "Nouveau titre",
                                  "destination": "Marseille",
                                  "startDate": "2026-07-10",
                                  "endDate": "2026-07-12",
                                  "description": "Apres"
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Nouveau titre"))
                .andExpect(jsonPath("$.destination").value("Marseille"))
                .andExpect(jsonPath("$.endDate").value("2026-07-12"));
    }

    @Test
    @DirtiesContext
    void generationWorkflowRejectsWhenAiServiceIsUnavailable() throws Exception {
        String userId = createUserAndReturnId("generation-owner@example.com");
        String travelId = createTravelAndReturnId(userId, "Voyage IA");

        mockMvc.perform(post("/api/travels/{travelId}/photos/metadata", travelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "filename": "existing.jpg",
                                  "size": 1234,
                                  "type": "image/jpeg"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/travels/{travelId}/preferences", travelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "style": "Cinematographique",
                                  "people": "Famille",
                                  "moments": "Paysages",
                                  "tone": "Inspirant"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.style").value("Cinematographique"));

        MockMultipartFile image = new MockMultipartFile(
                "images",
                "trip.jpg",
                "image/jpeg",
                "image-bytes".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/generation/jobs")
                        .file(image)
                        .param("travelId", travelId)
                        .param("title", "Voyage IA")
                        .param("destination", "Lisbonne")
                .param("preferences", """
                        {"style":"Cinematographique","people":"Famille","moments":"Paysages","tone":"Inspirant"}
                        """))
                .andExpect(status().isBadGateway());

        mockMvc.perform(get("/api/travels/{travelId}/episodes", travelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/episodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private String createUserAndReturnId(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "passwordHash": "hashed-password"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();

        return createdIdFromLocation(result);
    }

    private String createTravelAndReturnId(String userId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/travels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s",
                                  "title": "%s",
                                  "destination": "Lisbonne",
                                  "description": "Generation"
                                }
                                """.formatted(userId, title)))
                .andExpect(status().isCreated())
                .andReturn();

        return createdIdFromLocation(result);
    }

    private String createdIdFromLocation(MvcResult result) {
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        return location.substring(location.lastIndexOf('/') + 1);
    }

    @Test
    void uploadsJpgAndServesThenDeletesIt() throws Exception {
        Travel travel = travelRepository.save(new Travel("Bali", "Bali", "Demo"));
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/travels/{travelId}/photos", travel.getId())
                        .file(file)
                        .param("consentRgpd", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl", startsWith("/api/travels/")))
                .andExpect(jsonPath("$.consentRgpd").value(true))
                .andExpect(jsonPath("$.consentDate", not(nullValue())));

        var photo = photoRepository.findAll().getFirst();
        mockMvc.perform(get("/api/travels/{travelId}/photos/{photoId}/file", travel.getId(), photo.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));

        mockMvc.perform(delete("/api/travels/{travelId}/photos/{photoId}", travel.getId(), photo.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/travels/{travelId}/photos/{photoId}/file", travel.getId(), photo.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadsPng() throws Exception {
        Travel travel = travelRepository.save(new Travel("Rome", "Italie", "Demo"));
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/travels/{travelId}/photos", travel.getId())
                        .file(file)
                        .param("consentRgpd", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("image/png"));
    }

    @Test
    void refusesUploadWithoutConsentAndTooLargeFile() throws Exception {
        Travel travel = travelRepository.save(new Travel("Tokyo", "Japon", "Demo"));
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[] {1, 2, 3});
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.jpg", "image/jpeg", new byte[32]);

        mockMvc.perform(multipart("/api/travels/{travelId}/photos", travel.getId())
                        .file(file))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/travels/{travelId}/photos", travel.getId())
                        .file(largeFile)
                        .param("consentRgpd", "true"))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void createsTransientShareTokenAndPublicLookupStaysUnavailableWithoutShareColumn() throws Exception {
        Episode episode = episodeRepository.save(new Episode(
                travelRepository.save(new Travel("Bali", "Bali", "Production")),
                1,
                "Arrivee",
                "Ubud",
                null,
                "Resume",
                "",
                "Cinematographique",
                EpisodeStatus.READY));

        mockMvc.perform(post("/api/travels/{travelId}/episodes/{episodeId}/share",
                        episode.getTravel().getId(), episode.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareToken", not(nullValue())));

        mockMvc.perform(get("/api/public/episodes/{shareToken}", episode.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(episode.getId().toString()))
                .andExpect(jsonPath("$.title").value("Arrivee"));

        mockMvc.perform(get("/api/public/episodes/{shareToken}", "invalid-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void exportsEpisodeReadyOrFailed() throws Exception {
        Episode episode = new Episode(
                travelRepository.save(new Travel("Bali", "Bali", "Production")),
                1,
                "Arrivee",
                "Ubud",
                null,
                "Resume",
                "",
                "Cinematographique",
                EpisodeStatus.READY);
        episode.updateExport("idle", "https://cdn.example.com/videos/episode.mp4");
        Episode savedEpisode = episodeRepository.save(episode);

        mockMvc.perform(post("/api/travels/{travelId}/episodes/{episodeId}/export",
                        savedEpisode.getTravel().getId(), savedEpisode.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportStatus").value("ready"))
                .andExpect(jsonPath("$.videoUrl").value("https://cdn.example.com/videos/episode.mp4"));

        mockMvc.perform(post("/api/travels/{travelId}/episodes/{episodeId}/export",
                        savedEpisode.getTravel().getId(), savedEpisode.getId())
                        .param("fail", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportStatus").value("failed"));
    }
}
