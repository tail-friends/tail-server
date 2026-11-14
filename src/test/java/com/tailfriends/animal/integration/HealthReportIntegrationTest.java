package com.tailfriends.animal.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class HealthReportIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private Clock clock;

    @Test
    void 최근_기간의_체중_급여_탈피_진료_기록을_PDF로_내려준다() throws Exception {
        LocalDate today = KstDate.today(clock);
        String token = tokenOf(newUser());
        long mango = createAnimal(token);
        send(token, "/api/v1/logs", """
                { "animalId": %d, "type": "WEIGHT", "date": "%s", "value": 48.0 }""".formatted(mango, today.minusDays(40)));
        send(token, "/api/v1/logs", """
                { "animalId": %d, "type": "WEIGHT", "date": "%s", "value": 52.5 }""".formatted(mango, today.minusDays(3)));
        send(token, "/api/v1/logs", """
                { "animalId": %d, "type": "FEED", "date": "%s", "food": "귀뚜라미", "count": 5, "refused": true }"""
                .formatted(mango, today.minusDays(2)));
        send(token, "/api/v1/logs", """
                { "animalId": %d, "type": "SHED", "date": "%s", "stage": "SHED", "complete": true }"""
                .formatted(mango, today.minusDays(10)));
        send(token, "/api/v1/logs", """
                { "animalId": %d, "type": "WEIGHT", "date": "%s", "value": 30.0 }""".formatted(mango, today.minusMonths(5)));
        send(token, "/api/v1/health/vet-visits", """
                { "animalId": %d, "hospital": "제주 특수동물병원", "visitDate": "%s", "diagnosis": "탈피 부전" }"""
                .formatted(mango, today.minusDays(7)));

        byte[] pdf = mockMvc.perform(get("/api/v1/animals/" + mango + "/report")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();

        String text = text(pdf);
        assertThat(text).contains("망고", "처음 48.0g", "마지막 52.5g", "기록 2개");
        assertThat(text).contains("급여 기록 1번", "안 먹음 1번", "완탈 1번", "제주 특수동물병원", "탈피 부전");
        assertThat(text).doesNotContain("30.0g");
    }

    @Test
    void 남의_개체는_404이고_months가_범위_밖이면_400() throws Exception {
        String token = tokenOf(newUser());
        long stranger = createAnimal(tokenOf(newUser()));

        mockMvc.perform(get("/api/v1/animals/" + stranger + "/report").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/animals/" + createAnimal(token) + "/report").param("months", "13")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    private static String text(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }

    private void send(String token, String path, String body) throws Exception {
        mockMvc.perform(post(path)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private long createAnimal(String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": 1, \"feedIntervalDays\": 4 }"))
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
