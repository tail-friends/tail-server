package com.tailfriends.animal.domain;

import com.tailfriends.global.domain.BaseTimeEntity;
import com.tailfriends.species.domain.Shape;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 개체의 캐릭터. 개체와 1:1이고 PK가 animal_id다. (java.lang.Character와 겹치지 않게 AnimalCharacter)
 */
@Getter
@Entity
@Table(name = "characters")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnimalCharacter extends BaseTimeEntity {

    @Id
    private Long animalId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id")
    private Animal animal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RenderStyle renderStyle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Shape shape;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PatternType patternType;

    @Column(nullable = false, length = 7)
    private String colorBody;

    @Column(nullable = false, length = 7)
    private String colorPattern;

    @Column(nullable = false, length = 7)
    private String colorBelly;

    @Column(nullable = false, length = 7)
    private String colorEye;

    @Column(precision = 3, scale = 2)
    private BigDecimal headSize;

    @Column(precision = 3, scale = 2)
    private BigDecimal tailWidth;

    @Column(precision = 3, scale = 2)
    private BigDecimal patternDensity;

    private AnimalCharacter(Animal animal) {
        this.animal = animal;
    }

    public static AnimalCharacter of(Animal animal, CharacterLook look) {
        AnimalCharacter character = new AnimalCharacter(animal);
        character.change(look);
        return character;
    }

    /**
     * 값 전체를 바꾼다. (PUT) renderStyle이 없으면 ILLUSTRATION_2D, 색은 대문자로 맞춘다.
     */
    public void change(CharacterLook look) {
        this.renderStyle = look.renderStyle() != null ? look.renderStyle() : RenderStyle.ILLUSTRATION_2D;
        this.shape = look.shape();
        this.patternType = look.patternType();
        this.colorBody = look.colorBody().toUpperCase();
        this.colorPattern = look.colorPattern().toUpperCase();
        this.colorBelly = look.colorBelly().toUpperCase();
        this.colorEye = look.colorEye().toUpperCase();
        this.headSize = look.headSize();
        this.tailWidth = look.tailWidth();
        this.patternDensity = look.patternDensity();
    }
}
