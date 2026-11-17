package com.tailfriends.enclosure.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 합사 보기로 함께 띄울 개체를 통째로 바꾼다. 최대 4마리, 빈 배열이면 모두 뺀다.
 */
public record EnclosureAnimalsUpdateRequest(
        @NotNull @Size(max = 4) List<@NotNull Long> animalIds
) {
}
