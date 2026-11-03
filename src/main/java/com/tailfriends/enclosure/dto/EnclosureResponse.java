package com.tailfriends.enclosure.dto;

import java.util.List;

/**
 * animalIds는 합사 보기를 만들 때까지 빈 배열이다.
 */
public record EnclosureResponse(
        Long id,
        BackgroundResponse background,
        List<DecorationResponse> decorations,
        List<Long> animalIds
) {
}
