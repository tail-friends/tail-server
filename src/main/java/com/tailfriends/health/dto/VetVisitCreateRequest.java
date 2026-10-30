package com.tailfriends.health.dto;

import com.tailfriends.schedule.dto.RepeatRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * animalId, visitDate만 필수다. 방문일은 오늘(KST)까지, 다음 방문일은 방문일보다 늦어야 한다(서비스가 확인).
 * createMedicationSchedule을 보내면 투약 반복 일정도 만든다.
 */
public record VetVisitCreateRequest(
        @NotNull Long animalId,
        @Size(max = 40) String hospital,
        @NotNull LocalDate visitDate,
        @Size(max = 200) String symptom,
        @Size(max = 200) String diagnosis,
        @Size(max = 200) String prescription,
        LocalDate nextVisitDate,
        @Valid MedicationSchedule createMedicationSchedule
) {

    /**
     * 종류는 MEDICATION, 개체는 진료의 개체, 시작일은 오늘(KST)이다. repeat은 반복 일정 생성과 같은 형식.
     */
    public record MedicationSchedule(
            @NotBlank @Size(max = 30) String name,
            @NotNull @Valid RepeatRequest repeat,
            LocalDate endDate
    ) {
    }
}
