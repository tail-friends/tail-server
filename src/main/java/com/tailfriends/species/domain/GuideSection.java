package com.tailfriends.species.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사육 가이드 카드의 섹션. 선언 순서가 카드에 보여주는 순서다.
 */
@Getter
@RequiredArgsConstructor
public enum GuideSection {

    HABIT("습성"),
    FEED("먹이"),
    ENV("환경"),
    HEALTH("건강");

    private final String title;
}
