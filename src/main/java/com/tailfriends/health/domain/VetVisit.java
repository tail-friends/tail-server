package com.tailfriends.health.domain;

import com.tailfriends.animal.domain.Animal;
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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 진료 기록. 방문일만 필수다. 날짜 규칙(방문일은 오늘까지, 다음 방문일은 방문일보다 늦게)은 서비스가 확인한다.
 */
@Getter
@Entity
@Table(name = "vet_visits")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VetVisit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @Column(length = 40)
    private String hospital;

    @Column(nullable = false)
    private LocalDate visitDate;

    @Column(length = 200)
    private String symptom;

    @Column(length = 200)
    private String diagnosis;

    @Column(length = 200)
    private String prescription;

    private LocalDate nextVisitDate;

    @Builder
    private VetVisit(Animal animal, String hospital, LocalDate visitDate, String symptom, String diagnosis,
                     String prescription, LocalDate nextVisitDate) {
        this.animal = animal;
        this.hospital = hospital;
        this.visitDate = visitDate;
        this.symptom = symptom;
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.nextVisitDate = nextVisitDate;
    }

    /**
     * null인 값은 바꾸지 않는다.
     */
    public void update(String hospital, LocalDate visitDate, String symptom, String diagnosis, String prescription,
                       LocalDate nextVisitDate) {
        if (hospital != null) {
            this.hospital = hospital;
        }
        if (visitDate != null) {
            this.visitDate = visitDate;
        }
        if (symptom != null) {
            this.symptom = symptom;
        }
        if (diagnosis != null) {
            this.diagnosis = diagnosis;
        }
        if (prescription != null) {
            this.prescription = prescription;
        }
        if (nextVisitDate != null) {
            this.nextVisitDate = nextVisitDate;
        }
    }
}
