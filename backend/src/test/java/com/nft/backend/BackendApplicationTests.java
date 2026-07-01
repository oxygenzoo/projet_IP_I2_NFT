package com.nft.backend;

import com.nft.backend.model.Episode;
import com.nft.backend.model.EpisodeStatus;
import com.nft.backend.model.Travel;
import com.nft.backend.repository.EpisodeRepository;
import com.nft.backend.repository.PhotoRepository;
import com.nft.backend.repository.TravelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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

    @BeforeEach
    void cleanDatabase() {
        photoRepository.deleteAll();
        episodeRepository.deleteAll();
        travelRepository.deleteAll();
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
    void travelDetailReturnsNotFoundWithoutDemoData() throws Exception {
        mockMvc.perform(get("/api/travels/{id}", "bali-2025"))
                .andExpect(status().isNotFound());
    }

    @Test
    void episodeDetailReturnsNotFoundWithoutDemoData() throws Exception {
        mockMvc.perform(get("/api/episodes/{id}", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void refusesQuestionnaireForUnknownTravel() throws Exception {
        mockMvc.perform(post("/api/travels/{travelId}/preferences", "bali-2025")
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
    void preferencesAreNotStoredForDemoTravels() throws Exception {
        mockMvc.perform(post("/api/travels/{travelId}/preferences", "bali-2025")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "style": "Emotionnel",
                                  "people": "Famille",
                                  "moments": "Rencontres",
                                  "tone": "Fun"
                                }
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/travels/{travelId}/preferences", "bali-2025"))
                .andExpect(status().isNotFound());
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
    void sharesEpisodeAndRejectsInvalidToken() throws Exception {
        Episode episode = episodeRepository.save(new Episode(
                travelRepository.save(new Travel("Bali", "Bali", "Demo")),
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

        String token = episodeRepository.findById(episode.getId()).orElseThrow().getShareToken();
        mockMvc.perform(get("/api/public/episodes/{shareToken}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Arrivee"));

        mockMvc.perform(get("/api/public/episodes/{shareToken}", "invalid-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void exportsEpisodeReadyOrFailed() throws Exception {
        Episode episode = episodeRepository.save(new Episode(
                travelRepository.save(new Travel("Bali", "Bali", "Demo")),
                1,
                "Arrivee",
                "Ubud",
                null,
                "Resume",
                "",
                "Cinematographique",
                EpisodeStatus.READY));

        mockMvc.perform(post("/api/travels/{travelId}/episodes/{episodeId}/export",
                        episode.getTravel().getId(), episode.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportStatus").value("ready"))
                .andExpect(jsonPath("$.videoUrl").value("/demo-video.mp4"));

        mockMvc.perform(post("/api/travels/{travelId}/episodes/{episodeId}/export",
                        episode.getTravel().getId(), episode.getId())
                        .param("fail", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportStatus").value("failed"));
    }
}
