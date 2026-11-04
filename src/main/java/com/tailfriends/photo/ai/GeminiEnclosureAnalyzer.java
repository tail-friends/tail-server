package com.tailfriends.photo.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Gemini로 사육장 사진을 분석해 바닥·벽 색과 주요 장식을 읽는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiEnclosureAnalyzer implements EnclosureAnalyzer {

    static final int MAX_OBJECTS = 10;

    private static final Pattern COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final List<String> OBJECT_TYPES = Arrays.stream(EnclosureObjectType.values())
            .map(Enum::name)
            .toList();

    static final String PROMPT = """
            You read a photo of a pet reptile enclosure (terrarium, vivarium, tank) \
            to draw it as a simple 2D background.
            - isEnclosure: false if the photo is not a reptile enclosure. \
            Then fill the other fields with any valid value.
            - floorColor: main color of the substrate (sand, soil, paper, tiles) as #RRGGBB.
            - wallColor: main color of the back wall or background as #RRGGBB.
            - objects: main decorations, at most 10. type is ROCK, HIDE (cave or shelter), PLANT (real or fake), \
            BRANCH (wood, vines), WATER_DISH, or LAMP (heat or UV lamp). x, y are the center of the object \
            as a ratio of the photo width and height, 0.0 to 1.0, with (0, 0) at the top left.
            """;

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "isEnclosure", Map.of("type", "BOOLEAN"),
                    "floorColor", Map.of("type", "STRING"),
                    "wallColor", Map.of("type", "STRING"),
                    "objects", Map.of(
                            "type", "ARRAY",
                            "items", Map.of(
                                    "type", "OBJECT",
                                    "properties", Map.of(
                                            "type", Map.of("type", "STRING", "enum", OBJECT_TYPES),
                                            "x", Map.of("type", "NUMBER"),
                                            "y", Map.of("type", "NUMBER")),
                                    "required", List.of("type", "x", "y")))),
            "required", List.of("isEnclosure", "floorColor", "wallColor", "objects"));

    private final GeminiClient geminiClient;

    @Override
    public EnclosureAnalysis analyze(byte[] jpeg) {
        Answer answer = geminiClient.generate(PROMPT, RESPONSE_SCHEMA, jpeg, Answer.class);
        if (!Boolean.TRUE.equals(answer.isEnclosure())) {
            return EnclosureAnalysis.notEnclosure();
        }
        if (!isColor(answer.floorColor()) || !isColor(answer.wallColor())) {
            log.warn("Gemini 답이 형식에 맞지 않아요: {}", answer);
            throw GeminiClient.unavailable();
        }
        return new EnclosureAnalysis(
                true, upper(answer.floorColor()), upper(answer.wallColor()), objects(answer.objects()));
    }

    /**
     * 종류를 모르거나 위치가 없는 장식은 빼고 앞에서부터 10개까지.
     */
    private static List<EnclosureAnalysis.DetectedObject> objects(List<Answer.DetectedObject> objects) {
        if (objects == null) {
            return List.of();
        }
        return objects.stream()
                .filter(Objects::nonNull)
                .map(GeminiEnclosureAnalyzer::toObject)
                .filter(Objects::nonNull)
                .limit(MAX_OBJECTS)
                .toList();
    }

    private static EnclosureAnalysis.DetectedObject toObject(Answer.DetectedObject object) {
        EnclosureObjectType type = type(object.type());
        BigDecimal x = ratio(object.x());
        BigDecimal y = ratio(object.y());
        if (type == null || x == null || y == null) {
            return null;
        }
        return new EnclosureAnalysis.DetectedObject(type, x, y);
    }

    private static EnclosureObjectType type(String value) {
        if (value == null) {
            return null;
        }
        String name = value.trim().toUpperCase(Locale.ROOT);
        return OBJECT_TYPES.contains(name) ? EnclosureObjectType.valueOf(name) : null;
    }

    private static boolean isColor(String value) {
        return value != null && COLOR.matcher(value).matches();
    }

    private static String upper(String value) {
        return value.toUpperCase(Locale.ROOT);
    }

    /**
     * 0~1 밖이면 가까운 끝으로, 소수 셋째 자리까지. 값이 없으면 null.
     */
    private static BigDecimal ratio(Double value) {
        if (value == null || value.isNaN()) {
            return null;
        }
        return BigDecimal.valueOf(Math.min(1.0, Math.max(0.0, value))).setScale(3, RoundingMode.HALF_UP);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Answer(Boolean isEnclosure, String floorColor, String wallColor, List<DetectedObject> objects) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record DetectedObject(String type, Double x, Double y) {
        }
    }
}
