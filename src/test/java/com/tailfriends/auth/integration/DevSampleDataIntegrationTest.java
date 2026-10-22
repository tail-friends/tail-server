package com.tailfriends.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.animal.repository.AnimalCharacterRepository;
import com.tailfriends.global.support.IntegrationTestSupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@TestPropertySource(properties = "dev.login.enabled=true")
class DevSampleDataIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnimalCharacterRepository characterRepository;

    @Test
    void 샘플_데이터를_만들면_개체마다_급여_상태가_다르게_보인다() throws Exception {
        String token = login();

        String body = mockMvc.perform(post("/api/v1/dev/sample-data").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.animalIds", hasSize(3)))
                .andReturn().getResponse().getContentAsString();
        List<Integer> animalIds = JsonPath.read(body, "$.data.animalIds");

        mockMvc.perform(get("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.animals", hasSize(3)))
                .andExpect(jsonPath("$.data.animals[0].name").value("망고"))
                .andExpect(jsonPath("$.data.animals[0].feed.status").value("UPCOMING"))
                .andExpect(jsonPath("$.data.animals[0].feed.daysLeft").value(2))
                .andExpect(jsonPath("$.data.animals[1].feed.status").value("OVERDUE"))
                .andExpect(jsonPath("$.data.animals[1].feed.daysLeft").value(-2))
                .andExpect(jsonPath("$.data.animals[2].feed.status").value("DUE"));

        mockMvc.perform(get("/api/v1/animals/" + animalIds.get(0) + "/weights").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points", hasSize(9)));
        assertThat(characterRepository.findById(animalIds.get(0).longValue()))
                .hasValueSatisfying(character -> assertThat(character.getColorBody()).isEqualTo("#E0A33A"));
        mockMvc.perform(get("/api/v1/logs").param("animalId", String.valueOf(animalIds.get(2)))
                        .param("type", "SHED").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.logs", hasSize(3)));
    }

    @Test
    void 토큰이_없으면_만들지_않는다() throws Exception {
        mockMvc.perform(post("/api/v1/dev/sample-data"))
                .andExpect(status().isUnauthorized());
    }

    private String login() throws Exception {
        String body = mockMvc.perform(post("/api/v1/dev/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"sample-%s\" }".formatted(UUID.randomUUID().toString().substring(0, 8))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.data.accessToken");
    }
}
