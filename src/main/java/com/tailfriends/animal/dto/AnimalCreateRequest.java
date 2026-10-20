package com.tailfriends.animal.dto;

import com.tailfriends.animal.domain.Sex;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * speciesId와 speciesName 중 하나는 있어야 한다. 둘 다 있으면 speciesId를 쓴다.
 */
public record AnimalCreateRequest(
        @NotBlank @Size(max = 30) String name,
        Long speciesId,
        @Size(max = 40) String speciesName,
        @Size(max = 40) String morph,
        Sex sex,
        LocalDate hatchDate,
        @NotNull @Min(1) @Max(60) Integer feedIntervalDays,
        @Size(max = 300) String memo
) {
}
