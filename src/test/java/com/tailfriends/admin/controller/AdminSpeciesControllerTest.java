package com.tailfriends.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.admin.dto.AdminSpeciesResponse;
import com.tailfriends.admin.service.AdminSpeciesService;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.species.domain.Home;
import com.tailfriends.species.domain.Shape;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminSpeciesController.class)
@WebMvcTestSupport
class AdminSpeciesControllerTest {

    private static final String CREATE_BODY = """
            { "name": "토케이 게코", "shape": "gecko", "feedIntervalDays": 5,
              "humidityMin": 60, "humidityMax": 80, "defaultHome": "forest" }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminSpeciesService adminSpeciesService;

    @Test
    @WithMockUserId(role = "ADMIN")
    void 관리자는_소문자_값으로도_종을_추가할_수_있다() throws Exception {
        given(adminSpeciesService.create(any())).willReturn(new AdminSpeciesResponse(11L, "토케이 게코", "gecko",
                false, 5, null, null, "forest", false, "reptile", null, null, List.of(), 1000));

        mockMvc.perform(post("/api/v1/admin/species").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.hidden").value(false));
        verify(adminSpeciesService).create(argThat(request ->
                request.shape() == Shape.GECKO && request.defaultHome() == Home.FOREST));
    }

    @Test
    @WithMockUserId
    void 일반_사용자는_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/species"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mockMvc.perform(post("/api/v1/admin/species").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/species"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUserId(role = "ADMIN")
    void 급여_주기가_범위를_벗어나면_400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/species").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"토케이 게코\", \"shape\": \"GECKO\", \"feedIntervalDays\": 0, \"defaultHome\": \"FOREST\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId(role = "ADMIN")
    void 종을_숨기면_본문_없이_204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/species/3"))
                .andExpect(status().isNoContent());
        verify(adminSpeciesService).hide(3L);
    }
}
