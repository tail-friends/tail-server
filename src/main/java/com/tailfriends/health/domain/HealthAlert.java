package com.tailfriends.health.domain;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.global.domain.BaseTimeEntity;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "health_alerts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthAlert extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertType type;

    @Column(nullable = false, length = 100)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AlertStatus status;

    private HealthAlert(Animal animal, AlertType type, String message) {
        this.animal = animal;
        this.type = type;
        this.message = message;
        this.status = AlertStatus.OPEN;
    }

    public static HealthAlert open(Animal animal, AlertType type, String message) {
        return new HealthAlert(animal, type, message);
    }

    public void acknowledge() {
        this.status = AlertStatus.ACKNOWLEDGED;
    }
}
