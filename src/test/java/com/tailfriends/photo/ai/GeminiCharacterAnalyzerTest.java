package com.tailfriends.photo.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.Base64;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiCharacterAnalyzerTest {

    private static final String URL = GeminiCharacterAnalyzer.BASE_URL + "gemini-test:generateContent";
    private static final byte[] JPEG = {1, 2, 3};

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private GeminiCharacterAnalyzer analyzer(String apiKey) {
        return new GeminiCharacterAnalyzer(builder.build(), new AiProperties("gemini-test", 5, apiKey), objectMapper);
    }

    @Test
    void 사진과_프롬프트를_보내고_답을_캐릭터_값으로_바꾼다() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(GeminiCharacterAnalyzer.API_KEY_HEADER, "test-key"))
                .andExpect(jsonPath("$.contents[0].parts[1].inlineData.mimeType").value("image/jpeg"))
                .andExpect(jsonPath("$.contents[0].parts[1].inlineData.data")
                        .value(Base64.getEncoder().encodeToString(JPEG)))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andRespond(withSuccess(answer("""
                        { "isReptile": true, "shape": "GECKO", "patternType": "spots",
                          "colors": { "body": "#e0a33a", "pattern": "#3A2A1E", "belly": "#F2E6C8", "eye": "#2b2b2b" },
                          "extra": { "headSize": 0.6, "tailWidth": 1.3, "patternDensity": 0.456 },
                          "guess": " 탠저린 레오파드 게코 " }
                        """), MediaType.APPLICATION_JSON));

        CharacterAnalysis analysis = analyzer("test-key").analyze(JPEG);

        assertThat(analysis.reptile()).isTrue();
        assertThat(analysis.shape()).isEqualTo("gecko");
        assertThat(analysis.patternType()).isEqualTo("spots");
        assertThat(analysis.colors()).isEqualTo(new CharacterAnalysis.Colors("#E0A33A", "#3A2A1E", "#F2E6C8", "#2B2B2B"));
        assertThat(analysis.extra()).isEqualTo(new CharacterAnalysis.Extra(
                new BigDecimal("0.60"), new BigDecimal("1.00"), new BigDecimal("0.46")));
        assertThat(analysis.guess()).isEqualTo("탠저린 레오파드 게코");
        server.verify();
    }

    @Test
    void 파충류가_없다고_답하면_reptile이_false() {
        server.expect(requestTo(URL)).andRespond(withSuccess(answer("""
                { "isReptile": false, "shape": "gecko", "patternType": "none",
                  "colors": { "body": "#000000", "pattern": "#000000", "belly": "#000000", "eye": "#000000" },
                  "extra": { "headSize": 0, "tailWidth": 0, "patternDensity": 0 }, "guess": "" }
                """), MediaType.APPLICATION_JSON));

        assertThat(analyzer("test-key").analyze(JPEG).reptile()).isFalse();
    }

    @Test
    void 색이_형식에_맞지_않으면_AI_UNAVAILABLE() {
        server.expect(requestTo(URL)).andRespond(withSuccess(answer("""
                { "isReptile": true, "shape": "gecko", "patternType": "spots",
                  "colors": { "body": "orange", "pattern": "#3A2A1E", "belly": "#F2E6C8", "eye": "#2B2B2B" },
                  "extra": { "headSize": 0.5, "tailWidth": 0.5, "patternDensity": 0.5 }, "guess": "게코" }
                """), MediaType.APPLICATION_JSON));

        assertUnavailable(() -> analyzer("test-key").analyze(JPEG));
    }

    @Test
    void 무료_사용량을_다_써서_429가_오면_AI_UNAVAILABLE() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertUnavailable(() -> analyzer("test-key").analyze(JPEG));
    }

    @Test
    void 답이_비어_있으면_AI_UNAVAILABLE() {
        server.expect(requestTo(URL)).andRespond(withSuccess("{ \"candidates\": [] }", MediaType.APPLICATION_JSON));

        assertUnavailable(() -> analyzer("test-key").analyze(JPEG));
    }

    @Test
    void 키가_없으면_호출하지_않고_AI_UNAVAILABLE() {
        assertUnavailable(() -> analyzer("").analyze(JPEG));
        server.verify();
    }

    private static void assertUnavailable(ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AI_UNAVAILABLE);
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
