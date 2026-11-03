package com.tailfriends.breeding.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 해칭일은 메이팅일부터 오늘(KST)까지(서비스가 확인). 한 번에 50마리까지.
 */
public record HatchlingsCreateRequest(
        @NotNull LocalDate hatchDate,
        @NotEmpty @Size(max = 50) List<@NotNull @Valid Hatchling> hatchlings
) {

    public record Hatchling(
            @NotBlank @Size(max = 30) String name,
            @Size(max = 40) String morph
    ) {
    }
}
