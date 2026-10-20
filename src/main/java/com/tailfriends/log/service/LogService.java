package com.tailfriends.log.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.dto.FeedResponse;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.animal.service.FeedService;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.dto.LogCreateRequest;
import com.tailfriends.log.dto.LogCreateResponse;
import com.tailfriends.log.dto.LogIdResponse;
import com.tailfriends.log.dto.LogListResponse;
import com.tailfriends.log.dto.LogResponse;
import com.tailfriends.log.dto.LogUpdateRequest;
import com.tailfriends.log.repository.LogRepository;
import com.tailfriends.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogService {

    private final LogRepository logRepository;
    private final AnimalRepository animalRepository;
    private final UserRepository userRepository;
    private final FeedService feedService;
    private final Clock clock;

    /**
     * 같은 사용자가 같은 clientId로 다시 보내면 저장하지 않고 409와 기존 기록 ID를 돌려준다. (오프라인 재전송)
     */
    @Transactional
    public LogCreateResponse create(Long userId, LogCreateRequest request) {
        validateDate(request.date());
        Animal animal = findAnimal(userId, request.animalId());
        if (request.clientId() != null) {
            logRepository.findByUserIdAndClientId(userId, request.clientId()).ifPresent(existing -> {
                throw new BusinessException(ErrorCode.DUPLICATE_CLIENT_ID,
                        ErrorCode.DUPLICATE_CLIENT_ID.getMessage(), new LogIdResponse(existing.getId()));
            });
        }

        Log log = logRepository.save(Log.create(
                animal,
                userRepository.getReferenceById(userId),
                request.clientId(),
                request.type(),
                request.date(),
                request.memo(),
                request.toDetails()));
        return new LogCreateResponse(log.getId(), FeedResponse.from(feedService.scheduleOf(animal)));
    }

    /**
     * 기록일 최신순. cursor는 이전 페이지의 마지막 기록 ID다.
     */
    public LogListResponse getLogs(Long userId, Long animalId, LogType type, Long cursor, int size) {
        findAnimal(userId, animalId);
        Set<LogType> types = type == null ? EnumSet.allOf(LogType.class) : EnumSet.of(type);
        PageRequest limit = PageRequest.of(0, size + 1);

        List<Log> logs;
        if (cursor == null) {
            logs = logRepository.findFirstPage(animalId, types, limit);
        } else {
            Log last = logRepository.findByIdAndUserId(cursor, userId)
                    .filter(log -> log.getAnimal().getId().equals(animalId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "cursor가 올바르지 않아요"));
            logs = logRepository.findPageAfter(animalId, types, last.getLogDate(), last.getId(), limit);
        }

        boolean hasNext = logs.size() > size;
        List<Log> page = hasNext ? logs.subList(0, size) : logs;
        Long nextCursor = hasNext ? page.getLast().getId() : null;
        return new LogListResponse(page.stream().map(LogResponse::from).toList(), nextCursor);
    }

    @Transactional
    public LogResponse update(Long userId, Long logId, LogUpdateRequest request) {
        Log log = findLog(userId, logId);
        validateDate(request.date());
        log.update(request.date(), request.memo(), request.toDetails());
        return LogResponse.from(log);
    }

    @Transactional
    public void delete(Long userId, Long logId) {
        logRepository.delete(findLog(userId, logId));
    }

    /**
     * 없거나 남의 개체면 존재를 숨기기 위해 똑같이 ANIMAL_NOT_FOUND.
     */
    private Animal findAnimal(Long userId, Long animalId) {
        return animalRepository.findByIdAndUserId(animalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
    }

    private Log findLog(Long userId, Long logId) {
        return logRepository.findByIdAndUserId(logId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOG_NOT_FOUND));
    }

    private void validateDate(LocalDate date) {
        if (date != null && date.isAfter(KstDate.today(clock))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "기록일은 오늘 이후일 수 없어요");
        }
    }
}
