package com.tailfriends.schedule.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.schedule.domain.RepeatType;
import com.tailfriends.schedule.domain.ScheduleKind;
import com.tailfriends.schedule.domain.Weekday;
import com.tailfriends.schedule.dto.ScheduleCompleteResponse;
import com.tailfriends.schedule.dto.ScheduleCreateResponse;
import com.tailfriends.schedule.dto.ScheduleListResponse;
import com.tailfriends.schedule.dto.ScheduleResponse;
import com.tailfriends.schedule.dto.ScheduleResponse.RepeatResponse;
import com.tailfriends.schedule.service.ScheduleService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ScheduleController.class)
@WebMvcTestSupport
class ScheduleControllerTest {

    private static final String CREATE_BODY = """
            { "animalId": 12, "name": "UVB 램프 교체", "kind": "EQUIPMENT",
              "repeat": { "type": "EVERY_N_DAYS", "n": 180 }, "startDate": "2026-10-18", "endDate": null }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleService scheduleService;

    @Test
    @WithMockUserId
    void 일정을_만들면_201과_ID_다음_일정일을_돌려준다() throws Exception {
        given(scheduleService.create(any())).willReturn(new ScheduleCreateResponse(42L, LocalDate.of(2026, 10, 18)));

        mockMvc.perform(post("/api/v1/schedules").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.nextDate").value("2026-10-18"));
    }

    @Test
    @WithMockUserId
    void 목록은_반복_종류에_해당하는_값만_내려준다() throws Exception {
        given(scheduleService.getSchedules(12L)).willReturn(new ScheduleListResponse(List.of(
                new ScheduleResponse(41L, 12L, "칼슘 더스팅", ScheduleKind.SUPPLEMENT,
                        new RepeatResponse(RepeatType.WEEKDAYS, null, List.of(Weekday.MON, Weekday.THU)),
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 16), null),
                new ScheduleResponse(42L, null, "UVB 램프 교체", ScheduleKind.EQUIPMENT,
                        new RepeatResponse(RepeatType.EVERY_N_DAYS, 180, null),
                        LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 18), null, null))));

        mockMvc.perform(get("/api/v1/schedules").param("animalId", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schedules[0].repeat.type").value("WEEKDAYS"))
                .andExpect(jsonPath("$.data.schedules[0].repeat.days[1]").value("THU"))
                .andExpect(jsonPath("$.data.schedules[0].repeat.n").doesNotExist())
                .andExpect(jsonPath("$.data.schedules[0].lastDoneDate").value("2026-10-16"))
                .andExpect(jsonPath("$.data.schedules[0].endDate").isEmpty())
                .andExpect(jsonPath("$.data.schedules[1].animalId").isEmpty())
                .andExpect(jsonPath("$.data.schedules[1].repeat.n").value(180))
                .andExpect(jsonPath("$.data.schedules[1].repeat.days").doesNotExist());
    }

    @Test
    @WithMockUserId
    void 이름이_비었거나_반복이_없으면_400() throws Exception {
        mockMvc.perform(post("/api/v1/schedules").contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY.replace("UVB 램프 교체", " ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        mockMvc.perform(post("/api/v1/schedules").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"더스팅\", \"kind\": \"SUPPLEMENT\", \"startDate\": \"2026-10-18\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId
    void n이_범위_밖이거나_요일_표기가_틀리면_400() throws Exception {
        mockMvc.perform(post("/api/v1/schedules").contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY.replace("\"n\": 180", "\"n\": 0")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/schedules").contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY.replace("{ \"type\": \"EVERY_N_DAYS\", \"n\": 180 }",
                                "{ \"type\": \"WEEKDAYS\", \"days\": [\"MONDAY\"] }")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId
    void 완료하면_마지막_완료일과_다음_일정일을_돌려준다() throws Exception {
        given(scheduleService.complete(eq(41L), any())).willReturn(
                new ScheduleCompleteResponse(LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 22)));

        mockMvc.perform(post("/api/v1/schedules/41/complete").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"date\": \"2026-10-18\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lastDoneDate").value("2026-10-18"))
                .andExpect(jsonPath("$.data.nextDate").value("2026-10-22"));
    }

    @Test
    @WithMockUserId
    void 완료일이_없으면_400() throws Exception {
        mockMvc.perform(post("/api/v1/schedules/41/complete").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserId
    void 삭제하면_본문_없이_204() throws Exception {
        mockMvc.perform(delete("/api/v1/schedules/41"))
                .andExpect(status().isNoContent());
        verify(scheduleService).delete(41L);
    }

    @Test
    @WithMockUserId
    void 없는_일정이면_404_SCHEDULE_NOT_FOUND() throws Exception {
        given(scheduleService.complete(eq(99L), any())).willThrow(new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        mockMvc.perform(post("/api/v1/schedules/99/complete").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"date\": \"2026-10-18\" }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_FOUND"));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/schedules"))
                .andExpect(status().isUnauthorized());
    }
}
