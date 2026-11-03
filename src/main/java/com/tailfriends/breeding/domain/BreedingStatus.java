package com.tailfriends.breeding.domain;

/**
 * 메이팅 → 포란 → 인큐베이션 → 해칭(또는 실패). 순서는 확인하지 않는다.
 */
public enum BreedingStatus {
    PAIRED,
    GRAVID,
    INCUBATING,
    HATCHED,
    FAILED
}
