package com.tailfriends.photo.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Gemini로 사진을 분석해 캐릭터 값을 만든다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiCharacterAnalyzer implements CharacterAnalyzer {

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

    private final GeminiClient geminiClient;

    @Override
    public CharacterAnalysis analyze(byte[] jpeg) {
        Answer answer = geminiClient.generate(PROMPT, RESPONSE_SCHEMA, jpeg, Answer.class);
        if (!Boolean.TRUE.equals(answer.isReptile())) {
            return CharacterAnalysis.notReptile();
        }
        return toAnalysis(answer);
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
            throw GeminiClient.unavailable();
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
