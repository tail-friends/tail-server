package com.tailfriends.animal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tailfriends.species.domain.Home;
import jakarta.validation.constraints.AssertTrue;

/**
 * home(기본 테마)과 enclosureId(내 사육장) 중 하나만 보낸다. home은 대소문자를 가리지 않는다.
 */
public record AnimalHomeRequest(
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_VALUES) Home home,
        Long enclosureId
) {

    @AssertTrue(message = "home과 enclosureId 중 하나만 보내 주세요")
    boolean isOneOfHomeOrEnclosure() {
        return (home == null) != (enclosureId == null);
    }
}
