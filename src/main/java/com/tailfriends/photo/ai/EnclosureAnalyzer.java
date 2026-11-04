package com.tailfriends.photo.ai;

/**
 * 사육장 사진에서 바닥·벽 색과 주요 장식을 읽는다. 지금은 Gemini, 나중에 다른 모델로 바꾸면 구현만 바꾼다.
 */
public interface EnclosureAnalyzer {

    /**
     * @param jpeg 분석할 JPG
     * @throws com.tailfriends.global.exception.BusinessException AI를 쓸 수 없거나 답이 형식에 맞지 않으면 AI_UNAVAILABLE
     */
    EnclosureAnalysis analyze(byte[] jpeg);
}
