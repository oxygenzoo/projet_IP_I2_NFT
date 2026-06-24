package com.nft.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
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
                .andExpect(jsonPath("$[0].id").value("bali-2025"))
                .andExpect(jsonPath("$[0].episodes[0].id").value("1"));
    }

    @Test
    void episodesEndpointReturnsBackendData() throws Exception {
        mockMvc.perform(get("/api/episodes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].travelId").value("bali-2025"));
    }

    @Test
    void canSaveCompleteQuestionnaireForTravel() throws Exception {
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.travelId").value("bali-2025"))
                .andExpect(jsonPath("$.style").value("Cinematographique"))
                .andExpect(jsonPath("$.people").value("Tout le monde"))
                .andExpect(jsonPath("$.moments").value("Paysages"))
                .andExpect(jsonPath("$.tone").value("Inspirant"));
    }

    @Test
    void refusesEmptyQuestionnaire() throws Exception {
        mockMvc.perform(post("/api/travels/{travelId}/preferences", "kyrgyzstan-2025")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void canReloadSavedPreferences() throws Exception {
        mockMvc.perform(post("/api/travels/{travelId}/preferences", "italy-2024")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "style": "Documentaire",
                                  "people": "Couple",
                                  "moments": "Culture",
                                  "tone": "Nostalgique"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/travels/{travelId}/preferences", "italy-2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.travelId").value("italy-2024"))
                .andExpect(jsonPath("$.style").value("Documentaire"))
                .andExpect(jsonPath("$.people").value("Couple"))
                .andExpect(jsonPath("$.moments").value("Culture"))
                .andExpect(jsonPath("$.tone").value("Nostalgique"));
    }

    @Test
    void preferencesAreAssociatedWithTheRequestedTravelOnly() throws Exception {
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
                .andExpect(jsonPath("$.travelId").value("bali-2025"));

        mockMvc.perform(get("/api/travels/{travelId}/preferences", "bali-2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.people").value("Famille"));

        mockMvc.perform(get("/api/travels/{travelId}/preferences", "kyrgyzstan-2025"))
                .andExpect(status().isNotFound());
    }
}
