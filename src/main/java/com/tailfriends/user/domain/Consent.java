package com.tailfriends.user.domain;

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

/**
 * 동의·철회 이력. 바꾸지 않고 새 행으로 남긴다.
 */
@Getter
@Entity
@Table(name = "consents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Consent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConsentType type;

    @Column(nullable = false)
    private boolean agreed;

    private Consent(User user, ConsentType type, boolean agreed) {
        this.user = user;
        this.type = type;
        this.agreed = agreed;
    }

    public static Consent of(User user, ConsentType type, boolean agreed) {
        return new Consent(user, type, agreed);
    }
}
