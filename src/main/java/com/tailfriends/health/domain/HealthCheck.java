package com.tailfriends.health.domain;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.global.domain.BaseTimeEntity;
import com.tailfriends.photo.domain.Photo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 사진 건강 체크 결과 (F-33). 체크한 시각은 created_at이다.
 */
@Getter
@Entity
@Table(name = "health_checks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthCheck extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    /**
     * 사진이 지워지면 null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_id")
    private Photo photo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<HealthCheckItem> items;

    private HealthCheck(Animal animal, Photo photo, List<HealthCheckItem> items) {
        this.animal = animal;
        this.photo = photo;
        this.items = List.copyOf(items);
    }

    public static HealthCheck of(Animal animal, Photo photo, List<HealthCheckItem> items) {
        return new HealthCheck(animal, photo, items);
    }

    public int attentionCount() {
        return (int) items.stream().filter(HealthCheckItem::needsAttention).count();
    }
}
