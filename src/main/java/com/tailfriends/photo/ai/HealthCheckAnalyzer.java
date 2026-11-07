package com.tailfriends.photo.ai;

import com.tailfriends.health.domain.HealthCheckKey;
import java.util.Set;

/**
 * 사진으로 겉보기 건강 상태를 항목별로 판정한다. 진단명이나 치료법은 만들지 않는다.
 */
public interface HealthCheckAnalyzer {

    /**
     * @param jpeg     체크할 JPG
     * @param previous 같은 개체의 이전 체크 사진(꼬리 두께 비교용). 없으면 null
     * @param keys     판정할 항목. 결과에는 이 항목만 이 순서로 들어 있다
     * @throws com.tailfriends.global.exception.BusinessException AI를 쓸 수 없거나 답이 형식에 맞지 않으면 AI_UNAVAILABLE
     */
    HealthCheckAnalysis analyze(byte[] jpeg, byte[] previous, Set<HealthCheckKey> keys);
}
