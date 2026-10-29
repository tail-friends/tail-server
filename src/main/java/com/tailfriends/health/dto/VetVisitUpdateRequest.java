package com.tailfriends.health.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * null인 값은 바꾸지 않는다. 바꾼 뒤에도 작성과 같은 날짜 규칙을 지켜야 한다.
 */
public record VetVisitUpdateRequest(
        @Size(max = 40) String hospital,
        LocalDate visitDate,
        @Size(max = 200) String symptom,
        @Size(max = 200) String diagnosis,
        @Size(max = 200) String prescription,
        LocalDate nextVisitDate
) {
}
