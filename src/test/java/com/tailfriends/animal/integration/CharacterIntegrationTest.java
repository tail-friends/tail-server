package com.tailfriends.animal.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.animal.repository.AnimalCharacterRepository;
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

class CharacterIntegrationTest extends IntegrationTestSupport {

    private static final String BODY = """
            { "shape": "gecko", "patternType": "spots",
              "colors": { "body": "%s", "pattern": "#3A2A1E", "belly": "#F2E6C8", "eye": "#2B2B2B" },
              "extra": { "headSize": 0.6, "tailWidth": 0.8, "patternDensity": 0.4 } }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnimalCharacterRepository characterRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 처음_저장하면_만들고_다시_저장하면_바꾼다() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token);

        save(token, animalId, "#e0a33a")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.renderStyle").value("ILLUSTRATION_2D"))
                .andExpect(jsonPath("$.data.colors.body").value("#E0A33A"))
                .andExpect(jsonPath("$.data.extra.tailWidth").value(0.8));
        save(token, animalId, "#112233")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.colors.body").value("#112233"));

        assertThat(characterRepository.findById(animalId.longValue()))
                .hasValueSatisfying(character -> assertThat(character.getColorBody()).isEqualTo("#112233"));
    }

    @Test
    void 개체를_삭제하면_캐릭터도_함께_삭제된다() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token);
        save(token, animalId, "#E0A33A").andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/animals/" + animalId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        assertThat(characterRepository.findById(animalId.longValue())).isEmpty();
    }

    @Test
    void 남의_개체에는_저장할_수_없다() throws Exception {
        Integer animalId = createAnimal(tokenOfNewUser());

        save(tokenOfNewUser(), animalId, "#E0A33A")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
    }

    private ResultActions save(String token, Integer animalId, String bodyColor) throws Exception {
        return mockMvc.perform(put("/api/v1/animals/" + animalId + "/character").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.formatted(bodyColor)));
    }

    private Integer createAnimal(String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": 1, \"feedIntervalDays\": 4 }"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.id");
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
