package com.tailfriends.photo.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param model      Gemini 모델 이름
 * @param dailyLimit 사용자당 하루(KST) 분석 횟수. AI가 답을 준 분석만 센다
 * @param apiKey     Gemini API 키. 환경 변수로만 넣는다. 비어 있으면 분석할 수 없다.
 * @param dailyAttemptLimit 사용자당 하루(KST) 분석 시도 횟수. 실패한 시도도 센다
 */
@ConfigurationProperties(prefix = "ai")
public record AiProperties(
        String model,
        int dailyLimit,
        String apiKey,
        int dailyAttemptLimit
) {
}
