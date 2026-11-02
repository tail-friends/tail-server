package com.tailfriends.breeding.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 암수마다 내 개체면 ID, 외부 개체면 이름만 보낸다. 둘 중 하나는 있어야 하고 ID가 있으면 이름은 무시한다(서비스가 확인).
 * 메이팅일은 오늘(KST)까지.
 */
public record BreedingCreateRequest(
        Long maleId,
        @Size(max = 30) String maleName,
        Long femaleId,
        @Size(max = 30) String femaleName,
        @NotNull LocalDate pairedDate,
        @Size(max = 200) String memo
) {
}
