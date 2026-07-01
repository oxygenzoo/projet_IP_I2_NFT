package com.nft.backend;

import java.nio.charset.StandardCharsets;

import com.nft.backend.model.User;
import com.nft.backend.repository.TravelRepository;
import com.nft.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
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
    private UserRepository userRepository;

    @Autowired
    private TravelRepository travelRepository;

    @BeforeEach
    void cleanDatabase() {
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
    void createsValidUser() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "demo@example.com",
                                  "passwordHash": "hashed-password",
                                  "consentRgpd": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("demo@example.com"))
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
    void generationWorkflowCreatesReadyEpisodeFromTravelPhotosAndPreferences() throws Exception {
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
                "fake-image".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/generation/jobs")
                        .file(image)
                        .param("travelId", travelId)
                        .param("title", "Voyage IA")
                        .param("destination", "Lisbonne")
                        .param("preferences", """
                                {"style":"Cinematographique","people":"Famille","moments":"Paysages","tone":"Inspirant"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ready"))
                .andExpect(jsonPath("$.selection_report.photos_recues").value(1))
                .andExpect(jsonPath("$.script.preferences", containsString("Cinematographique")));

        mockMvc.perform(get("/api/travels/{travelId}/episodes", travelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Episode 1 - Voyage IA"))
                .andExpect(jsonPath("$[0].status").value("ready"));

        mockMvc.perform(get("/api/episodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].scenes", hasSize(1)))
                .andExpect(jsonPath("$[0].remaining").value("Pret a regarder"));
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
}
