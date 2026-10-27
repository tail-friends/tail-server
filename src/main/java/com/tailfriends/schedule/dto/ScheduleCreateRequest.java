package com.tailfriends.schedule.dto;

import com.tailfriends.schedule.domain.ScheduleKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * animalId가 null이면 모든 개체 공통 일정. startDate가 첫 일정일이다.
 */
public record ScheduleCreateRequest(
        Long animalId,
        @NotBlank @Size(max = 30) String name,
        @NotNull ScheduleKind kind,
        @NotNull @Valid RepeatRequest repeat,
        @NotNull LocalDate startDate,
        LocalDate endDate
) {
}
