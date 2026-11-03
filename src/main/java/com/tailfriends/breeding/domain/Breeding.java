package com.tailfriends.breeding.domain;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.global.domain.BaseTimeEntity;
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

/**
 * 번식 기록. 부모가 내 개체면 male·female이 있고, 외부 개체거나 개체를 지웠으면 null이다(DB ON DELETE SET NULL).
 * 이름은 항상 저장해서 개체가 없어도 남는다. 날짜 규칙은 서비스가 확인한다.
 */
@Getter
@Entity
@Table(name = "breedings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Breeding extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "male_id")
    private Animal male;

    @Column(nullable = false, length = 30)
    private String maleName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "female_id")
    private Animal female;

    @Column(nullable = false, length = 30)
    private String femaleName;

    @Column(nullable = false)
    private LocalDate pairedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private BreedingStatus status;

    @Column(length = 200)
    private String memo;

    @Builder
    private Breeding(User user, Animal male, String maleName, Animal female, String femaleName, LocalDate pairedDate,
                     String memo) {
        this.user = user;
        this.male = male;
        this.maleName = maleName;
        this.female = female;
        this.femaleName = femaleName;
        this.pairedDate = pairedDate;
        this.status = BreedingStatus.PAIRED;
        this.memo = memo;
    }

    public void changeStatus(BreedingStatus status) {
        this.status = status;
    }

    /**
     * 내 개체면 지금 개체 이름, 아니면 저장한 이름.
     */
    public String currentMaleName() {
        return male != null ? male.getName() : maleName;
    }

    /**
     * 내 개체면 지금 개체 이름, 아니면 저장한 이름.
     */
    public String currentFemaleName() {
        return female != null ? female.getName() : femaleName;
    }
}
