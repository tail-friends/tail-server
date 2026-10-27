package com.tailfriends.auth.domain;

import com.tailfriends.global.domain.BaseTimeEntity;
import com.tailfriends.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    /**
     * 토큰 묶음. 로그인할 때 새로 만들고, 재발급한 토큰은 쓴 토큰의 묶음을 이어받는다.
     */
    @Column(nullable = false)
    private UUID familyId;

    /**
     * 재발급에 쓴 시각. null이면 아직 쓰지 않았다.
     */
    private Instant usedAt;

    @Builder
    private RefreshToken(User user, String tokenHash, Instant expiresAt, UUID familyId) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.familyId = familyId;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * 쓴 지 grace가 지나지 않았으면 true. 앱의 재시도나 동시 요청으로 보고 도난으로 보지 않는다.
     */
    public boolean isUsedWithin(Duration grace, Instant now) {
        return isUsed() && now.isBefore(usedAt.plus(grace));
    }

    /**
     * 리프레시 토큰은 원문 대신 이 SHA-256 해시(16진수 64자)로 저장하고 조회한다.
     */
    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
