package com.tailfriends.photo.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tailfriends.health.domain.HealthCheckItem;
import com.tailfriends.health.domain.HealthCheckKey;
import com.tailfriends.health.domain.HealthCheckResult;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Gemini로 파충류 사진의 겉보기 건강 상태를 항목별로 판정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiHealthCheckAnalyzer implements HealthCheckAnalyzer {

    static final int MAX_NOTE_LENGTH = 100;

    static final String PROMPT = """
            You check the visible condition of a pet reptile in the first photo for its keeper. \
            You are not a vet: never name a disease or suggest a treatment.
            - isReptile: false if the first photo does not show a reptile. Then return an empty items array.
            - items: one entry for each of these keys: %s.
              RETAINED_SHED: stuck old skin on toes, tail tip, or around the eyes.
              EYES: clear and open, not sunken, cloudy, or swollen.
              TAIL: tail thickness compared with the second photo (the same animal at an earlier check). \
            If there is no second photo, judge whether the tail looks thin for the body.
              BODY: overall body condition, not too thin or swollen.
              SKIN: wounds, spots, blisters, or unusual color on the skin.
            - result: GOOD, ATTENTION (the keeper should look closer), or UNKNOWN (the part is not visible \
            or the photo is too unclear to judge).
            - note: for ATTENTION and UNKNOWN, one short sentence in Korean (polite 해요체) on what you saw \
            or what photo is needed. Empty for GOOD.
            """;

    private static final List<String> KEYS = Arrays.stream(HealthCheckKey.values()).map(Enum::name).toList();
    private static final List<String> RESULTS = Arrays.stream(HealthCheckResult.values()).map(Enum::name).toList();

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "isReptile", Map.of("type", "BOOLEAN"),
                    "items", Map.of(
                            "type", "ARRAY",
                            "items", Map.of(
                                    "type", "OBJECT",
                                    "properties", Map.of(
                                            "key", Map.of("type", "STRING", "enum", KEYS),
                                            "result", Map.of("type", "STRING", "enum", RESULTS),
                                            "note", Map.of("type", "STRING")),
                                    "required", List.of("key", "result")))),
            "required", List.of("isReptile", "items"));

    private final GeminiClient geminiClient;

    @Override
    public HealthCheckAnalysis analyze(byte[] jpeg, byte[] previous, Set<HealthCheckKey> keys) {
        List<byte[]> photos = previous == null ? List.of(jpeg) : List.of(jpeg, previous);
        String prompt = PROMPT.formatted(String.join(", ", keys.stream().map(Enum::name).toList()));
        Answer answer = geminiClient.generate(prompt, RESPONSE_SCHEMA, photos, Answer.class);
        if (!Boolean.TRUE.equals(answer.isReptile())) {
            return HealthCheckAnalysis.notReptile();
        }
        return new HealthCheckAnalysis(true, items(answer.items(), keys));
    }

    /**
     * 요청한 항목만 그 순서로. 같은 항목이 여러 번이면 처음 것, 빠진 항목은 UNKNOWN으로 채운다.
     */
    static List<HealthCheckItem> items(List<Answer.Item> answers, Set<HealthCheckKey> keys) {
        Map<HealthCheckKey, HealthCheckItem> byKey = new EnumMap<>(HealthCheckKey.class);
        if (answers != null) {
            answers.stream()
                    .filter(Objects::nonNull)
                    .map(GeminiHealthCheckAnalyzer::toItem)
                    .filter(Objects::nonNull)
                    .filter(item -> keys.contains(item.key()))
                    .forEach(item -> byKey.putIfAbsent(item.key(), item));
        }
        if (byKey.size() < keys.size()) {
            log.warn("Gemini 건강 체크 답에 빠진 항목이 있어요: {}", answers);
        }
        return Arrays.stream(HealthCheckKey.values())
                .filter(keys::contains)
                .map(key -> byKey.getOrDefault(key, new HealthCheckItem(key, HealthCheckResult.UNKNOWN, null)))
                .toList();
    }

    private static HealthCheckItem toItem(Answer.Item answer) {
        HealthCheckKey key = parse(HealthCheckKey.class, answer.key());
        HealthCheckResult result = parse(HealthCheckResult.class, answer.result());
        if (key == null || result == null) {
            return null;
        }
        String note = result == HealthCheckResult.GOOD ? null : note(answer.note());
        return new HealthCheckItem(key, result, note);
    }

    private static String note(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.length() > MAX_NOTE_LENGTH ? trimmed.substring(0, MAX_NOTE_LENGTH) : trimmed;
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Answer(Boolean isReptile, List<Item> items) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Item(String key, String result, String note) {
        }
    }
}
