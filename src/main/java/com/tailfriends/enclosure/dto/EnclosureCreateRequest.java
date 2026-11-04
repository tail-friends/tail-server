package com.tailfriends.enclosure.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 사진 테마 저장. objects를 생략하면 빈 배열. setAsHomeFor를 보내면 그 개체의 평소 사육장으로도 정한다.
 */
public record EnclosureCreateRequest(
        @NotNull Long photoId,
        @NotNull @Pattern(regexp = COLOR) String floorColor,
        @NotNull @Pattern(regexp = COLOR) String wallColor,
        @Size(max = 10) List<@NotNull @Valid BackgroundObjectRequest> objects,
        Long setAsHomeFor
) {

    static final String COLOR = "^#[0-9A-Fa-f]{6}$";

    public List<BackgroundObjectRequest> objectsOrEmpty() {
        return objects == null ? List.of() : objects;
    }
}
