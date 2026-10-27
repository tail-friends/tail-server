package com.tailfriends.animal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.AnimalCharacter;
import com.tailfriends.animal.domain.CharacterLook;
import com.tailfriends.animal.domain.FeedSchedule;
import com.tailfriends.animal.domain.FeedStatus;
import com.tailfriends.animal.domain.Mood;
import com.tailfriends.animal.domain.PatternType;
import com.tailfriends.animal.domain.RenderStyle;
import com.tailfriends.animal.repository.AnimalCharacterRepository;
import com.tailfriends.animal.service.AnimalStatusService.AnimalStatus;
import com.tailfriends.log.domain.ShedStage;
import com.tailfriends.log.repository.LogRepository;
import com.tailfriends.log.repository.ShedEntry;
import com.tailfriends.species.domain.Home;
import com.tailfriends.species.domain.Shape;
import com.tailfriends.species.domain.Species;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AnimalStatusServiceTest {

    // UTC 2026-10-20 16:00 = KST 2026-10-21 01:00
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T16:00:00Z"), ZoneOffset.UTC);
    private static final FeedSchedule UPCOMING = new FeedSchedule(FeedStatus.UPCOMING, null, null, 2);

    @Mock
    private LogRepository logRepository;

    @Mock
    private AnimalCharacterRepository animalCharacterRepository;

    @Test
    void 탈피_체중_캐릭터를_모으고_기분은_한국_날짜로_정한다() {
        Animal animal = animal(species(Home.DESERT));
        given(logRepository.findLatestSheds(eq(12L), any()))
                .willReturn(List.of(new ShedEntry(LocalDate.parse("2026-10-18"), ShedStage.SHED, true)));
        given(logRepository.findLatestWeights(eq(12L), any()))
                .willReturn(List.of(new BigDecimal("57.8"), new BigDecimal("55.0")));
        given(animalCharacterRepository.findById(12L)).willReturn(Optional.of(character(animal)));

        AnimalStatus status = service().statusOf(animal, UPCOMING, false);

        assertThat(status.shed().lastDate()).isEqualTo(LocalDate.parse("2026-10-18"));
        assertThat(status.weight().latest()).isEqualByComparingTo("57.8");
        assertThat(status.weight().diff()).isEqualByComparingTo("2.8");
        assertThat(status.character().shape()).isEqualTo("gecko");
        assertThat(status.character().home()).isEqualTo("desert");
        // KST 10-21 기준 탈피 3일 전이라 NORMAL (UTC 날짜 10-20으로 보면 2일 전이라 JUST_SHED가 됨)
        assertThat(status.mood()).isEqualTo(Mood.NORMAL);
    }

    @Test
    void 기록과_캐릭터가_없으면_요약은_비어_있고_캐릭터는_null() {
        Animal animal = animal(species(Home.DESERT));

        AnimalStatus status = service().statusOf(animal, UPCOMING, false);

        assertThat(status.shed().lastDate()).isNull();
        assertThat(status.weight().latest()).isNull();
        assertThat(status.weight().diff()).isNull();
        assertThat(status.character()).isNull();
        assertThat(status.mood()).isEqualTo(Mood.NORMAL);
    }

    @Test
    void 개체에_지정한_사육장이_없고_직접_입력한_종이면_home은_null() {
        Animal animal = animal(null);
        given(animalCharacterRepository.findById(12L)).willReturn(Optional.of(character(animal)));

        assertThat(service().statusOf(animal, UPCOMING, false).character().home()).isNull();
    }

    private AnimalStatusService service() {
        return new AnimalStatusService(logRepository, animalCharacterRepository, CLOCK);
    }

    private static Species species(Home defaultHome) {
        return Species.builder().name("레오파드 게코").shape(Shape.GECKO).feedIntervalDays(4).defaultHome(defaultHome).build();
    }

    private static Animal animal(Species species) {
        Animal animal = Animal.builder().species(species).speciesName("레오파드 게코").name("망고").feedIntervalDays(4).build();
        ReflectionTestUtils.setField(animal, "id", 12L);
        return animal;
    }

    private static AnimalCharacter character(Animal animal) {
        return AnimalCharacter.of(animal, new CharacterLook(RenderStyle.ILLUSTRATION_2D, Shape.GECKO, PatternType.SPOTS,
                "#E0A33A", "#3A2A1E", "#F2E6C8", "#2B2B2B", null, null, null));
    }
}
