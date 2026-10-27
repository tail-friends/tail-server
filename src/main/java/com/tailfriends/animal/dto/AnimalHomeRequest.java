package com.tailfriends.animal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tailfriends.species.domain.Home;
import jakarta.validation.constraints.NotNull;

/**
 * home은 대소문자를 가리지 않는다. 사진으로 만든 테마(enclosureId)는 enclosures 테이블이 생기면 받는다.
 */
public record AnimalHomeRequest(
        @NotNull @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_VALUES) Home home
) {
}
