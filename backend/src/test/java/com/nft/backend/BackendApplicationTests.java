package com.nft.backend;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.nft.backend.dto.ai.PhotoAnalysisDto;
import com.nft.backend.dto.travel.EpisodeDto;
import com.nft.backend.dto.travel.SceneDto;
import com.nft.backend.dto.travel.SynopsisDto;
import com.nft.backend.service.EpisodeSynopsisService;
import com.nft.backend.service.MockAiAnalysisService;
import com.nft.backend.service.TravelCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private TravelCatalogService travelCatalogService;

    @Autowired
    private EpisodeSynopsisService episodeSynopsisService;

    @Autowired
    private MockAiAnalysisService mockAiAnalysisService;

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
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("bali-2025"))
                .andExpect(jsonPath("$[0].episodes", hasSize(2)));
    }

    @Test
    void episodesEndpointReturnsBackendData() throws Exception {
        mockMvc.perform(get("/api/episodes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].scenes", hasSize(5)))
                .andExpect(jsonPath("$[0].keyMoments", hasSize(3)));
    }

    @Test
    void travelDetailReturnsDemoData() throws Exception {
        mockMvc.perform(get("/api/travels/{id}", "bali-2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination").value("Bali"));
    }

    @Test
    void episodeDetailReturnsTimelineInOrder() throws Exception {
        mockMvc.perform(get("/api/episodes/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenes[0].order").value(1))
                .andExpect(jsonPath("$.scenes[0].type").value("intro"))
                .andExpect(jsonPath("$.scenes[4].order").value(5))
                .andExpect(jsonPath("$.scenes[4].type").value("conclusion"))
                .andExpect(jsonPath("$.scenes[1].voiceOverText").value(containsString("offrandes")));
    }

    @Test
    void episodeDetailReturnsAiReconstructionData() throws Exception {
        mockMvc.perform(get("/api/episodes/{id}", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenes[2].isAiReconstructed").value(true))
                .andExpect(jsonPath("$.scenes[2].aiPrompt").value(containsString("Ubud")))
                .andExpect(jsonPath("$.scenes[2].generationStatus").value("ai_reconstructed"));
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
    void mockAiServiceReturnsStableData() {
        List<PhotoAnalysisDto> first = mockAiAnalysisService.analyzePhotos(List.of("photo-1", "photo-2"));
        List<PhotoAnalysisDto> second = mockAiAnalysisService.analyzePhotos(List.of("photo-1", "photo-2"));

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(2);
        assertThat(first.get(0).qualityScore()).isPositive();
        assertThat(first.get(0).tags()).contains("paysage", "groupe", "monument");
        assertThat(mockAiAnalysisService.analyzePhotos(List.of())).isEmpty();
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
    void preferencesAreStoredForDemoTravels() throws Exception {
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.travelId").value("bali-2025"))
                .andExpect(jsonPath("$.tone").value("Fun"));

        mockMvc.perform(get("/api/travels/{travelId}/preferences", "bali-2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moments").value("Rencontres"));
    }
}
