package com.tailfriends.photo.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Gemini API(generateContent)로 사진을 분석한다. 답은 정해진 JSON 형식(responseSchema)으로 받는다.
 * 키는 URL이 아닌 헤더로 보내 로그에 남지 않게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiCharacterAnalyzer implements CharacterAnalyzer {

    static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    static final String API_KEY_HEADER = "x-goog-api-key";

    private static final Set<String> SHAPES = Set.of("gecko", "lizard", "snake", "turtle");
    private static final Set<String> PATTERN_TYPES = Set.of("spots", "bands", "stripes", "none");
    private static final Pattern COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final int GUESS_MAX_LENGTH = 40;

    static final String PROMPT = """
            You turn a photo of a pet reptile into values for a simple 2D cartoon character.
            - isReptile: false if there is no reptile in the photo. Then fill the other fields with any valid value.
            - shape: body type. gecko (leopard gecko, crested gecko), lizard (bearded dragon, iguana, monitor), \
            snake, turtle (turtles, tortoises).
            - patternType: main pattern on the back. spots, bands, stripes, or none.
            - colors: #RRGGBB. body = main body color, pattern = pattern color (if none, a slightly darker body color), \
            belly = belly or underside, eye = eye color.
            - extra: 0.0 to 1.0. headSize = head size relative to body, tailWidth = tail thickness, \
            patternDensity = how much of the body the pattern covers.
            - guess: the species and morph in Korean, short. e.g. "탠저린 레오파드 게코". Best guess if unsure.
            """;

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "isReptile", Map.of("type", "BOOLEAN"),
                    "shape", Map.of("type", "STRING", "enum", List.copyOf(SHAPES)),
                    "patternType", Map.of("type", "STRING", "enum", List.copyOf(PATTERN_TYPES)),
                    "colors", object(List.of("body", "pattern", "belly", "eye"), "STRING"),
                    "extra", object(List.of("headSize", "tailWidth", "patternDensity"), "NUMBER"),
                    "guess", Map.of("type", "STRING")),
            "required", List.of("isReptile", "shape", "patternType", "colors", "extra", "guess"));

    private final RestClient aiRestClient;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public CharacterAnalysis analyze(byte[] jpeg) {
        if (!StringUtils.hasText(properties.apiKey())) {
            log.warn("AI 키(AI_API_KEY)가 설정되지 않아 분석할 수 없어요");
            throw unavailable();
        }
        Answer answer = parse(request(jpeg));
        if (!Boolean.TRUE.equals(answer.isReptile())) {
            return CharacterAnalysis.notReptile();
        }
        return toAnalysis(answer);
    }

    private String request(byte[] jpeg) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(
                        Map.of("text", PROMPT),
                        Map.of("inlineData", Map.of(
                                "mimeType", MediaType.IMAGE_JPEG_VALUE,
                                "data", Base64.getEncoder().encodeToString(jpeg)))))),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", MediaType.APPLICATION_JSON_VALUE,
                        "responseSchema", RESPONSE_SCHEMA));
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

    private Answer parse(String text) {
        try {
            return objectMapper.readValue(text, Answer.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("Gemini 답이 JSON이 아니에요: {}", text);
            throw unavailable();
        }
    }

    private CharacterAnalysis toAnalysis(Answer answer) {
        String shape = lower(answer.shape());
        String patternType = lower(answer.patternType());
        Answer.Colors colors = answer.colors();
        Answer.Extra extra = answer.extra();
        if (!SHAPES.contains(shape) || !PATTERN_TYPES.contains(patternType) || colors == null || extra == null
                || !isColor(colors.body()) || !isColor(colors.pattern())
                || !isColor(colors.belly()) || !isColor(colors.eye())) {
            log.warn("Gemini 답이 형식에 맞지 않아요: {}", answer);
            throw unavailable();
        }
        return new CharacterAnalysis(
                true,
                shape,
                patternType,
                new CharacterAnalysis.Colors(
                        upper(colors.body()), upper(colors.pattern()), upper(colors.belly()), upper(colors.eye())),
                new CharacterAnalysis.Extra(
                        ratio(extra.headSize()), ratio(extra.tailWidth()), ratio(extra.patternDensity())),
                guess(answer.guess()));
    }

    private static Map<String, Object> object(List<String> fields, String type) {
        return Map.of(
                "type", "OBJECT",
                "properties", fields.stream().collect(Collectors.toMap(
                        field -> field, field -> Map.of("type", type))),
                "required", fields);
    }

    private static boolean isColor(String value) {
        return value != null && COLOR.matcher(value).matches();
    }

    private static String lower(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String upper(String value) {
        return value.toUpperCase(Locale.ROOT);
    }

    /**
     * 0~1 밖이면 가까운 끝으로, 소수 둘째 자리까지. 값이 없으면 null(앱 기본값).
     */
    private static BigDecimal ratio(Double value) {
        if (value == null || value.isNaN()) {
            return null;
        }
        return BigDecimal.valueOf(Math.min(1.0, Math.max(0.0, value))).setScale(2, RoundingMode.HALF_UP);
    }

    private static String guess(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > GUESS_MAX_LENGTH ? trimmed.substring(0, GUESS_MAX_LENGTH) : trimmed;
    }

    private static BusinessException unavailable() {
        return new BusinessException(ErrorCode.AI_UNAVAILABLE);
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Answer(Boolean isReptile, String shape, String patternType, Colors colors, Extra extra, String guess) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Colors(String body, String pattern, String belly, String eye) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Extra(Double headSize, Double tailWidth, Double patternDensity) {
        }
    }
}
