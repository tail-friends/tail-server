package com.tailfriends.log.service;

import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.log.repository.LogRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기록 내보내기 CSV(F-17). 개체별 수의사용 PDF는 HealthReportService.
 */
@Service
@RequiredArgsConstructor
public class LogExportService {

    private final CurrentUser currentUser;
    private final AnimalRepository animalRepository;
    private final LogRepository logRepository;

    /**
     * 내 기록을 CSV로 만든다. animalId·from·to는 선택이고, 없으면 거르지 않는다(기간은 양끝 포함).
     *
     * @throws BusinessException from이 to보다 늦으면 INVALID_INPUT, 개체가 없거나 내 개체가 아니면 ANIMAL_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public byte[] exportCsv(Long animalId, LocalDate from, LocalDate to) {
        Long userId = currentUser.id();
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "시작일이 종료일보다 늦어요");
        }
        if (animalId != null && animalRepository.findByIdAndUserId(animalId, userId).isEmpty()) {
            throw new BusinessException(ErrorCode.ANIMAL_NOT_FOUND);
        }
        return LogCsvWriter.write(logRepository.findForExport(userId, animalId, from, to));
    }
}
