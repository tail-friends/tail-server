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
     * 평소 사육장. null이면 종의 default_home을 쓴다.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Home home;

    private LocalDate coolingStart;

    private LocalDate coolingEnd;

    @Builder
    private Animal(User user, Species species, String speciesName, String name, String morph, Sex sex,
                   LocalDate hatchDate, int feedIntervalDays, String memo) {
        this.user = user;
        this.species = species;
        this.speciesName = speciesName;
        this.name = name;
        this.morph = morph;
        this.sex = sex == null ? Sex.UNKNOWN : sex;
        this.hatchDate = hatchDate;
        this.feedIntervalDays = feedIntervalDays;
        this.memo = memo;
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
