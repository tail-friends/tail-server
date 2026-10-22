package com.tailfriends.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalCharacterRepository;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.auth.dto.SampleDataResponse;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.log.repository.LogRepository;
import com.tailfriends.species.domain.Home;
import com.tailfriends.species.domain.Shape;
import com.tailfriends.species.domain.Species;
import com.tailfriends.species.repository.SpeciesRepository;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class DevSampleDataServiceTest {

    // UTC 2026-10-21 15:30 = KST 2026-10-22 00:30
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T15:30:00Z"), ZoneOffset.UTC);
    private static final Long USER_ID = 1L;

    private final UserRepository userRepository = mock(UserRepository.class);
    private final SpeciesRepository speciesRepository = mock(SpeciesRepository.class);
    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final AnimalCharacterRepository characterRepository = mock(AnimalCharacterRepository.class);
    private final LogRepository logRepository = mock(LogRepository.class);
    private final DevSampleDataService service = new DevSampleDataService(userRepository, speciesRepository,
            animalRepository, characterRepository, logRepository, CLOCK);

    @BeforeEach
    void setUp() {
        AtomicLong ids = new AtomicLong(10);
        given(animalRepository.save(any())).willAnswer(invocation -> {
            Animal animal = invocation.getArgument(0);
            ReflectionTestUtils.setField(animal, "id", ids.incrementAndGet());
            return animal;
        });
    }

    @Test
    void 개체_3마리와_캐릭터와_기록을_저장한다() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));
        given(speciesRepository.findByName(anyString())).willReturn(Optional.empty());

        SampleDataResponse response = service.create(USER_ID);

        assertThat(response.animalIds()).containsExactly(11L, 12L, 13L);
        assertThat(response.logCount()).isPositive();
        verify(characterRepository, times(3)).save(any());
        verify(logRepository, times(3)).saveAll(any());
    }

    @Test
    void 해칭일과_메모는_한국_날짜_오늘을_기준으로_정한다() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));
        given(speciesRepository.findByName(anyString())).willReturn(Optional.empty());

        service.create(USER_ID);

        Animal mango = savedAnimals().get(0);
        assertThat(mango.getName()).isEqualTo("망고");
        assertThat(mango.getHatchDate()).isEqualTo(LocalDate.of(2025, 6, 22));
        assertThat(mango.getMemo()).isEqualTo(DevSampleDataService.MEMO);
    }

    @Test
    void 종_목록에_있는_종은_연결하고_없는_종은_이름만_남긴다() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user()));
        given(speciesRepository.findByName(anyString())).willReturn(Optional.empty());
        given(speciesRepository.findByName("레오파드 게코")).willReturn(Optional.of(species("레오파드 게코")));

        service.create(USER_ID);

        List<Animal> animals = savedAnimals();
        assertThat(animals.get(0).getSpecies()).isNotNull();
        assertThat(animals.get(1).getSpecies()).isNull();
        assertThat(animals.get(1).getSpeciesName()).isEqualTo("크레스티드 게코");
    }

    @Test
    void 사용자가_없으면_UNAUTHORIZED() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
        verify(animalRepository, never()).save(any());
    }

    private List<Animal> savedAnimals() {
        ArgumentCaptor<Animal> captor = ArgumentCaptor.forClass(Animal.class);
        verify(animalRepository, times(3)).save(captor.capture());
        return captor.getAllValues();
    }

    private static User user() {
        return User.builder().provider(Provider.DEV).providerId("tester").nickname("tester").build();
    }

    private static Species species(String name) {
        return Species.builder()
                .name(name)
                .shape(Shape.GECKO)
                .feedIntervalDays(4)
                .defaultHome(Home.DESERT)
                .build();
    }
}
