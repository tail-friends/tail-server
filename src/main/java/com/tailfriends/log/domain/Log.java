package com.tailfriends.log.domain;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.global.domain.BaseTimeEntity;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * 사육 기록. 종류(type)에 해당하는 컬럼만 값이 있다.
 */
@Getter
@Entity
@Table(name = "logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Log extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 앱이 만든 ID. 같은 기록을 두 번 저장하지 않으려고 쓴다.
     */
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LogType type;

    @Column(nullable = false)
    private LocalDate logDate;

    @Column(length = 200)
    private String memo;

    @Column(length = 40)
    private String food;

    private Integer foodCount;

    private Boolean refused;

    private Boolean dusted;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private ShedStage shedStage;

    private Boolean shedComplete;

    @Column(name = "weight_g", precision = 7, scale = 1)
    private BigDecimal weightG;

    private Boolean cleanFull;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Stool stool;

    @Column(name = "hot_c", precision = 4, scale = 1)
    private BigDecimal hotC;

    @Column(name = "cool_c", precision = 4, scale = 1)
    private BigDecimal coolC;

    private Integer humidity;

    /**
     * 일괄 기록으로 함께 저장한 기록 묶음. 혼자 저장했으면 null.
     */
    private UUID batchId;

    private Log(Animal animal, User user, UUID clientId, LogType type, LocalDate logDate, String memo) {
        this.animal = animal;
        this.user = user;
        this.clientId = clientId;
        this.type = type;
        this.logDate = logDate;
        this.memo = memo;
    }

    /**
     * 종류에 해당하지 않는 값은 버리고, 빠진 예/아니오 값은 false로 채운다.
     *
     * @throws BusinessException 종류별 필수 값이 없으면 INVALID_INPUT
     */
    public static Log create(Animal animal, User user, UUID clientId, LogType type, LocalDate logDate,
                             String memo, LogDetails details) {
        Log log = new Log(animal, user, clientId, type, logDate, memo);
        log.apply(details);
        log.fillDefaults();
        log.validate();
        return log;
    }

    /**
     * 일괄 기록의 한 건. 나머지는 create와 같다.
     *
     * @throws BusinessException 종류별 필수 값이 없으면 INVALID_INPUT
     */
    public static Log createInBatch(UUID batchId, Animal animal, User user, UUID clientId, LogType type,
                                    LocalDate logDate, String memo, LogDetails details) {
        Log log = create(animal, user, clientId, type, logDate, memo, details);
        log.batchId = batchId;
        return log;
    }

    /**
     * null인 값은 바꾸지 않는다. 종류는 바꿀 수 없다.
     *
     * @throws BusinessException 바꾼 뒤 종류별 필수 값이 없으면 INVALID_INPUT
     */
    public void update(LocalDate logDate, String memo, LogDetails details) {
        if (logDate != null) {
            this.logDate = logDate;
        }
        if (memo != null) {
            this.memo = memo;
        }
        apply(details);
        validate();
    }

    private void apply(LogDetails details) {
        if (details == null) {
            return;
        }
        switch (type) {
            case FEED -> {
                food = valueOr(details.food(), food);
                foodCount = valueOr(details.count(), foodCount);
                refused = valueOr(details.refused(), refused);
                dusted = valueOr(details.dusted(), dusted);
            }
            case SHED -> {
                shedStage = valueOr(details.stage(), shedStage);
                shedComplete = valueOr(details.complete(), shedComplete);
            }
            case WEIGHT -> weightG = valueOr(details.weight(), weightG);
            case CLEAN -> {
                cleanFull = valueOr(details.full(), cleanFull);
                stool = valueOr(details.stool(), stool);
            }
            case ENV -> {
                hotC = valueOr(details.hot(), hotC);
                coolC = valueOr(details.cool(), coolC);
                humidity = valueOr(details.humidity(), humidity);
            }
            case NOTE -> {
            }
        }
    }

    private void fillDefaults() {
        switch (type) {
            case FEED -> {
                refused = valueOr(refused, false);
                dusted = valueOr(dusted, false);
            }
            case SHED -> shedComplete = valueOr(shedComplete, false);
            case CLEAN -> cleanFull = valueOr(cleanFull, false);
            default -> {
            }
        }
    }

    private void validate() {
        switch (type) {
            case SHED -> require(shedStage != null, "탈피 기록에는 stage가 필요해요");
            case WEIGHT -> require(weightG != null, "체중 기록에는 value가 필요해요");
            case ENV -> require(hotC != null || coolC != null || humidity != null,
                    "온습도 기록에는 hot, cool, humidity 중 하나가 필요해요");
            case NOTE -> require(StringUtils.hasText(memo), "메모 기록에는 memo가 필요해요");
            default -> {
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, message);
        }
    }

    private static <T> T valueOr(T value, T fallback) {
        return value != null ? value : fallback;
    }
}
