package com.tailfriends.schedule.domain;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.global.domain.BaseTimeEntity;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 반복 일정. animal이 null이면 모든 개체 공통 일정. nextDate가 null이면 endDate가 지나 끝난 일정이다.
 */
@Getter
@Entity
@Table(name = "schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id")
    private Animal animal;

    @Column(nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RepeatType repeatType;

    // 기본 이름 규칙은 끝 대문자 앞에 _를 넣지 않아 repeatn이 된다
    @Column(name = "repeat_n")
    private Integer repeatN;

    @Convert(converter = WeekdaysConverter.class)
    @Column(length = 27)
    private Set<Weekday> repeatDays = EnumSet.noneOf(Weekday.class);

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate nextDate;

    private LocalDate lastDoneDate;

    private LocalDate endDate;

    private Schedule(User user, Animal animal, String name, ScheduleKind kind, Repeat repeat,
                     LocalDate startDate, LocalDate endDate) {
        this.user = user;
        this.animal = animal;
        this.name = name;
        this.kind = kind;
        changeRepeat(repeat);
        this.startDate = startDate;
        this.endDate = endDate;
        validateDates();
        this.nextDate = repeat.firstOnOrAfter(startDate);
        closeIfEnded();
    }

    /**
     * nextDate는 startDate부터 첫 일정일이다.
     *
     * @throws BusinessException endDate가 startDate보다 앞이면 INVALID_INPUT
     */
    public static Schedule create(User user, Animal animal, String name, ScheduleKind kind, Repeat repeat,
                                  LocalDate startDate, LocalDate endDate) {
        return new Schedule(user, animal, name, kind, repeat, startDate, endDate);
    }

    public Repeat repeat() {
        return new Repeat(repeatType, repeatN, repeatDays);
    }

    /**
     * 완료한 날 기준으로 다음 일정일을 정한다.
     *
     * @throws BusinessException 끝난 일정이거나 시작일 전 날짜면 INVALID_INPUT
     */
    public void complete(LocalDate date) {
        if (nextDate == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 끝난 일정이에요");
        }
        if (date.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "시작일 전 날짜로 완료할 수 없어요");
        }
        lastDoneDate = date;
        nextDate = repeat().nextAfter(date);
        closeIfEnded();
    }

    /**
     * null인 값은 바꾸지 않는다. 반복·시작일·종료일이 바뀌면 다음 일정일을 다시 계산한다
     * (완료한 적이 있으면 마지막 완료일 기준, 아니면 시작일 기준).
     *
     * @throws BusinessException endDate가 startDate보다 앞이면 INVALID_INPUT
     */
    public void update(String name, ScheduleKind kind, Repeat repeat, LocalDate startDate, LocalDate endDate) {
        if (name != null) {
            this.name = name;
        }
        if (kind != null) {
            this.kind = kind;
        }
        if (repeat == null && startDate == null && endDate == null) {
            return;
        }
        if (repeat != null) {
            changeRepeat(repeat);
        }
        if (startDate != null) {
            this.startDate = startDate;
        }
        if (endDate != null) {
            this.endDate = endDate;
        }
        validateDates();
        boolean doneSinceStart = lastDoneDate != null && !lastDoneDate.isBefore(this.startDate);
        nextDate = doneSinceStart ? repeat().nextAfter(lastDoneDate) : repeat().firstOnOrAfter(this.startDate);
        closeIfEnded();
    }

    private void changeRepeat(Repeat repeat) {
        this.repeatType = repeat.type();
        this.repeatN = repeat.n();
        this.repeatDays = repeat.days().isEmpty() ? EnumSet.noneOf(Weekday.class) : EnumSet.copyOf(repeat.days());
    }

    private void validateDates() {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "종료일은 시작일 이후여야 해요");
        }
    }

    private void closeIfEnded() {
        if (endDate != null && nextDate != null && nextDate.isAfter(endDate)) {
            nextDate = null;
        }
    }
}
