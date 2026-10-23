package com.tailfriends.animal.integration;

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
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class FeedIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.now(KstDate.ZONE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 급여_기록이_없으면_NO_RECORD() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token, 4);

        mockMvc.perform(get("/api/v1/animals/" + animalId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.feed.status").value("NO_RECORD"))
                .andExpect(jsonPath("$.data.feed.nextDate").isEmpty());
    }

    @Test
    void 안_먹음_기록은_빼고_마지막_급여일로_계산한다() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token, 4);
        feed(token, animalId, TODAY.minusDays(6), false);
        feed(token, animalId, TODAY.minusDays(2), true);

        mockMvc.perform(get("/api/v1/animals/" + animalId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feed.status").value("OVERDUE"))
                .andExpect(jsonPath("$.data.feed.lastDate").value(TODAY.minusDays(6).toString()))
                .andExpect(jsonPath("$.data.feed.nextDate").value(TODAY.minusDays(2).toString()))
                .andExpect(jsonPath("$.data.feed.daysLeft").value(-2));
    }

    @Test
    void 급여를_기록하면_작성_응답에_새_급여_상태가_온다() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token, 4);

        feed(token, animalId, TODAY, false)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.feed.status").value("UPCOMING"))
                .andExpect(jsonPath("$.data.feed.nextDate").value(TODAY.plusDays(4).toString()))
                .andExpect(jsonPath("$.data.feed.daysLeft").value(4));
    }

    @Test
    void 개체_목록에_개체별_급여_상태가_온다() throws Exception {
        String token = tokenOfNewUser();
        Integer due = createAnimal(token, 3);
        Integer noRecord = createAnimal(token, 3);
        feed(token, due, TODAY.minusDays(3), false);

        mockMvc.perform(get("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.animals[0].id").value(due))
                .andExpect(jsonPath("$.data.animals[0].feed.status").value("DUE"))
                .andExpect(jsonPath("$.data.animals[0].feed.daysLeft").value(0))
                .andExpect(jsonPath("$.data.animals[1].id").value(noRecord))
                .andExpect(jsonPath("$.data.animals[1].feed.status").value("NO_RECORD"));
    }

    private ResultActions feed(String token, Integer animalId, LocalDate date, boolean refused) throws Exception {
        return mockMvc.perform(post("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "animalId": %d, "type": "FEED", "date": "%s", "food": "귀뚜라미", "count": 5, "refused": %s }
                        """.formatted(animalId, date, refused)));
    }

    private Integer createAnimal(String token, int feedIntervalDays) throws Exception {
        String body = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": 1, \"feedIntervalDays\": %d }"
                                .formatted(feedIntervalDays)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.id");
    }

    private String tokenOfNewUser() {
        User user = userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("사용자")
                .build());
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
