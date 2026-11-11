package com.tailfriends.log.controller;

import com.tailfriends.global.util.FileResponses;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.log.service.LogExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Log")
@RestController
@RequiredArgsConstructor
public class LogExportController {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final LogExportService logExportService;
    private final Clock clock;

    @Operation(summary = "기록 내보내기 (CSV)",
            description = "내 개체의 기록을 CSV 파일로 내려준다(UTF-8 BOM, 기록일 오름차순). animalId·from·to는 선택. "
                    + "from이 to보다 늦으면 400 INVALID_INPUT, 내 개체가 아니면 404 ANIMAL_NOT_FOUND.")
    @GetMapping("/api/v1/users/me/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) Long animalId,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
        byte[] csv = logExportService.exportCsv(animalId, from, to);
        return FileResponses.attachment(csv, TEXT_CSV, "tail-friends-logs-" + KstDate.today(clock) + ".csv");
    }
}
