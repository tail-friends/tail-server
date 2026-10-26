package com.tailfriends.schedule.dto;

import com.tailfriends.schedule.domain.ScheduleKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 보낸 필드만 바꾼다. animalId는 바꿀 수 없고, endDate를 비울 수는 없다.
 */
public record ScheduleUpdateRequest(
        @Size(max = 30) @Pattern(regexp = "(?s).*\\S.*", message = "공백만 입력할 수 없습니다") String name,
        ScheduleKind kind,
        @Valid RepeatRequest repeat,
        LocalDate startDate,
        LocalDate endDate
) {
}
