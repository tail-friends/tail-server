package com.tailfriends.photo.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Gemini API(generateContent)에 사진과 프롬프트를 보낸다. 답은 정해진 JSON 형식(responseSchema)으로 받는다.
 * 키는 URL이 아닌 헤더로 보내 로그에 남지 않게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    static final String API_KEY_HEADER = "x-goog-api-key";

    private final RestClient aiRestClient;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * @param responseSchema 답 JSON 형식
     * @param answerType     답을 읽을 타입. 형식 검사는 부르는 쪽에서 한다
     * @throws BusinessException 키가 없거나, 호출에 실패하거나, 답이 JSON이 아니면 AI_UNAVAILABLE
     */
    public <T> T generate(String prompt, Map<String, Object> responseSchema, byte[] jpeg, Class<T> answerType) {
        if (!StringUtils.hasText(properties.apiKey())) {
            log.warn("AI 키(AI_API_KEY)가 설정되지 않아 분석할 수 없어요");
            throw unavailable();
        }
        return parse(request(prompt, responseSchema, jpeg), answerType);
    }

    static BusinessException unavailable() {
        return new BusinessException(ErrorCode.AI_UNAVAILABLE);
    }

    private String request(String prompt, Map<String, Object> responseSchema, byte[] jpeg) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(
                        Map.of("text", prompt),
                        Map.of("inlineData", Map.of(
                                "mimeType", MediaType.IMAGE_JPEG_VALUE,
                                "data", Base64.getEncoder().encodeToString(jpeg)))))),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", MediaType.APPLICATION_JSON_VALUE,
                        "responseSchema", responseSchema));
        try {
            GenerateResponse response = aiRestClient.post()
                    .uri(BASE_URL + properties.model() + ":generateContent")
                    .header(API_KEY_HEADER, properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GenerateResponse.class);
            return response.text();
        } catch (RestClientException e) {
            // 무료 사용량을 다 쓴 경우(429)도 여기로 온다
            log.warn("Gemini 호출 실패: {}", e.getMessage());
            throw unavailable();
        } catch (RuntimeException e) {
            log.warn("Gemini 응답을 읽지 못했어요", e);
            throw unavailable();
        }
    }

    private <T> T parse(String text, Class<T> answerType) {
        try {
            return objectMapper.readValue(text, answerType);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("Gemini 답이 JSON이 아니에요: {}", text);
            throw unavailable();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateResponse(List<Candidate> candidates) {

        /**
         * 첫 후보의 첫 텍스트. 안전 필터 등으로 답이 없으면 예외.
         */
        String text() {
            if (candidates == null || candidates.isEmpty() || candidates.get(0).content() == null
                    || candidates.get(0).content().parts() == null
                    || candidates.get(0).content().parts().isEmpty()) {
                throw new IllegalStateException("Gemini 답이 비어 있어요");
            }
            return candidates.get(0).content().parts().get(0).text();
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Candidate(Content content) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Content(List<Part> parts) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Part(String text) {
        }
    }
}
