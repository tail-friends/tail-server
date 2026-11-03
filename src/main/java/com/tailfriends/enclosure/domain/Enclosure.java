package com.tailfriends.enclosure.domain;

import com.tailfriends.global.domain.BaseTimeEntity;
import com.tailfriends.species.domain.Home;
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
 * 사육장. 기본 테마 사육장은 사용자마다 테마 하나에 하나이고,
 * 중복 없이 만들려고 EnclosureRepository.insertBuiltInIfAbsent로 넣는다.
 */
@Getter
@Entity
@Table(name = "enclosures")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enclosure extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BackgroundType backgroundType;

    /**
     * 기본 테마면 그 테마. 사진 테마면 null.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Home theme;
}
