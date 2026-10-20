package com.tailfriends.auth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.global.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@TestPropertySource(properties = "dev.login.enabled=true")
class DevLoginIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 테스트_로그인으로_받은_토큰으로_API를_호출한다() throws Exception {
        String name = "tester-" + UUID.randomUUID();
        String body = login("{ \"name\": \"%s\" }".formatted(name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("KEEPER"))
                .andReturn().getResponse().getContentAsString();
        Integer userId = JsonPath.read(body, "$.data.userId");
        String accessToken = JsonPath.read(body, "$.data.accessToken");

        mockMvc.perform(get("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());
        login("{ \"name\": \"%s\" }".formatted(name))
                .andExpect(jsonPath("$.data.userId").value(userId));
    }

    @Test
    void role을_보내면_그_권한으로_바꾼다() throws Exception {
        login("{ \"name\": \"admin-%s\", \"role\": \"ADMIN\" }".formatted(UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    private ResultActions login(String content) throws Exception {
        return mockMvc.perform(post("/api/v1/dev/login").contentType(MediaType.APPLICATION_JSON).content(content));
    }
}
