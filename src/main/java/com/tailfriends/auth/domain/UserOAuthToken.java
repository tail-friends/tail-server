package com.tailfriends.auth.domain;

import com.tailfriends.global.domain.BaseTimeEntity;
import com.tailfriends.user.domain.Provider;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 소셜 쪽 refresh token. 애플 로그인 사용자가 탈퇴할 때 애플 토큰을 철회하려고 둔다.
 * refreshToken은 OAuthTokenCipher로 암호화한 값이다. 원문을 이 엔티티에 담지 않는다.
 */
@Getter
@Entity
@Table(name = "user_oauth_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserOAuthToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider;

    @Column(nullable = false, columnDefinition = "text")
    private String refreshToken;

    private UserOAuthToken(User user, Provider provider, String encryptedRefreshToken) {
        this.user = user;
        this.provider = provider;
        this.refreshToken = encryptedRefreshToken;
    }

    public static UserOAuthToken of(User user, Provider provider, String encryptedRefreshToken) {
        return new UserOAuthToken(user, provider, encryptedRefreshToken);
    }

    /**
     * 로그인할 때마다 소셜 서버가 새 토큰을 주므로 바꾼다.
     */
    public void change(String encryptedRefreshToken) {
        this.refreshToken = encryptedRefreshToken;
    }
}
