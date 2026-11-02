package com.tailfriends.breeding.dto;

import com.tailfriends.breeding.domain.BreedingStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * null인 값은 바꾸지 않는다. clutches를 보내면 산란 기록을 통째로 바꾸고, 빈 배열이면 모두 지운다.
 */
public record BreedingUpdateRequest(
        BreedingStatus status,
        @Size(max = 20) List<@NotNull @Valid ClutchRequest> clutches
) {
}
