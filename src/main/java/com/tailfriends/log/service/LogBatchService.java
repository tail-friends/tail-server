package com.tailfriends.log.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.health.service.HealthAlertService;
import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.dto.LogBatchRequest;
import com.tailfriends.log.dto.LogBatchRequest.Item;
import com.tailfriends.log.dto.LogBatchResponse;
import com.tailfriends.log.dto.LogIdResponse;
import com.tailfriends.log.repository.LogRepository;
import com.tailfriends.point.service.PointService;
import com.tailfriends.schedule.service.ScheduleService;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일괄 기록 (기능명세서 4-9). 한 트랜잭션으로 저장해서 하나라도 실패하면 전부 저장하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class LogBatchService {

    static final Set<LogType> BATCH_TYPES = EnumSet.of(LogType.FEED, LogType.CLEAN, LogType.WEIGHT);
    static final Duration UNDO_WINDOW = Duration.ofSeconds(5);

    private final CurrentUser currentUser;
    private final LogRepository logRepository;
    private final AnimalRepository animalRepository;
    private final UserRepository userRepository;
    private final HealthAlertService healthAlertService;
    private final ScheduleService scheduleService;
    private final PointService pointService;
    private final Clock clock;

    /**
     * 케어 포인트는 한 건씩 기록할 때와 같이 개체마다 하루 한 번 적립한다. 되돌려도 빼지 않는다.
     *
     * @throws BusinessException 지원하지 않는 종류·미래 날짜·같은 개체나 clientId가 두 번이면 INVALID_INPUT,
     *                           내 개체가 아니면 ANIMAL_NOT_FOUND, 이미 저장된 clientId가 있으면 DUPLICATE_CLIENT_ID
     */
    @Transactional
    public LogBatchResponse create(LogBatchRequest request) {
        Long userId = currentUser.id();
        validate(request);
        Map<Long, Animal> animals = findAnimals(userId, request.items());
        rejectSavedClientIds(userId, request.items());

        UUID batchId = UUID.randomUUID();
        User user = userRepository.getReferenceById(userId);
        List<Log> logs = request.items().stream()
                .map(item -> logRepository.save(Log.createInBatch(batchId, animals.get(item.animalId()), user,
                        item.clientId(), request.type(), request.date(), item.memo(), item.toDetails())))
                .toList();
        logs.stream()
                .filter(log -> Boolean.TRUE.equals(log.getDusted()))
                .forEach(log -> scheduleService.completeDusting(userId, log.getAnimal().getId(), log.getLogDate()));
        logs.forEach(pointService::earnForLog);
        animals.values().forEach(animal -> healthAlertService.evaluate(animal, request.type()));
        return new LogBatchResponse(batchId, logs.stream().map(Log::getId).toList(), undoUntil(logs));
    }

    /**
     * 저장 후 5초(응답의 undoUntil) 안에만 통째로 지운다. 이번 기록으로 생긴 건강 경고와 자동 완료한 더스팅 일정은
     * 그대로 둔다(기록 삭제와 같음).
     *
     * @throws BusinessException 없거나 남의 일괄 기록이면 BATCH_NOT_FOUND, 5초가 지났으면 UNDO_EXPIRED
     */
    @Transactional
    public void undo(UUID batchId) {
        List<Log> logs = logRepository.findAllByUserIdAndBatchIdOrderByIdAsc(currentUser.id(), batchId);
        if (logs.isEmpty()) {
            throw new BusinessException(ErrorCode.BATCH_NOT_FOUND);
        }
        if (clock.instant().isAfter(undoUntil(logs))) {
            throw new BusinessException(ErrorCode.UNDO_EXPIRED);
        }
        logRepository.deleteAll(logs);
    }

    private void validate(LogBatchRequest request) {
        if (!BATCH_TYPES.contains(request.type())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "일괄 기록은 급여, 청소, 체중만 돼요");
        }
        if (request.date().isAfter(KstDate.today(clock))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "기록일은 오늘 이후일 수 없어요");
        }
        if (hasDuplicate(request.items().stream().map(Item::animalId).toList())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 개체가 두 번 들어 있어요");
        }
        if (hasDuplicate(request.items().stream().map(Item::clientId).filter(Objects::nonNull).toList())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 clientId가 두 번 들어 있어요");
        }
    }

    /**
     * 없거나 남의 개체가 하나라도 있으면 존재를 숨기기 위해 ANIMAL_NOT_FOUND.
     */
    private Map<Long, Animal> findAnimals(Long userId, List<Item> items) {
        Map<Long, Animal> mine = animalRepository.findAllByUserIdOrderByIdAsc(userId).stream()
                .collect(Collectors.toMap(Animal::getId, Function.identity()));
        Map<Long, Animal> animals = new LinkedHashMap<>();
        for (Item item : items) {
            Animal animal = mine.get(item.animalId());
            if (animal == null) {
                throw new BusinessException(ErrorCode.ANIMAL_NOT_FOUND);
            }
            animals.put(animal.getId(), animal);
        }
        return animals;
    }

    /**
     * 오프라인에서 보낸 일괄 기록을 다시 보낸 경우. 하나라도 이미 있으면 전부 저장하지 않고,
     * items 순서로 첫 번째 중복의 기존 기록 ID를 돌려준다.
     */
    private void rejectSavedClientIds(Long userId, List<Item> items) {
        List<UUID> clientIds = items.stream().map(Item::clientId).filter(Objects::nonNull).toList();
        if (clientIds.isEmpty()) {
            return;
        }
        Map<UUID, Long> saved = logRepository.findAllByUserIdAndClientIdIn(userId, clientIds).stream()
                .collect(Collectors.toMap(Log::getClientId, Log::getId));
        clientIds.stream()
                .filter(saved::containsKey)
                .findFirst()
                .map(saved::get)
                .ifPresent(existingId -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_CLIENT_ID,
                            ErrorCode.DUPLICATE_CLIENT_ID.getMessage(), new LogIdResponse(existingId));
                });
    }

    private static Instant undoUntil(List<Log> logs) {
        return logs.stream().map(Log::getCreatedAt).min(Instant::compareTo).orElseThrow().plus(UNDO_WINDOW);
    }

    private static <T> boolean hasDuplicate(List<T> values) {
        return new HashSet<>(values).size() != values.size();
    }
}
