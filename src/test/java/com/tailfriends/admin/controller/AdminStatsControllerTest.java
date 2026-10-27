package com.tailfriends.admin.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.admin.dto.AdminStatsResponse;
import com.tailfriends.admin.dto.AdminStatsResponse.Daily;
import com.tailfriends.admin.service.AdminStatsService;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.user.domain.Role;
import com.tailfriends.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminStatsController.class)
@WebMvcTestSupport
class AdminStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private AdminStatsService adminStatsService;

    @BeforeEach
    void setUp() {
        // @WithMockUserId의 기본 ID(1)를 관리자로 둔다. 일반 사용자는 다른 ID로 테스트한다
        given(userRepository.findRoleById(1L)).willReturn(Optional.of(Role.ADMIN));
    }

    @Test
    @WithMockUserId(role = "ADMIN")
    void 기본은_최근_7일() throws Exception {
        given(adminStatsService.getStats(7)).willReturn(new AdminStatsResponse(120, 340,
                List.of(new Daily(LocalDate.of(2026, 10, 20), 1, 2, 10, 1))));

        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(120))
                .andExpect(jsonPath("$.data.totalAnimals").value(340))
                .andExpect(jsonPath("$.data.daily[0].date").value("2026-10-20"))
                .andExpect(jsonPath("$.data.daily[0].newAnimals").value(2))
                .andExpect(jsonPath("$.data.daily[0].aiAnalyses").value(10))
                .andExpect(jsonPath("$.data.daily[0].aiFailures").value(1));
    }

    @Test
    @WithMockUserId(role = "ADMIN")
    void days가_범위를_벗어나면_400() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats").param("days", "91"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId(2L)
    void 일반_사용자는_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUserId(value = 3L, role = "ADMIN")
    void 토큰은_ADMIN이어도_DB_역할이_KEEPER면_403() throws Exception {
        given(userRepository.findRoleById(3L)).willReturn(Optional.of(Role.KEEPER));

        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
