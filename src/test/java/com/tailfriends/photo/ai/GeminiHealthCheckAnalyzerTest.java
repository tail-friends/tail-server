package com.tailfriends.photo.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailfriends.health.domain.HealthCheckItem;
import com.tailfriends.health.domain.HealthCheckKey;
import com.tailfriends.health.domain.HealthCheckResult;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiHealthCheckAnalyzerTest {

    private static final String URL = GeminiClient.BASE_URL + "gemini-test:generateContent";
    private static final byte[] JPEG = {1, 2, 3};
    private static final byte[] PREVIOUS = {4, 5, 6};
    private static final Set<HealthCheckKey> ALL = EnumSet.allOf(HealthCheckKey.class);

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final GeminiHealthCheckAnalyzer analyzer = new GeminiHealthCheckAnalyzer(
            new GeminiClient(builder.build(), new AiProperties("gemini-test", 5, "test-key", 10), objectMapper));

    @Test
    void 이번_사진과_이전_사진을_순서대로_보내고_항목을_정해진_순서로_돌려준다() {
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.contents[0].parts[0].text", Matchers.containsString("TAIL")))
                .andExpect(jsonPath("$.contents[0].parts[1].inlineData.data")
                        .value(Base64.getEncoder().encodeToString(JPEG)))
                .andExpect(jsonPath("$.contents[0].parts[2].inlineData.data")
                        .value(Base64.getEncoder().encodeToString(PREVIOUS)))
                .andRespond(withSuccess(answer("""
                        { "isReptile": true, "items": [
                          { "key": "SKIN", "result": "GOOD", "note": "" },
                          { "key": "RETAINED_SHED", "result": "attention", "note": "  발가락에 허물이 남아 보여요 " },
                          { "key": "EYES", "result": "GOOD", "note": "좋아요" },
                          { "key": "EYES", "result": "ATTENTION" },
                          { "key": "TAIL", "result": "GOOD" },
                          { "key": "BODY", "result": "UNKNOWN", "note": "몸 전체가 보이는 사진이 필요해요" } ] }
                        """), MediaType.APPLICATION_JSON));

        HealthCheckAnalysis analysis = analyzer.analyze(JPEG, PREVIOUS, ALL);

        assertThat(analysis.reptile()).isTrue();
        assertThat(analysis.items()).containsExactly(
                new HealthCheckItem(HealthCheckKey.RETAINED_SHED, HealthCheckResult.ATTENTION, "발가락에 허물이 남아 보여요"),
                new HealthCheckItem(HealthCheckKey.EYES, HealthCheckResult.GOOD, null),
                new HealthCheckItem(HealthCheckKey.TAIL, HealthCheckResult.GOOD, null),
                new HealthCheckItem(HealthCheckKey.BODY, HealthCheckResult.UNKNOWN, "몸 전체가 보이는 사진이 필요해요"),
                new HealthCheckItem(HealthCheckKey.SKIN, HealthCheckResult.GOOD, null));
        server.verify();
    }

    @Test
    void 이전_사진이_없으면_한_장만_보내고_요청하지_않은_항목은_빼고_빠진_항목은_UNKNOWN() {
        Set<HealthCheckKey> noTail = EnumSet.complementOf(EnumSet.of(HealthCheckKey.TAIL));
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.contents[0].parts.length()").value(2))
                .andRespond(withSuccess(answer("""
                        { "isReptile": true, "items": [
                          { "key": "TAIL", "result": "ATTENTION" },
                          { "key": "EYES", "result": "WEIRD" },
                          { "key": "SKIN", "result": "GOOD" } ] }
                        """), MediaType.APPLICATION_JSON));

        HealthCheckAnalysis analysis = analyzer.analyze(JPEG, null, noTail);

        assertThat(analysis.items()).extracting(HealthCheckItem::key)
                .containsExactly(HealthCheckKey.RETAINED_SHED, HealthCheckKey.EYES, HealthCheckKey.BODY,
                        HealthCheckKey.SKIN);
        assertThat(analysis.items()).extracting(HealthCheckItem::result)
                .containsExactly(HealthCheckResult.UNKNOWN, HealthCheckResult.UNKNOWN, HealthCheckResult.UNKNOWN,
                        HealthCheckResult.GOOD);
    }

    @Test
    void 파충류가_아니라고_답하면_reptile이_false() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess(answer("{ \"isReptile\": false, \"items\": [] }"), MediaType.APPLICATION_JSON));

        HealthCheckAnalysis analysis = analyzer.analyze(JPEG, null, ALL);

        assertThat(analysis.reptile()).isFalse();
        assertThat(analysis.items()).isEmpty();
    }

    @Test
    void 설명은_100자까지만_남긴다() {
        String longNote = "가".repeat(150);
        server.expect(requestTo(URL))
                .andRespond(withSuccess(answer("""
                        { "isReptile": true, "items": [ { "key": "SKIN", "result": "ATTENTION", "note": "%s" } ] }
                        """.formatted(longNote)), MediaType.APPLICATION_JSON));

        HealthCheckAnalysis analysis = analyzer.analyze(JPEG, null, EnumSet.of(HealthCheckKey.SKIN));

        assertThat(analysis.items().getFirst().note()).hasSize(GeminiHealthCheckAnalyzer.MAX_NOTE_LENGTH);
    }

    /**
     * Gemini generateContent 응답. 답(JSON)은 문자열로 text에 들어 있다.
     */
    private String answer(String json) {
        try {
            return """
                    { "candidates": [ { "content": { "role": "model", "parts": [ { "text": %s } ] } } ] }
                    """.formatted(objectMapper.writeValueAsString(json));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
