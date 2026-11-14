package com.tailfriends.enclosure.dto;

import java.util.List;

/**
 * animalIds는 합사 보기로 함께 띄울 개체, 저장한 순서. 정하지 않았으면 빈 배열이다.
 */
public record EnclosureResponse(
        Long id,
        BackgroundResponse background,
        List<DecorationResponse> decorations,
        List<Long> animalIds
) {
}
