package com.nft.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
}
