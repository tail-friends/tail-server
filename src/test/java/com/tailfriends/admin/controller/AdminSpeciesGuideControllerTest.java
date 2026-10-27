package com.tailfriends.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.admin.service.AdminSpeciesGuideService;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.species.domain.GuideSection;
import com.tailfriends.species.dto.SpeciesGuideResponse;
import com.tailfriends.user.domain.Role;
import com.tailfriends.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminSpeciesGuideController.class)
@WebMvcTestSupport
class AdminSpeciesGuideControllerTest {

    private static final String BODY = """
            { "sections": [ { "key": "FEED", "body": "귀뚜라미, 밀웜" } ] }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private AdminSpeciesGuideService adminSpeciesGuideService;

    @BeforeEach
    void setUp() {
        // @WithMockUserId의 기본 ID(1)를 관리자로 둔다. 일반 사용자는 다른 ID로 테스트한다
        given(userRepository.findRoleById(1L)).willReturn(Optional.of(Role.ADMIN));
    }

    @Test
    @WithMockUserId(role = "ADMIN")
    void 가이드를_저장한다() throws Exception {
        given(adminSpeciesGuideService.saveGuide(eq(1L), any())).willReturn(new SpeciesGuideResponse(1L, "레오파드 게코",
                List.of(new SpeciesGuideResponse.Section(GuideSection.FEED, "먹이", "귀뚜라미, 밀웜",
                        LocalDate.of(2026, 10, 21)))));

        mockMvc.perform(put("/api/v1/admin/species/1/guide").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sections[0].key").value("FEED"))
                .andExpect(jsonPath("$.data.sections[0].title").value("먹이"));
    }

    @Test
    @WithMockUserId(role = "ADMIN")
    void 내용이_비어_있으면_400() throws Exception {
        mockMvc.perform(put("/api/v1/admin/species/1/guide").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"sections\": [ { \"key\": \"FEED\", \"body\": \" \" } ] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId(role = "ADMIN")
    void 없는_섹션_키면_400() throws Exception {
        mockMvc.perform(put("/api/v1/admin/species/1/guide").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"sections\": [ { \"key\": \"SLEEP\", \"body\": \"낮잠\" } ] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId(2L)
    void 일반_사용자는_403() throws Exception {
        mockMvc.perform(put("/api/v1/admin/species/1/guide").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }
}
