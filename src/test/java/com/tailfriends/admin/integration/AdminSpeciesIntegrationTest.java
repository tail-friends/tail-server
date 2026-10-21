package com.tailfriends.admin.integration;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.species.repository.SpeciesRepository;
import com.tailfriends.user.domain.Role;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 컨테이너 DB를 다른 통합 테스트와 함께 쓰므로, 만든 종은 테스트가 끝나면 지운다. (공개 종 목록은 초기 10종 기준)
 */
class AdminSpeciesIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private SpeciesRepository speciesRepository;

    private final List<Long> createdIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        speciesRepository.deleteAllById(createdIds);
    }

    @Test
    void 관리자가_추가한_종은_앱의_종_목록에_보이고_숨기면_빠진다() throws Exception {
        String admin = adminToken();
        Integer id = createSpecies(admin, "토케이 게코 " + System.nanoTime());

        mockMvc.perform(get("/api/v1/species"))
                .andExpect(jsonPath("$.data.species[*].id", hasItem(id)));

        mockMvc.perform(delete("/api/v1/admin/species/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/species"))
                .andExpect(jsonPath("$.data.species[*].id", not(hasItem(id))));
        mockMvc.perform(get("/api/v1/admin/species").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.species[?(@.id == " + id + ")].hidden").value(true));
    }

    @Test
    void 보낸_필드만_수정하고_hidden_false로_다시_보이게_한다() throws Exception {
        String admin = adminToken();
        Integer id = createSpecies(admin, "토케이 게코 " + System.nanoTime());
        mockMvc.perform(delete("/api/v1/admin/species/" + id).header(HttpHeaders.AUTHORIZATION, admin));

        mockMvc.perform(patch("/api/v1/admin/species/" + id).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"feedIntervalDays\": 6, \"hidden\": false }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedIntervalDays").value(6))
                .andExpect(jsonPath("$.data.shape").value("gecko"))
                .andExpect(jsonPath("$.data.envRange.humidity.min").value(60))
                .andExpect(jsonPath("$.data.hidden").value(false));
    }

    @Test
    void 이미_있는_이름이면_400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/species").header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("레오파드 게코")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 일반_사용자는_관리자_API를_쓸_수_없다() throws Exception {
        String keeper = "Bearer " + jwtProvider.createAccessToken(1L, Role.KEEPER);

        mockMvc.perform(get("/api/v1/admin/species").header(HttpHeaders.AUTHORIZATION, keeper))
                .andExpect(status().isForbidden());
    }

    private Integer createSpecies(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/admin/species").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.hidden").value(false))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.data.id");
        createdIds.add(id.longValue());
        return id;
    }

    private static String createBody(String name) {
        return """
                { "name": "%s", "shape": "GECKO", "feedIntervalDays": 5,
                  "humidityMin": 60, "humidityMax": 80, "defaultHome": "FOREST" }
                """.formatted(name);
    }

    private String adminToken() {
        return "Bearer " + jwtProvider.createAccessToken(1L, Role.ADMIN);
    }
}
