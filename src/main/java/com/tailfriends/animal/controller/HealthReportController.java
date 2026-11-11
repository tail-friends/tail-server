package com.tailfriends.animal.controller;

import com.tailfriends.animal.service.HealthReportService;
import com.tailfriends.global.util.FileResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Animal")
@RestController
@RequiredArgsConstructor
public class HealthReportController {

    private final HealthReportService healthReportService;

    @Operation(summary = "수의사용 건강 리포트 (PDF)",
            description = "오늘(KST)까지 최근 months개월(1~12, 기본 3)의 체중 그래프, 급여·거식·탈피 요약, 진료 기록을 담은 PDF. "
                    + "months가 범위 밖이면 400 INVALID_INPUT, 내 개체가 아니면 404 ANIMAL_NOT_FOUND.")
    @GetMapping("/api/v1/animals/{animalId}/report")
    public ResponseEntity<byte[]> getReport(@PathVariable Long animalId,
                                            @RequestParam(defaultValue = "3") @Min(1) @Max(12) int months) {
        byte[] pdf = healthReportService.createReport(animalId, months);
        return FileResponses.attachment(pdf, MediaType.APPLICATION_PDF, "health-report-" + animalId + ".pdf");
    }
}
