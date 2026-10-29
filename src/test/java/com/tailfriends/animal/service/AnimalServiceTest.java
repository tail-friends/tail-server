package com.tailfriends.animal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.Sex;
import com.tailfriends.animal.dto.AnimalCoolingRequest;
import com.tailfriends.animal.dto.AnimalCoolingResponse;
import com.tailfriends.animal.dto.AnimalCreateRequest;
import com.tailfriends.animal.dto.AnimalHomeRequest;
import com.tailfriends.animal.dto.AnimalHomeResponse;
import com.tailfriends.animal.dto.AnimalResponse;
import com.tailfriends.animal.dto.AnimalUpdateRequest;
import com.tailfriends.animal.dto.CoolingResponse;
import com.tailfriends.animal.repository.AnimalCharacterRepository;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.health.service.HealthAlertService;
import com.tailfriends.log.repository.LogRepository;
import com.tailfriends.photo.service.PhotoFileRemover;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AnimalServiceTest {

    // UTC 2026-10-19 15:30 = KST 2026-10-20 00:30
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T15:30:00Z"), ZoneOffset.UTC);
    private static final Long USER_ID = 1L;

    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final SpeciesRepository speciesRepository = mock(SpeciesRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final LogRepository logRepository = mock(LogRepository.class);
    private final PhotoFileRemover photoFileRemover = mock(PhotoFileRemover.class);
    private final AnimalService animalService = new AnimalService(CurrentUserStub.of(USER_ID), animalRepository,
            speciesRepository, userRepository, new FeedService(logRepository, CLOCK), mock(HealthAlertService.class),
            new AnimalStatusService(logRepository, mock(AnimalCharacterRepository.class), CLOCK), CLOCK,
            photoFileRemover);

    @Test
    void 종_ID로_등록하면_종_이름을_종_목록에서_가져온다() {
        given(speciesRepository.findById(1L)).willReturn(Optional.of(species("레오파드 게코")));
        given(animalRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 12L));

        Long id = animalService.create(createRequest(1L, "무시되는 이름", null)).id();

        Animal saved = captureSaved();
        assertThat(id).isEqualTo(12L);
        assertThat(saved.getSpeciesName()).isEqualTo("레오파드 게코");
        assertThat(saved.getSpecies()).isNotNull();
        assertThat(saved.getSex()).isEqualTo(Sex.UNKNOWN);
    }

    @Test
    void 목록에_없는_종은_종_이름만으로_등록한다() {
        given(animalRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 12L));

        animalService.create(createRequest(null, " 사바나 모니터 ", null));

        Animal saved = captureSaved();
        assertThat(saved.getSpecies()).isNull();
        assertThat(saved.getSpeciesName()).isEqualTo("사바나 모니터");
    }

    @Test
    void 종_ID와_종_이름이_모두_없으면_등록하지_않는다() {
        assertInvalidInput(() -> animalService.create(createRequest(null, " ", null)));
        verify(animalRepository, never()).save(any());
    }

    @Test
    void 없는_종_ID로는_등록하지_않는다() {
        given(speciesRepository.findById(99L)).willReturn(Optional.empty());

        assertInvalidInput(() -> animalService.create(createRequest(99L, null, null)));
    }

    @Test
    void 해칭일은_한국_날짜로_오늘까지_허용한다() {
        given(animalRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 12L));

        animalService.create(createRequest(null, "게코", LocalDate.of(2026, 10, 20)));

        assertThat(captureSaved().getHatchDate()).isEqualTo(LocalDate.of(2026, 10, 20));
    }

    @Test
    void 해칭일이_한국_날짜로_내일이면_등록하지_않는다() {
        assertInvalidInput(() -> animalService.create(createRequest(null, "게코", LocalDate.of(2026, 10, 21))));
    }

    @Test
    void 남의_개체나_없는_개체는_ANIMAL_NOT_FOUND() {
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> animalService.getAnimal(12L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ANIMAL_NOT_FOUND);
    }

    @Test
    void 수정은_보낸_필드만_바꾼다() {
        Animal animal = animal();
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.of(animal));

        AnimalResponse response = animalService.update(12L,
                new AnimalUpdateRequest(null, null, null, null, null, null, 5, "성체가 되어 주기 변경"));

        assertThat(response.feedIntervalDays()).isEqualTo(5);
        assertThat(response.memo()).isEqualTo("성체가 되어 주기 변경");
        assertThat(response.name()).isEqualTo("망고");
        assertThat(response.morph()).isEqualTo("탠저린");
        assertThat(response.sex()).isEqualTo(Sex.FEMALE);
        assertThat(response.species()).isEqualTo("레오파드 게코");
    }

    @Test
    void 종_이름만_보내_수정하면_종_목록과의_연결을_끊는다() {
        Animal animal = animal();
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.of(animal));

        animalService.update(12L,
                new AnimalUpdateRequest(null, null, "사바나 모니터", null, null, null, null, null));

        assertThat(animal.getSpecies()).isNull();
        assertThat(animal.getSpeciesName()).isEqualTo("사바나 모니터");
    }

    @Test
    void 평소_사육장을_바꾸면_소문자로_돌려준다() {
        Animal animal = animal();
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.of(animal));

        AnimalHomeResponse response = animalService.changeHome(12L, new AnimalHomeRequest(Home.FOREST));

        assertThat(animal.getHome()).isEqualTo(Home.FOREST);
        assertThat(response.home()).isEqualTo("forest");
    }

    @Test
    void 남의_개체나_없는_개체의_평소_사육장은_바꾸지_않는다() {
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> animalService.changeHome(12L, new AnimalHomeRequest(Home.FOREST)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ANIMAL_NOT_FOUND);
    }

    @Test
    void 쿨링_기간을_정하면_그대로_돌려주고_종료일이_없으면_계속이다() {
        Animal animal = animal();
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.of(animal));

        AnimalCoolingResponse response = animalService.changeCooling(12L,
                new AnimalCoolingRequest(LocalDate.of(2026, 12, 1), LocalDate.of(2027, 2, 15)));
        assertThat(response.cooling()).isEqualTo(
                new CoolingResponse(LocalDate.of(2026, 12, 1), LocalDate.of(2027, 2, 15)));

        AnimalCoolingResponse endless = animalService.changeCooling(12L,
                new AnimalCoolingRequest(LocalDate.of(2026, 12, 1), null));
        assertThat(endless.cooling().endDate()).isNull();
        assertThat(animal.getCoolingEnd()).isNull();
    }

    @Test
    void 시작일을_비워_보내면_쿨링을_끄고_null을_돌려준다() {
        Animal animal = animal();
        animal.changeCooling(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31));
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.of(animal));

        AnimalCoolingResponse response = animalService.changeCooling(12L, new AnimalCoolingRequest(null, null));

        assertThat(response.cooling()).isNull();
        assertThat(animal.getCoolingStart()).isNull();
        assertThat(animal.getCoolingEnd()).isNull();
    }

    @Test
    void 종료일이_시작일보다_빠르거나_종료일만_보내면_쿨링을_바꾸지_않는다() {
        Animal animal = animal();
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.of(animal));

        assertInvalidInput(() -> animalService.changeCooling(12L,
                new AnimalCoolingRequest(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 30))));
        assertInvalidInput(() -> animalService.changeCooling(12L,
                new AnimalCoolingRequest(null, LocalDate.of(2026, 12, 31))));
        assertThat(animal.getCoolingStart()).isNull();
    }

    @Test
    void 시작일과_종료일이_같은_하루짜리_쿨링도_된다() {
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.of(animal()));

        AnimalCoolingResponse response = animalService.changeCooling(12L,
                new AnimalCoolingRequest(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 1)));

        assertThat(response.cooling().startDate()).isEqualTo(response.cooling().endDate());
    }

    @Test
    void 남의_개체나_없는_개체의_쿨링은_바꾸지_않는다() {
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> animalService.changeCooling(12L, new AnimalCoolingRequest(null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ANIMAL_NOT_FOUND);
    }

    @Test
    void 삭제는_내_개체만_지우고_사진_파일도_지운다() {
        Animal animal = animal();
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.of(animal));

        animalService.delete(12L);

        verify(animalRepository).delete(animal);
        verify(photoFileRemover).removeFilesOfAnimalAfterCommit(12L);
    }

    private Animal captureSaved() {
        ArgumentCaptor<Animal> captor = ArgumentCaptor.forClass(Animal.class);
        verify(animalRepository).save(captor.capture());
        return captor.getValue();
    }

    private static void assertInvalidInput(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    private static AnimalCreateRequest createRequest(Long speciesId, String speciesName, LocalDate hatchDate) {
        return new AnimalCreateRequest("망고", speciesId, speciesName, null, null, hatchDate, 4, null);
    }

    private static Animal withId(Animal animal, Long id) {
        ReflectionTestUtils.setField(animal, "id", id);
        return animal;
    }

    private static Species species(String name) {
        return Species.builder()
                .name(name)
                .shape(Shape.GECKO)
                .feedIntervalDays(4)
                .defaultHome(Home.DESERT)
                .build();
    }

    private static Animal animal() {
        return withId(Animal.builder()
                .user(User.builder().provider(Provider.KAKAO).providerId("kakao-1").nickname("사용자").build())
                .species(species("레오파드 게코"))
                .speciesName("레오파드 게코")
                .name("망고")
                .morph("탠저린")
                .sex(Sex.FEMALE)
                .feedIntervalDays(4)
                .build(), 12L);
    }
}
