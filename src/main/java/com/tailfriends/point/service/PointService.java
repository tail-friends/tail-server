package com.tailfriends.point.service;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.log.domain.Log;
import com.tailfriends.point.domain.PointHistory;
import com.tailfriends.point.domain.PointReason;
import com.tailfriends.point.dto.PointHistoryListResponse;
import com.tailfriends.point.dto.PointHistoryResponse;
import com.tailfriends.point.repository.PointHistoryRepository;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 케어 포인트. 기능명세서 4-7.
 */
@Service
@RequiredArgsConstructor
public class PointService {

    /**
     * 급여·체중·탈피·청소 기록 1건의 포인트.
     */
    static final int LOG_POINTS = 10;

    private final CurrentUser currentUser;
    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * 기록 1건에 포인트를 적립하고 적립한 포인트를 돌려준다. 기록을 저장한 트랜잭션 안에서 부른다.
     * 온습도·메모 기록이거나, 같은 개체·같은 종류를 오늘(KST, 기록의 날짜가 아니라 기록한 날) 이미 적립했으면 0.
     */
    @Transactional
    public int earnForLog(Log log) {
        if (!recordEarning(log)) {
            return 0;
        }
        userRepository.addPoints(log.getUser().getId(), LOG_POINTS);
        return LOG_POINTS;
    }

    /**
     * 일괄 기록용. 적립 기준은 {@link #earnForLog}와 같고, 잔액은 사용자마다 적립한 합계로 한 번만 더한다.
     */
    @Transactional
    public void earnForLogs(List<Log> logs) {
        Map<Long, Integer> earned = new LinkedHashMap<>();
        for (Log log : logs) {
            if (recordEarning(log)) {
                earned.merge(log.getUser().getId(), LOG_POINTS, Integer::sum);
            }
        }
        earned.forEach(userRepository::addPoints);
    }

    /**
     * 적립 내역을 남겼으면 true. 적립 대상이 아니거나 오늘 이미 적립했으면 false.
     */
    private boolean recordEarning(Log log) {
        Optional<PointReason> reason = PointReason.ofLog(log.getType());
        if (reason.isEmpty()) {
            return false;
        }
        return pointHistoryRepository.insertEarning(log.getUser().getId(), log.getAnimal().getId(), log.getId(),
                LOG_POINTS, reason.get().name(), KstDate.today(clock)) > 0;
    }

    /**
     * 잔액과 내역(최신순). cursor는 이전 페이지의 마지막 내역 ID다.
     */
    @Transactional(readOnly = true)
    public PointHistoryListResponse getHistory(Long cursor, int size) {
        Long userId = currentUser.id();
        int balance = userRepository.findById(userId)
                .map(User::getPoints)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        long before = cursor == null ? Long.MAX_VALUE : cursor;
        List<PointHistory> histories = pointHistoryRepository.findPage(userId, before, PageRequest.of(0, size + 1));

        boolean hasNext = histories.size() > size;
        List<PointHistory> page = hasNext ? histories.subList(0, size) : histories;
        Long nextCursor = hasNext ? page.getLast().getId() : null;
        return new PointHistoryListResponse(balance,
                page.stream().map(PointHistoryResponse::from).toList(), nextCursor);
    }
}
