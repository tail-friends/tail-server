package com.tailfriends.notification.domain;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기기의 푸시 토큰 (F-08). 토큰은 전체에서 유일하다.
 */
@Getter
@Entity
@Table(name = "push_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PushPlatform platform;

    private PushToken(User user, String token, PushPlatform platform) {
        this.user = user;
        this.token = token;
        this.platform = platform;
    }

    public static PushToken of(User user, String token, PushPlatform platform) {
        return new PushToken(user, token, platform);
    }

    /**
     * 같은 기기가 다른 사용자로 로그인해 다시 등록한 경우.
     */
    public void moveTo(User user, PushPlatform platform) {
        this.user = user;
        this.platform = platform;
    }
}
