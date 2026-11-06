package com.tailfriends.enclosure.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 배치를 통째로 바꾼다. 빈 배열이면 모두 치운다.
 */
public record DecorationsUpdateRequest(
        @NotNull @Size(max = 20) List<@NotNull @Valid DecorationRequest> decorations
) {
}
