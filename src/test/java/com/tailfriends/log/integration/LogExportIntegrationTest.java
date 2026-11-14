package com.tailfriends.log.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class LogExportIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private Clock clock;

    @Test
    void 내_기록만_날짜순으로_내보내고_개체와_기간으로_거른다() throws Exception {
        LocalDate today = KstDate.today(clock);
        String token = tokenOf(newUser());
        String other = tokenOf(newUser());
        long mango = createAnimal(token, "망고");
        long choco = createAnimal(token, "초코");
        long stranger = createAnimal(other, "남의 개체");
        writeLog(token, """
                { "animalId": %d, "type": "WEIGHT", "date": "%s", "value": 57.8 }""".formatted(mango, today));
        writeLog(token, """
                { "animalId": %d, "type": "FEED", "date": "%s", "food": "귀뚜라미, 밀웜", "count": 6 }"""
                .formatted(choco, today.minusDays(2)));
        writeLog(token, """
                { "animalId": %d, "type": "NOTE", "date": "%s", "memo": "=1+1" }"""
                .formatted(mango, today.minusDays(5)));
        writeLog(other, """
                { "animalId": %d, "type": "NOTE", "date": "%s", "memo": "남의 기록" }""".formatted(stranger, today));

        List<String> all = export(token, get("/api/v1/users/me/export"));
        assertThat(all).containsExactly(
                "개체명,종,날짜,종류,먹이,개수,안 먹음,체중(g),탈피 상태,온도(℃),습도(%),메모",
                "망고,레오파드 게코,%s,메모,,,,,,,,'=1+1".formatted(today.minusDays(5)),
                "초코,레오파드 게코,%s,급여,\"귀뚜라미, 밀웜\",6,아니오,,,,,".formatted(today.minusDays(2)),
                "망고,레오파드 게코,%s,체중,,,,57.8,,,,".formatted(today));

        assertThat(export(token, get("/api/v1/users/me/export").param("animalId", String.valueOf(mango))))
                .hasSize(3).noneMatch(line -> line.startsWith("초코"));
        assertThat(export(token, get("/api/v1/users/me/export")
                .param("from", today.minusDays(2).toString()).param("to", today.minusDays(2).toString())))
                .hasSize(2).last().asString().startsWith("초코");
        assertThat(export(token, get("/api/v1/users/me/export").param("to", today.minusDays(3).toString())))
                .hasSize(2).last().asString().contains("'=1+1");
    }

    @Test
    void 남의_개체나_거꾸로_된_기간은_JSON_에러다() throws Exception {
        String token = tokenOf(newUser());
        long stranger = createAnimal(tokenOf(newUser()), "남의 개체");

        mockMvc.perform(get("/api/v1/users/me/export").param("animalId", String.valueOf(stranger))
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/users/me/export").param("from", "2026-10-02").param("to", "2026-10-01")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 기록이_없으면_머리줄만_있다() throws Exception {
        assertThat(export(tokenOf(newUser()), get("/api/v1/users/me/export"))).hasSize(1);
    }

    private List<String> export(String token, MockHttpServletRequestBuilder request) throws Exception {
        byte[] body = mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(Arrays.copyOf(body, 3)).containsExactly(0xEF, 0xBB, 0xBF);
        return List.of(new String(body, 3, body.length - 3, StandardCharsets.UTF_8).split("\r\n"));
    }

    private void writeLog(String token, String body) throws Exception {
        mockMvc.perform(post("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private long createAnimal(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"%s\", \"speciesId\": 1, \"feedIntervalDays\": 4 }".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private User newUser() {
        return userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("사용자")
                .build());
    }

    private String tokenOf(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
