package com.tailfriends.animal.dto;

import com.tailfriends.animal.domain.Sex;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 보낸 필드만 바꾼다. null이거나 보내지 않은 필드는 그대로 둔다.
 */
public record AnimalUpdateRequest(
        @Size(max = 30) @Pattern(regexp = "(?s).*\\S.*", message = "공백만 입력할 수 없습니다") String name,
        Long speciesId,
        @Size(max = 40) @Pattern(regexp = "(?s).*\\S.*", message = "공백만 입력할 수 없습니다") String speciesName,
        @Size(max = 40) String morph,
        Sex sex,
        LocalDate hatchDate,
        @Min(1) @Max(60) Integer feedIntervalDays,
        @Size(max = 300) String memo
) {
}
