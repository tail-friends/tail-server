package com.tailfriends.animal.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class AnimalHomeIntegrationTest extends IntegrationTestSupport {

    private static final String CHARACTER_BODY = """
            { "shape": "gecko", "patternType": "spots",
              "colors": { "body": "#E0A33A", "pattern": "#3A2A1E", "belly": "#F2E6C8", "eye": "#2B2B2B" } }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 평소_사육장을_바꾸면_개체_상세의_캐릭터_사육장이_바뀐다() throws Exception {
        String token = tokenOfNewUser();
        int animalId = createAnimalWithCharacter(token);
        // 레오파드 게코의 기본 사육장
        mockMvc.perform(get("/api/v1/animals/" + animalId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.character.home").value("desert"));

        changeHome(token, animalId, "Pond")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.home").value("pond"));

        mockMvc.perform(get("/api/v1/animals/" + animalId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.character.home").value("pond"));
    }

    @Test
    void 남의_개체의_평소_사육장은_바꿀_수_없다() throws Exception {
        String owner = tokenOfNewUser();
        String other = tokenOfNewUser();
        int animalId = createAnimalWithCharacter(owner);

        changeHome(other, animalId, "pond")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/animals/" + animalId).header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(jsonPath("$.data.character.home").value("desert"));
    }

    private ResultActions changeHome(String token, int animalId, String home) throws Exception {
        return mockMvc.perform(patch("/api/v1/animals/" + animalId + "/home").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"home\": \"" + home + "\" }"));
    }

    private int createAnimalWithCharacter(String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": 1, \"feedIntervalDays\": 4 }"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int animalId = JsonPath.read(body, "$.data.id");
        mockMvc.perform(put("/api/v1/animals/" + animalId + "/character").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CHARACTER_BODY))
                .andExpect(status().isOk());
        return animalId;
    }

    private String tokenOfNewUser() {
        User user = userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("사용자")
                .build());
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
