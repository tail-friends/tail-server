package com.tailfriends.log.service;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.log.dto.LogCreateRequest;
import com.tailfriends.log.dto.LogIdResponse;
import com.tailfriends.log.dto.LogSyncRequest;
import com.tailfriends.log.dto.LogSyncResponse;
import com.tailfriends.log.dto.LogSyncResult;
import com.tailfriends.log.repository.LogRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 오프라인 기록 동기화 (F-15). 일괄 기록과 달리 항목마다 따로 저장해서, 하나가 실패해도 나머지는 저장한다.
 */
@Service
@RequiredArgsConstructor
public class LogSyncService {

    private final CurrentUser currentUser;
    private final LogService logService;
    private final LogRepository logRepository;
    private final Validator validator;

    /**
     * 항목마다 {@link LogService#create}를 따로 부른다(항목마다 트랜잭션이 따로 열린다). 그래서 이 메서드에는
     * 트랜잭션을 걸지 않는다. 포인트·먹이 재고·건강 경고도 한 건씩 기록할 때와 같이 처리된다.
     * 이미 있는 clientId는 DUPLICATE와 기존 기록 ID, 검증이나 처리에 실패하면 FAILED와 에러를 돌려준다.
     */
    public LogSyncResponse sync(LogSyncRequest request) {
        Long userId = currentUser.id();
        List<LogSyncResult> results = request.logs().stream()
                .map(item -> syncOne(userId, item))
                .toList();
        return new LogSyncResponse(results);
    }

    private LogSyncResult syncOne(Long userId, LogCreateRequest item) {
        if (item.clientId() == null) {
            return LogSyncResult.failed(null, ErrorCode.INVALID_INPUT, "clientId가 필요해요");
        }
        Set<ConstraintViolation<LogCreateRequest>> violations = validator.validate(item);
        if (!violations.isEmpty()) {
            return LogSyncResult.failed(item.clientId(), ErrorCode.INVALID_INPUT, describe(violations));
        }
        try {
            return LogSyncResult.created(item.clientId(), logService.create(item).id());
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.DUPLICATE_CLIENT_ID && e.getData() instanceof LogIdResponse existing) {
                return LogSyncResult.duplicate(item.clientId(), existing.id());
            }
            return LogSyncResult.failed(item.clientId(), e.getErrorCode(), e.getMessage());
        } catch (DataIntegrityViolationException e) {
            // 같은 clientId를 동시에 보내 유니크 제약에 걸린 경우
            return logRepository.findByUserIdAndClientId(userId, item.clientId())
                    .map(existing -> LogSyncResult.duplicate(item.clientId(), existing.getId()))
                    .orElseGet(() -> LogSyncResult.failed(item.clientId(), ErrorCode.INTERNAL_ERROR,
                            ErrorCode.INTERNAL_ERROR.getMessage()));
        }
    }

    private static String describe(Set<ConstraintViolation<LogCreateRequest>> violations) {
        return violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
