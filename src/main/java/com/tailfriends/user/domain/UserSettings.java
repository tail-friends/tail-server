package com.tailfriends.user.domain;

import com.tailfriends.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 설정. 사용자와 1:1이며, 바꾼 적이 없으면 행이 없고 기본값을 쓴다.
 */
@Getter
@Entity
@Table(name = "user_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSettings extends BaseTimeEntity {

    public static final LocalTime DEFAULT_NOTIFY_TIME = LocalTime.of(9, 0);

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalTime notifyTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private TempUnit tempUnit;

    @Column(nullable = false)
    private int refusalCount;

    @Column(nullable = false)
    private int weightLossPercent;

    @Column(nullable = false)
    private int weightLossDays;

    @Column(nullable = false)
    private int looseStoolCount;

    private UserSettings(User user) {
        this.user = user;
        this.notifyTime = DEFAULT_NOTIFY_TIME;
        this.tempUnit = TempUnit.C;
        applyAlertRules(AlertRules.DEFAULT);
    }

    public static UserSettings defaultsFor(User user) {
        return new UserSettings(user);
    }

    public AlertRules alertRules() {
        return new AlertRules(refusalCount, weightLossPercent, weightLossDays, looseStoolCount);
    }

    /**
     * null인 값은 그대로 둔다.
     */
    public void update(LocalTime notifyTime, TempUnit tempUnit, Integer refusalCount, Integer weightLossPercent,
                       Integer weightLossDays, Integer looseStoolCount) {
        if (notifyTime != null) {
            this.notifyTime = notifyTime;
        }
        if (tempUnit != null) {
            this.tempUnit = tempUnit;
        }
        AlertRules current = alertRules();
        applyAlertRules(new AlertRules(
                refusalCount != null ? refusalCount : current.refusalCount(),
                weightLossPercent != null ? weightLossPercent : current.weightLossPercent(),
                weightLossDays != null ? weightLossDays : current.weightLossDays(),
                looseStoolCount != null ? looseStoolCount : current.looseStoolCount()));
    }

    private void applyAlertRules(AlertRules rules) {
        this.refusalCount = rules.refusalCount();
        this.weightLossPercent = rules.weightLossPercent();
        this.weightLossDays = rules.weightLossDays();
        this.looseStoolCount = rules.looseStoolCount();
    }
}
