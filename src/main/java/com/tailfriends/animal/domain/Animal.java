package com.tailfriends.animal.domain;

import com.tailfriends.global.domain.BaseTimeEntity;
import com.tailfriends.species.domain.Home;
import com.tailfriends.species.domain.Species;
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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "animals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Animal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 목록에 없는 종이면 null. 이때는 speciesName만 있다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_id")
    private Species species;

    /**
     * 직접 입력한 종 이름, 또는 등록 당시 종 이름.
     */
    @Column(nullable = false, length = 40)
    private String speciesName;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(length = 40)
    private String morph;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Sex sex;

    private LocalDate hatchDate;

    @Column(nullable = false)
    private int feedIntervalDays;

    @Column(length = 300)
    private String memo;

    /**
     * 평소 사육장. home과 enclosureId가 모두 null이면 종의 default_home을 쓴다.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Home home;

    /**
     * 평소 사육장이 사진 테마면 그 사육장 ID. 이때 home은 null.
     * 사진 테마를 지우면 null(DB ON DELETE SET NULL)이 되어 종의 default_home으로 돌아간다.
     */
    private Long enclosureId;

    private LocalDate coolingStart;

    private LocalDate coolingEnd;

    /**
     * 해칭 개체 등록으로 만든 개체면 그 번식 기록 ID. 번식 기록을 지우면 null(DB ON DELETE SET NULL).
     */
    private Long breedingId;

    @Builder
    private Animal(User user, Species species, String speciesName, String name, String morph, Sex sex,
                   LocalDate hatchDate, int feedIntervalDays, String memo, Long breedingId) {
        this.user = user;
        this.species = species;
        this.speciesName = speciesName;
        this.name = name;
        this.morph = morph;
        this.sex = sex == null ? Sex.UNKNOWN : sex;
        this.hatchDate = hatchDate;
        this.feedIntervalDays = feedIntervalDays;
        this.memo = memo;
        this.breedingId = breedingId;
    }

    public void changeHome(Home home) {
        this.home = home;
        this.enclosureId = null;
    }

    public void changeHomeToPhotoTheme(Long enclosureId) {
        this.home = null;
        this.enclosureId = enclosureId;
    }

    /**
     * 쿨링 기간을 정한다. coolingEnd가 null이면 끝나는 날 없이 계속이다. 둘 다 null이면 쿨링을 끈다.
     */
    public void changeCooling(LocalDate coolingStart, LocalDate coolingEnd) {
        this.coolingStart = coolingStart;
        this.coolingEnd = coolingEnd;
    }

    public void changeSpecies(Species species, String speciesName) {
        this.species = species;
        this.speciesName = speciesName;
    }

    /**
     * null인 값은 바꾸지 않는다.
     */
    public void update(String name, String morph, Sex sex, LocalDate hatchDate, Integer feedIntervalDays, String memo) {
        if (name != null) {
            this.name = name;
        }
        if (morph != null) {
            this.morph = morph;
        }
        if (sex != null) {
            this.sex = sex;
        }
        if (hatchDate != null) {
            this.hatchDate = hatchDate;
        }
        if (feedIntervalDays != null) {
            this.feedIntervalDays = feedIntervalDays;
        }
        if (memo != null) {
            this.memo = memo;
        }
    }
}
