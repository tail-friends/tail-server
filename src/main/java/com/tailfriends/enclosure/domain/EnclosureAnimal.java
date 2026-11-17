package com.tailfriends.enclosure.domain;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.global.domain.BaseTimeEntity;
import jakarta.persistence.Entity;
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
 * 합사 보기(F-29)로 사육장 화면에 함께 띄울 개체. 화면에서만 함께 보이는 기능이다.
 * 고치지 않고 설정할 때 통째로 지우고 다시 넣는다. id 순서가 저장한 순서다.
 */
@Getter
@Entity
@Table(name = "enclosure_animals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnclosureAnimal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enclosure_id", nullable = false)
    private Enclosure enclosure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    private EnclosureAnimal(Enclosure enclosure, Animal animal) {
        this.enclosure = enclosure;
        this.animal = animal;
    }

    public static EnclosureAnimal of(Enclosure enclosure, Animal animal) {
        return new EnclosureAnimal(enclosure, animal);
    }
}
