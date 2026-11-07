package com.tailfriends.health.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.health.domain.HealthCheckItem;
import com.tailfriends.health.domain.HealthCheckKey;
import com.tailfriends.health.domain.HealthCheckResult;
import com.tailfriends.photo.ai.HealthCheckAnalysis;
import com.tailfriends.photo.ai.HealthCheckAnalyzer;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Gemini 호출만 가짜로 바꾸고, 결과 저장(JSONB)·경고·앨범 요약·하루 횟수는 실제 DB로 확인한다.
 */
class HealthCheckIntegrationTest extends IntegrationTestSupport {

    private static final List<HealthCheckItem> ATTENTION = List.of(
            new HealthCheckItem(HealthCheckKey.RETAINED_SHED, HealthCheckResult.ATTENTION, "발가락에 허물이 남아 보여요"),
            new HealthCheckItem(HealthCheckKey.EYES, HealthCheckResult.GOOD, null),
            new HealthCheckItem(HealthCheckKey.TAIL, HealthCheckResult.GOOD, null),
            new HealthCheckItem(HealthCheckKey.BODY, HealthCheckResult.UNKNOWN, "몸 전체가 보이는 사진이 필요해요"),
            new HealthCheckItem(HealthCheckKey.SKIN, HealthCheckResult.GOOD, null));
    private static final List<HealthCheckItem> ALL_GOOD = EnumSet.allOf(HealthCheckKey.class).stream()
            .map(key -> new HealthCheckItem(key, HealthCheckResult.GOOD, null))
            .toList();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private HealthCheckAnalyzer healthCheckAnalyzer;

    @Test
    void 체크_결과를_저장하고_확인_필요면_경고를_열며_목록과_앨범에서_보인다() throws Exception {
        String token = token(saveUser());
        long animalId = createAnimal(token, 1);
        long first = upload(token, animalId, "HEALTH");
        given(healthCheckAnalyzer.analyze(any(), isNull(), eq(EnumSet.allOf(HealthCheckKey.class))))
                .willReturn(new HealthCheckAnalysis(true, ATTENTION));

        String body = check(token, first)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(5)))
                .andExpect(jsonPath("$.data.items[0].key").value("RETAINED_SHED"))
                .andExpect(jsonPath("$.data.items[0].result").value("ATTENTION"))
                .andExpect(jsonPath("$.data.items[0].note").value("발가락에 허물이 남아 보여요"))
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        Integer checkId = JsonPath.read(body, "$.data.checkId");

        mockMvc.perform(get("/api/v1/health/alerts").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", String.valueOf(animalId)))
                .andExpect(jsonPath("$.data.alerts", hasSize(1)))
                .andExpect(jsonPath("$.data.alerts[0].type").value("HEALTH_CHECK_ATTENTION"));

        // 두 번째 체크는 직전 체크 사진과 비교한다. 열린 경고가 있으면 새로 만들지 않는다.
        long second = upload(token, animalId, "HEALTH");
        given(healthCheckAnalyzer.analyze(any(), notNull(), any())).willReturn(new HealthCheckAnalysis(true, ATTENTION));
        check(token, second).andExpect(status().isOk());
        verify(healthCheckAnalyzer).analyze(any(), notNull(), any());
        mockMvc.perform(get("/api/v1/health/alerts").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", String.valueOf(animalId)))
                .andExpect(jsonPath("$.data.alerts", hasSize(1)));

        mockMvc.perform(get("/api/v1/health/checks").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", String.valueOf(animalId)).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checks", hasSize(1)))
                .andExpect(jsonPath("$.data.checks[0].photoId").value(second))
                .andExpect(jsonPath("$.data.checks[0].thumbnailUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.nextCursor").isNumber());

        mockMvc.perform(get("/api/v1/photos").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", String.valueOf(animalId)))
                .andExpect(jsonPath("$.data.photos[1].photoId").value(first))
                .andExpect(jsonPath("$.data.photos[1].healthCheck.checkId").value(checkId))
                .andExpect(jsonPath("$.data.photos[1].healthCheck.attentionCount").value(1));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT items -> 0 ->> 'key' FROM health_checks WHERE id = ?", String.class, checkId))
                .isEqualTo("RETAINED_SHED");
    }

    @Test
    void 게코가_아니면_꼬리를_보지_않고_모두_양호면_경고가_없다() throws Exception {
        String token = token(saveUser());
        long animalId = createAnimal(token, 4);
        long photoId = upload(token, animalId, "HEALTH");
        given(healthCheckAnalyzer.analyze(any(), isNull(),
                eq(EnumSet.complementOf(EnumSet.of(HealthCheckKey.TAIL)))))
                .willReturn(new HealthCheckAnalysis(true, ALL_GOOD.stream()
                        .filter(item -> item.key() != HealthCheckKey.TAIL).toList()));

        check(token, photoId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(4)));
        mockMvc.perform(get("/api/v1/health/alerts").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", String.valueOf(animalId)))
                .andExpect(jsonPath("$.data.alerts", hasSize(0)));
    }

    @Test
    void 파충류가_없으면_422_건강_체크용_사진이_아니면_400_남의_사진이면_404() throws Exception {
        String token = token(saveUser());
        long animalId = createAnimal(token, 1);
        given(healthCheckAnalyzer.analyze(any(), any(), any())).willReturn(HealthCheckAnalysis.notReptile());

        check(token, upload(token, animalId, "HEALTH"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("NO_REPTILE_IN_PHOTO"));
        check(token, upload(token, animalId, "ALBUM"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        check(token(saveUser()), upload(token, animalId, "HEALTH"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PHOTO_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/health/checks").header(HttpHeaders.AUTHORIZATION, token(saveUser()))
                        .param("animalId", String.valueOf(animalId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
    }

    @Test
    void 개체를_지우면_체크_결과도_지워진다() throws Exception {
        String token = token(saveUser());
        long animalId = createAnimal(token, 1);
        given(healthCheckAnalyzer.analyze(any(), any(), any())).willReturn(new HealthCheckAnalysis(true, ALL_GOOD));
        check(token, upload(token, animalId, "HEALTH")).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/animals/{id}", animalId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM health_checks WHERE animal_id = ?", Integer.class, animalId)).isZero();
    }

    private ResultActions check(String token, long photoId) throws Exception {
        return mockMvc.perform(post("/api/v1/photos/{id}/health-check", photoId)
                .header(HttpHeaders.AUTHORIZATION, token));
    }

    private long upload(String token, long animalId, String purpose) throws Exception {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB), "png", png);
        String response = mockMvc.perform(multipart("/api/v1/photos")
                        .file(new MockMultipartFile("file", "gecko.png", "image/png", png.toByteArray()))
                        .param("animalId", String.valueOf(animalId))
                        .param("purpose", purpose)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.photoId")).longValue();
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("망고집사")
                .build());
    }

    private String token(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }

    private long createAnimal(String token, int speciesId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": %d, \"feedIntervalDays\": 4 }"
                                .formatted(speciesId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }
}
