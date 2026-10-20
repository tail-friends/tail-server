package com.tailfriends.species.domain;

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

@Getter
@Entity
@Table(name = "species_guides")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpeciesGuide extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_id", nullable = false)
    private Species species;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GuideSection section;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    private SpeciesGuide(Species species, GuideSection section, String body) {
        this.species = species;
        this.section = section;
        this.body = body;
    }

    public static SpeciesGuide of(Species species, GuideSection section, String body) {
        return new SpeciesGuide(species, section, body);
    }

    /**
     * 내용이 같으면 바뀌지 않아 수정 날짜도 그대로다.
     */
    public void changeBody(String body) {
        this.body = body;
    }
}
