package com.tailfriends.animal.service;

import com.tailfriends.animal.dto.WeightGraphResponse;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.log.repository.LogRepository;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WeightService {

    private final CurrentUser currentUser;
    private final AnimalRepository animalRepository;
    private final LogRepository logRepository;
    private final Clock clock;

    /**
     * to가 없으면 오늘(KST), from이 없으면 to의 1년 전. 둘 다 포함한다.
     */
    @Transactional(readOnly = true)
    public WeightGraphResponse getWeights(Long animalId, LocalDate from, LocalDate to) {
        Long userId = currentUser.id();
        animalRepository.findByIdAndUserId(animalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        LocalDate end = to != null ? to : KstDate.today(clock);
        LocalDate start = from != null ? from : end.minusYears(1);
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "from은 to보다 늦을 수 없어요");
        }
        return new WeightGraphResponse(logRepository.findWeights(animalId, start, end));
    }
}
