package com.tailfriends.photo.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiEnclosureAnalyzerTest {

    private static final String URL = GeminiClient.BASE_URL + "gemini-test:generateContent";
    private static final byte[] JPEG = {1, 2, 3};

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final GeminiEnclosureAnalyzer analyzer = new GeminiEnclosureAnalyzer(
            new GeminiClient(builder.build(), new AiProperties("gemini-test", 5, "test-key", 10), objectMapper));

    @Test
    void 사진과_사육장_프롬프트를_보내고_답을_색과_장식으로_바꾼다() {
        server.expect(requestTo(URL))
                .andExpect(header(GeminiClient.API_KEY_HEADER, "test-key"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value(GeminiEnclosureAnalyzer.PROMPT))
                .andExpect(jsonPath("$.contents[0].parts[1].inlineData.data")
                        .value(Base64.getEncoder().encodeToString(JPEG)))
                .andExpect(jsonPath("$.generationConfig.responseSchema.properties.objects.type").value("ARRAY"))
                .andRespond(withSuccess(answer("""
                        { "isEnclosure": true, "floorColor": "#c9a26b", "wallColor": "#5B4A3A",
                          "objects": [ { "type": "ROCK", "x": 0.7, "y": 0.8 },
                                       { "type": "water_dish", "x": 1.4, "y": 0.12345 } ] }
                        """), MediaType.APPLICATION_JSON));

        EnclosureAnalysis analysis = analyzer.analyze(JPEG);

        assertThat(analysis.enclosure()).isTrue();
        assertThat(analysis.floorColor()).isEqualTo("#C9A26B");
        assertThat(analysis.wallColor()).isEqualTo("#5B4A3A");
        assertThat(analysis.objects()).containsExactly(
                new EnclosureAnalysis.DetectedObject(EnclosureObjectType.ROCK,
                        new BigDecimal("0.700"), new BigDecimal("0.800")),
                new EnclosureAnalysis.DetectedObject(EnclosureObjectType.WATER_DISH,
                        new BigDecimal("1.000"), new BigDecimal("0.123")));
        server.verify();
    }

    @Test
    void 사육장이_아니라고_답하면_enclosure가_false() {
        server.expect(requestTo(URL)).andRespond(withSuccess(answer("""
                { "isEnclosure": false, "floorColor": "#000000", "wallColor": "#000000", "objects": [] }
                """), MediaType.APPLICATION_JSON));

        assertThat(analyzer.analyze(JPEG).enclosure()).isFalse();
    }

    @Test
    void 모르는_종류나_위치가_없는_장식은_빼고_10개까지만() {
        StringBuilder objects = new StringBuilder("""
                { "type": "CAT", "x": 0.5, "y": 0.5 }, { "type": "HIDE", "x": null, "y": 0.5 }, null""");
        for (int i = 0; i < 12; i++) {
            objects.append(", { \"type\": \"PLANT\", \"x\": 0.%d, \"y\": 0.5 }".formatted(i));
        }
        server.expect(requestTo(URL)).andRespond(withSuccess(answer("""
                { "isEnclosure": true, "floorColor": "#C9A26B", "wallColor": "#5B4A3A", "objects": [ %s ] }
                """.formatted(objects)), MediaType.APPLICATION_JSON));

        EnclosureAnalysis analysis = analyzer.analyze(JPEG);

        assertThat(analysis.objects()).hasSize(GeminiEnclosureAnalyzer.MAX_OBJECTS)
                .allMatch(object -> object.type() == EnclosureObjectType.PLANT);
        assertThat(analysis.objects().get(0).x()).isEqualByComparingTo("0");
    }

    @Test
    void 장식이_없으면_빈_배열() {
        server.expect(requestTo(URL)).andRespond(withSuccess(answer("""
                { "isEnclosure": true, "floorColor": "#C9A26B", "wallColor": "#5B4A3A" }
                """), MediaType.APPLICATION_JSON));

        assertThat(analyzer.analyze(JPEG).objects()).isEmpty();
    }

    @Test
    void 색이_형식에_맞지_않으면_AI_UNAVAILABLE() {
        server.expect(requestTo(URL)).andRespond(withSuccess(answer("""
                { "isEnclosure": true, "floorColor": "sand", "wallColor": "#5B4A3A", "objects": [] }
                """), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> analyzer.analyze(JPEG))
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
