package com.tailfriends.breeding.domain;

import com.tailfriends.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 산란 기록. 고치지 않고 번식 기록을 수정할 때 통째로 지우고 다시 넣는다.
 */
@Getter
@Entity
@Table(name = "clutches")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Clutch extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breeding_id", nullable = false)
    private Breeding breeding;

    @Column(nullable = false)
    private LocalDate laidDate;

    @Column(nullable = false)
    private int eggCount;

    @Column(precision = 4, scale = 1)
    private BigDecimal incubationTemp;

    private LocalDate expectedHatchDate;

    @Builder
    private Clutch(Breeding breeding, LocalDate laidDate, int eggCount, BigDecimal incubationTemp,
                   LocalDate expectedHatchDate) {
        this.breeding = breeding;
        this.laidDate = laidDate;
        this.eggCount = eggCount;
        this.incubationTemp = incubationTemp;
        this.expectedHatchDate = expectedHatchDate;
    }
}
