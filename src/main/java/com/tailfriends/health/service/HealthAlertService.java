package com.tailfriends.health.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.FeedSchedule;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.health.domain.AlertStatus;
import com.tailfriends.health.domain.AlertType;
import com.tailfriends.health.domain.EnvReading;
import com.tailfriends.health.domain.HealthAlert;
import com.tailfriends.health.domain.HealthRules;
import com.tailfriends.health.dto.AlertListResponse;
import com.tailfriends.health.dto.AlertResponse;
import com.tailfriends.health.dto.AlertUpdateRequest;
import com.tailfriends.health.repository.HealthAlertRepository;
import com.tailfriends.health.repository.OpenAlertCount;
import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.repository.LogRepository;
import com.tailfriends.species.domain.Species;
import com.tailfriends.user.domain.AlertRules;
import com.tailfriends.user.service.UserSettingsService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HealthAlertService {

    private final CurrentUser currentUser;
    private final HealthAlertRepository alertRepository;
    private final LogRepository logRepository;
    private final AnimalRepository animalRepository;
    private final UserSettingsService userSettingsService;
    private final Clock clock;

    /**
     * 기록을 저장·수정한 뒤 그 종류에 해당하는 규칙만 다시 판정한다. 새로 만든 경고를 돌려준다.
     * 같은 종류의 열린 경고가 이미 있으면 새로 만들지 않는다. 조건이 풀려도 경고를 자동으로 닫지 않는다.
     * 기준은 개체 주인의 경고 기준 설정이다.
     */
    @Transactional
    public List<AlertResponse> evaluate(Animal animal, LogType logType) {
        Optional<HealthAlert> alert = switch (logType) {
            case FEED -> judgeRefusal(animal, rulesOf(animal));
            case WEIGHT -> judgeWeightLoss(animal, rulesOf(animal));
            case CLEAN -> judgeLooseStool(animal, rulesOf(animal));
            case ENV -> judgeEnv(animal);
            default -> Optional.empty();
        };
        return alert
                .filter(candidate -> !alertRepository.existsByAnimalIdAndTypeAndStatus(
                        animal.getId(), candidate.getType(), AlertStatus.OPEN))
                .map(alertRepository::save)
                .map(AlertResponse::from)
                .stream()
                .toList();
    }

    /**
     * 최신순. animalId가 없으면 내 전체 개체, status가 없으면 모든 상태. cursor는 이전 페이지의 마지막 경고 ID다.
     */
    @Transactional(readOnly = true)
    public AlertListResponse getAlerts(Long animalId, AlertStatus status, Long cursor, int size) {
        Long userId = currentUser.id();
        Set<AlertStatus> statuses = status == null ? EnumSet.allOf(AlertStatus.class) : EnumSet.of(status);
        long before = cursor == null ? Long.MAX_VALUE : cursor;
        PageRequest limit = PageRequest.of(0, size + 1);

        List<HealthAlert> alerts;
        if (animalId == null) {
            alerts = alertRepository.findPageByUser(userId, statuses, before, limit);
        } else {
            animalRepository.findByIdAndUserId(animalId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
            alerts = alertRepository.findPageByAnimal(animalId, statuses, before, limit);
        }

        boolean hasNext = alerts.size() > size;
        List<HealthAlert> page = hasNext ? alerts.subList(0, size) : alerts;
        Long nextCursor = hasNext ? page.getLast().getId() : null;
        return new AlertListResponse(page.stream().map(AlertResponse::from).toList(), nextCursor);
    }

    @Transactional
    public AlertResponse update(Long alertId, AlertUpdateRequest request) {
        Long userId = currentUser.id();
        HealthAlert alert = alertRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALERT_NOT_FOUND));
        if (request.status() != AlertStatus.ACKNOWLEDGED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "status는 ACKNOWLEDGED만 보낼 수 있어요");
        }
        alert.acknowledge();
        return AlertResponse.from(alert);
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> openAlertsOf(Animal animal) {
        return alertRepository.findAllByAnimalIdAndStatusOrderByIdDesc(animal.getId(), AlertStatus.OPEN).stream()
                .map(AlertResponse::from)
                .toList();
    }

    /**
     * 개체 ID별 열린 경고 수. 쿼리 한 번으로 조회한다.
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> openCountsOf(List<Animal> animals) {
        if (animals.isEmpty()) {
            return Map.of();
        }
        return alertRepository.countOpenByAnimalIds(animals.stream().map(Animal::getId).toList()).stream()
                .collect(Collectors.toMap(OpenAlertCount::animalId, OpenAlertCount::count));
    }

    private AlertRules rulesOf(Animal animal) {
        return userSettingsService.alertRulesOf(animal.getUser().getId());
    }

    /**
     * 쿨링 기간에는 거식 경고를 만들지 않는다.
     */
    private Optional<HealthAlert> judgeRefusal(Animal animal, AlertRules rules) {
        if (FeedSchedule.isCooling(animal.getCoolingStart(), animal.getCoolingEnd(), KstDate.today(clock))) {
            return Optional.empty();
        }
        List<Boolean> latestRefused = logRepository.findLatestRefused(
                animal.getId(), PageRequest.of(0, rules.refusalCount()));
        if (!HealthRules.isRefusalStreak(rules, latestRefused)) {
            return Optional.empty();
        }
        return Optional.of(HealthAlert.open(animal, AlertType.REFUSAL_ALERT, HealthRules.refusalMessage(rules)));
    }

    /**
     * 가장 최근 체중을, 그 기록일로부터 weightLossDays일 안의 최고 체중과 비교한다.
     */
    private Optional<HealthAlert> judgeWeightLoss(Animal animal, AlertRules rules) {
        Optional<Log> latest = logRepository.findFirstByAnimalIdAndTypeOrderByLogDateDescIdDesc(
                animal.getId(), LogType.WEIGHT);
        if (latest.isEmpty()) {
            return Optional.empty();
        }
        Log latestWeight = latest.get();
        BigDecimal highest = logRepository.findHighestWeight(
                        animal.getId(),
                        latestWeight.getLogDate().minusDays(rules.weightLossDays()),
                        latestWeight.getLogDate())
                .orElse(null);
        OptionalInt percent = HealthRules.weightLossPercent(rules, latestWeight.getWeightG(), highest);
        if (percent.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(HealthAlert.open(animal, AlertType.WEIGHT_LOSS_ALERT,
                HealthRules.weightLossMessage(rules, percent.getAsInt())));
    }

    private Optional<HealthAlert> judgeLooseStool(Animal animal, AlertRules rules) {
        boolean streak = HealthRules.isLooseStoolStreak(rules, logRepository.findLatestStools(
                animal.getId(), PageRequest.of(0, rules.looseStoolCount())));
        if (!streak) {
            return Optional.empty();
        }
        return Optional.of(HealthAlert.open(animal, AlertType.STOOL_ALERT, HealthRules.looseStoolMessage(rules)));
    }

    /**
     * 가장 최근 온습도 기록을 종의 적정 범위와 비교한다. 핫존은 바닥 표면 온도다.
     * 목록에 없는 종(직접 입력)은 범위가 없어 판정하지 않는다. 쿨링 기간에는 일부러 온도를 낮추므로 판정하지 않는다.
     */
    private Optional<HealthAlert> judgeEnv(Animal animal) {
        Species species = animal.getSpecies();
        if (species == null
                || FeedSchedule.isCooling(animal.getCoolingStart(), animal.getCoolingEnd(), KstDate.today(clock))) {
            return Optional.empty();
        }
        Optional<Log> latest = logRepository.findFirstByAnimalIdAndTypeOrderByLogDateDescIdDesc(
                animal.getId(), LogType.ENV);
        if (latest.isEmpty()) {
            return Optional.empty();
        }
        Log env = latest.get();
        List<EnvReading> readings = List.of(
                new EnvReading("핫존 온도", env.getHotC(), species.getHotMin(), species.getHotMax(), "℃"),
                new EnvReading("쿨존 온도", env.getCoolC(), species.getCoolMin(), species.getCoolMax(), "℃"),
                EnvReading.humidity(env.getHumidity(), species.getHumidityMin(), species.getHumidityMax()));
        return HealthRules.envWarningMessage(readings)
                .map(message -> HealthAlert.open(animal, AlertType.ENV_WARNING, message));
    }
}
