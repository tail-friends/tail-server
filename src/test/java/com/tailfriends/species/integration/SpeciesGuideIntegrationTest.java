package com.tailfriends.species.integration;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.AdminTokens;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.user.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class SpeciesGuideIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 관리자가_저장한_가이드를_로그인_없이_조회하고_다시_저장하면_빠진_섹션이_지워진다() throws Exception {
        mockMvc.perform(get("/api/v1/species/2/guide"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sections", hasSize(0)));

        save(2, """
                { "sections": [
                  { "key": "HEALTH", "body": "탈피 부전 주의" },
                  { "key": "HABIT", "body": "야행성" },
                  { "key": "FEED", "body": "귀뚜라미" } ] }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.speciesId").value(2))
                .andExpect(jsonPath("$.data.sections[*].key").value(contains("HABIT", "FEED", "HEALTH")))
                .andExpect(jsonPath("$.data.sections[0].updatedAt").value(LocalDate.now(KstDate.ZONE).toString()));

        save(2, """
                { "sections": [
                  { "key": "HABIT", "body": "야행성, 온순함" },
                  { "key": "ENV", "body": "핫존 32도" } ] }
                """)
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/species/2/guide"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sections", hasSize(2)))
                .andExpect(jsonPath("$.data.sections[0].key").value("HABIT"))
                .andExpect(jsonPath("$.data.sections[0].title").value("습성"))
                .andExpect(jsonPath("$.data.sections[0].body").value("야행성, 온순함"))
                .andExpect(jsonPath("$.data.sections[1].key").value("ENV"));

        mockMvc.perform(get("/api/v1/species/2/guide").param("section", "ENV"))
                .andExpect(jsonPath("$.data.sections", hasSize(1)))
                .andExpect(jsonPath("$.data.sections[0].body").value("핫존 32도"));
    }

    @Test
    void 같은_섹션을_두_번_보내면_400() throws Exception {
        save(3, """
                { "sections": [ { "key": "FEED", "body": "귀뚜라미" }, { "key": "FEED", "body": "밀웜" } ] }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 없는_종이면_404() throws Exception {
        save(9999, "{ \"sections\": [] }")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SPECIES_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/species/9999/guide"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SPECIES_NOT_FOUND"));
    }

    private ResultActions save(long speciesId, String body) throws Exception {
        String admin = AdminTokens.newAdmin(userRepository, jwtProvider);
        return mockMvc.perform(put("/api/v1/admin/species/{id}/guide", speciesId)
                .header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
