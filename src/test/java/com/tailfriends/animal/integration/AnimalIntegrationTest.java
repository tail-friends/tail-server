package com.tailfriends.animal.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

class AnimalIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 개체를_등록하고_조회하고_수정하고_삭제한다() throws Exception {
        String token = tokenOfNewUser();

        String body = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "망고", "speciesId": 1, "morph": "탠저린", "sex": "FEMALE",
                                  "hatchDate": "2025-06-20", "feedIntervalDays": 4, "memo": "핫존 32℃" }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.data.id");

        mockMvc.perform(get("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.animals", hasSize(1)))
                .andExpect(jsonPath("$.data.animals[0].species").value("레오파드 게코"));

        mockMvc.perform(patch("/api/v1/animals/" + id).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"feedIntervalDays\": 5 }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedIntervalDays").value(5))
                .andExpect(jsonPath("$.data.morph").value("탠저린"));

        mockMvc.perform(get("/api/v1/animals/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedIntervalDays").value(5))
                .andExpect(jsonPath("$.data.hatchDate").value("2025-06-20"));

        mockMvc.perform(delete("/api/v1/animals/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/animals/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    @Test
    void 남의_개체는_조회_수정_삭제할_수_없다() throws Exception {
        String owner = tokenOfNewUser();
        String other = tokenOfNewUser();
        String body = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesName\": \"사바나 모니터\", \"feedIntervalDays\": 7 }"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.data.id");

        mockMvc.perform(get("/api/v1/animals/" + id).header(HttpHeaders.AUTHORIZATION, other))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
        mockMvc.perform(patch("/api/v1/animals/" + id).header(HttpHeaders.AUTHORIZATION, other)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"name\": \"뺏기\" }"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/animals/" + id).header(HttpHeaders.AUTHORIZATION, other))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, other))
                .andExpect(jsonPath("$.data.animals", hasSize(0)));
        mockMvc.perform(get("/api/v1/animals/" + id).header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("망고"));
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
