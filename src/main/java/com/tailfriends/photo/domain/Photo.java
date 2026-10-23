package com.tailfriends.photo.domain;

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
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파일은 저장소에 두고 DB에는 저장소 key만 둔다.
 */
@Getter
@Entity
@Table(name = "photos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PhotoPurpose purpose;

    @Column(nullable = false)
    private String thumbnailKey;

    /**
     * 원본 보관에 동의했을 때만 있다.
     */
    private String originalKey;

    @Column(precision = 7, scale = 1)
    private BigDecimal weightAtTime;

    @Builder
    private Photo(User user, Animal animal, PhotoPurpose purpose, String thumbnailKey, String originalKey) {
        this.user = user;
        this.animal = animal;
        this.purpose = purpose;
        this.thumbnailKey = thumbnailKey;
        this.originalKey = originalKey;
    }

    /**
     * 보관한 원본을 지운 뒤 key를 비운다. 썸네일은 남는다.
     */
    public void clearOriginalKey() {
        this.originalKey = null;
    }
}
