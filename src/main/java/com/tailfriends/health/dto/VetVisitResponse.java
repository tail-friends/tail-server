package com.tailfriends.health.dto;

import com.tailfriends.health.domain.VetVisit;
import java.time.LocalDate;

public record VetVisitResponse(
        Long id,
        String hospital,
        LocalDate visitDate,
        String symptom,
        String diagnosis,
        String prescription,
        LocalDate nextVisitDate
) {

    public static VetVisitResponse from(VetVisit visit) {
        return new VetVisitResponse(visit.getId(), visit.getHospital(), visit.getVisitDate(), visit.getSymptom(),
                visit.getDiagnosis(), visit.getPrescription(), visit.getNextVisitDate());
    }
}
