package com.tailfriends.admin.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.AdminTokens;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.photo.domain.AiKind;
import com.tailfriends.photo.domain.AiUsage;
import com.tailfriends.photo.repository.AiUsageRepository;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 컨테이너 DB를 다른 통합 테스트와 함께 쓰므로 절대값 대신 늘어난 만큼을 확인한다.
 */
class AdminStatsIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private AiUsageRepository aiUsageRepository;

    private String admin;

    // 관리자도 가입자로 세므로, 처음 통계를 읽기 전에 한 번만 만든다
    @BeforeEach
    void setUp() {
        admin = AdminTokens.newAdmin(userRepository, jwtProvider);
    }

    @Test
    void 가입자와_개체가_늘면_전체_수와_오늘_수가_그만큼_는다() throws Exception {
        String before = stats();

        User user = saveUser();
        saveUser();
        String token = "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
        mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": 1, \"feedIntervalDays\": 4 }"))
                .andExpect(status().isCreated());

        String after = stats();
        assertThat(number(after, "$.data.totalUsers") - number(before, "$.data.totalUsers")).isEqualTo(2);
        assertThat(number(after, "$.data.totalAnimals") - number(before, "$.data.totalAnimals")).isEqualTo(1);
        assertThat(today(after, "newUsers") - today(before, "newUsers")).isEqualTo(2);
        assertThat(today(after, "newAnimals") - today(before, "newAnimals")).isEqualTo(1);
        List<String> dates = JsonPath.read(after, "$.data.daily[*].date");
        assertThat(dates).hasSize(7).last().isEqualTo(LocalDate.now(KstDate.ZONE).toString());
    }

    @Test
    void 종별_등록_개체_수는_직접_입력한_종을_하나로_묶고_합이_전체_개체_수와_같다() throws Exception {
        String before = stats();

        User user = saveUser();
        String token = "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
        createAnimal(token, "{ \"name\": \"망고\", \"speciesId\": 1, \"feedIntervalDays\": 4 }");
        createAnimal(token, "{ \"name\": \"콩이\", \"speciesName\": \"사바나 모니터\", \"feedIntervalDays\": 3 }");
        createAnimal(token, "{ \"name\": \"밤이\", \"speciesName\": \"그린 이구아나\", \"feedIntervalDays\": 2 }");

        String after = stats();
        assertThat(speciesCount(after, "1") - speciesCount(before, "1")).isEqualTo(1);
        assertThat(speciesCount(after, null) - speciesCount(before, null)).isEqualTo(2);
        List<String> customNames = JsonPath.read(after, "$.data.speciesCounts[?(@.speciesId == null)].name");
        assertThat(customNames).containsExactly("직접 입력");
        List<Number> counts = JsonPath.read(after, "$.data.speciesCounts[*].count");
        assertThat(counts.stream().mapToLong(Number::longValue).sum()).isEqualTo(number(after, "$.data.totalAnimals"));
    }

    @Test
    void AI_분석을_하면_오늘_분석_수와_실패_수가_는다() throws Exception {
        String before = stats();

        User user = saveUser();
        aiUsageRepository.save(AiUsage.of(user, AiKind.CHARACTER, true));
        aiUsageRepository.save(AiUsage.of(user, AiKind.CHARACTER, true));
        aiUsageRepository.save(AiUsage.of(user, AiKind.CHARACTER, false));

        String after = stats();
        assertThat(today(after, "aiAnalyses") - today(before, "aiAnalyses")).isEqualTo(3);
        assertThat(today(after, "aiFailures") - today(before, "aiFailures")).isEqualTo(1);
    }

    private String stats() throws Exception {
        return mockMvc.perform(get("/api/v1/admin/stats").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private void createAnimal(String token, String content) throws Exception {
        mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(content))
                .andExpect(status().isCreated());
    }

    /**
     * speciesId가 null이면 직접 입력한 종 묶음. 항목이 없으면 0.
     */
    private static long speciesCount(String json, String speciesId) {
        String filter = speciesId == null ? "@.speciesId == null" : "@.speciesId == " + speciesId;
        List<Number> counts = JsonPath.read(json, "$.data.speciesCounts[?(" + filter + ")].count");
        return counts.isEmpty() ? 0 : counts.getFirst().longValue();
    }

    private static long number(String json, String path) {
        return ((Number) JsonPath.read(json, path)).longValue();
    }

    private static long today(String json, String field) {
        return number(json, "$.data.daily[-1]." + field);
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("사용자")
                .build());
    }
}
