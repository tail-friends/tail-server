package com.tailfriends.photo.domain;

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
 * AI 분석 1회. success는 AI가 답을 줬는지다. (사진에 파충류가 없다는 답도 성공)
 * AI를 부르기 전에 성공으로 미리 기록해 하루 횟수를 잡아 두고, 실패하면 fail()로 바꾼다.
 */
@Getter
@Entity
@Table(name = "ai_usages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiUsage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiKind kind;

    @Column(nullable = false)
    private boolean success;

    private AiUsage(User user, AiKind kind, boolean success) {
        this.user = user;
        this.kind = kind;
        this.success = success;
    }

    public static AiUsage of(User user, AiKind kind, boolean success) {
        return new AiUsage(user, kind, success);
    }

    public void fail() {
        this.success = false;
    }
}
