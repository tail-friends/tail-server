package com.tailfriends.breeding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.breeding.domain.Breeding;
import com.tailfriends.breeding.domain.BreedingStatus;
import com.tailfriends.breeding.domain.Clutch;
import com.tailfriends.breeding.dto.BreedingCreateRequest;
import com.tailfriends.breeding.dto.BreedingResponse;
import com.tailfriends.breeding.dto.BreedingUpdateRequest;
import com.tailfriends.breeding.dto.ClutchRequest;
import com.tailfriends.breeding.dto.ClutchResponse;
import com.tailfriends.breeding.dto.HatchlingsCreateRequest;
import com.tailfriends.breeding.dto.HatchlingsCreateRequest.Hatchling;
import com.tailfriends.breeding.repository.BreedingRepository;
import com.tailfriends.breeding.repository.ClutchRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.species.domain.Species;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

class BreedingServiceTest {

    // UTC 2026-10-19 15:30 = KST 2026-10-20 00:30
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T15:30:00Z"), ZoneOffset.UTC);
    private static final LocalDate KST_TODAY = LocalDate.of(2026, 10, 20);
    private static final LocalDate PAIRED_DATE = LocalDate.of(2026, 8, 1);
    private static final Long USER_ID = 1L;
    private static final User USER = withId(
            User.builder().provider(Provider.KAKAO).providerId("kakao-1").nickname("사용자").build(), USER_ID);

    private final BreedingRepository breedingRepository = mock(BreedingRepository.class);
    private final ClutchRepository clutchRepository = mock(ClutchRepository.class);
    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final BreedingService service = new BreedingService(CurrentUserStub.of(USER_ID), breedingRepository,
            clutchRepository, animalRepository, userRepository, CLOCK);

    @Test
    void 내_개체는_개체와_이름을_외부_개체는_이름만_저장하고_PAIRED로_시작한다() {
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.of(animal(12L, "레몬")));
        given(userRepository.getReferenceById(USER_ID)).willReturn(USER);
        given(breedingRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 4L));

        // 오늘(KST)까지 메이팅일로 적을 수 있다. 서버 시간(UTC)으로는 아직 10-19다
        Long id = service.create(new BreedingCreateRequest(null, " 분양처 수컷 ", 12L, "무시되는 이름", KST_TODAY,
                "첫 메이팅")).id();

        assertThat(id).isEqualTo(4L);
        ArgumentCaptor<Breeding> captor = ArgumentCaptor.forClass(Breeding.class);
        verify(breedingRepository).save(captor.capture());
        Breeding saved = captor.getValue();
        assertThat(saved.getMale()).isNull();
        assertThat(saved.getMaleName()).isEqualTo("분양처 수컷");
        assertThat(saved.getFemale().getId()).isEqualTo(12L);
        assertThat(saved.getFemaleName()).isEqualTo("레몬");
        assertThat(saved.getPairedDate()).isEqualTo(KST_TODAY);
        assertThat(saved.getStatus()).isEqualTo(BreedingStatus.PAIRED);
        assertThat(saved.getMemo()).isEqualTo("첫 메이팅");
    }

    @Test
    void 메이팅일이_오늘_이후면_INVALID_INPUT() {
        assertInvalidInput(() -> service.create(
                new BreedingCreateRequest(null, "수컷", null, "암컷", KST_TODAY.plusDays(1), null)));
        verify(breedingRepository, never()).save(any());
    }

    @Test
    void 같은_개체를_암수로_고르면_INVALID_INPUT() {
        assertInvalidInput(() -> service.create(new BreedingCreateRequest(12L, null, 12L, null, PAIRED_DATE, null)));
        verify(breedingRepository, never()).save(any());
    }

    @Test
    void 암수_중_개체도_이름도_없으면_INVALID_INPUT() {
        assertInvalidInput(() -> service.create(new BreedingCreateRequest(null, "수컷", null, " ", PAIRED_DATE, null)));
        verify(breedingRepository, never()).save(any());
    }

    @Test
    void 남의_개체를_부모로_고르면_ANIMAL_NOT_FOUND() {
        given(animalRepository.findByIdAndUserId(99L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new BreedingCreateRequest(99L, null, null, "암컷", PAIRED_DATE, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ANIMAL_NOT_FOUND);
        verify(breedingRepository, never()).save(any());
    }

    @Test
    void 목록은_산란_기록과_해칭_개체를_번식_기록별로_묶고_내_개체는_지금_이름을_보여준다() {
        Animal female = animal(12L, "레몬");
        Breeding first = breeding(4L, null, "분양처 수컷", female, "옛 이름");
        Breeding second = breeding(3L, null, "수컷", null, "지운 암컷");
        given(breedingRepository.findAllWithParentsByUserId(USER_ID)).willReturn(List.of(first, second));
        given(clutchRepository.findAllByBreedingIdInOrderByLaidDateAscIdAsc(List.of(4L, 3L))).willReturn(List.of(
                clutch(first, LocalDate.of(2026, 9, 1), 2), clutch(first, LocalDate.of(2026, 9, 20), 1)));
        given(animalRepository.findAllByBreedingIdInOrderByIdAsc(List.of(4L, 3L)))
                .willReturn(List.of(hatchling(31L, 3L), hatchling(32L, 3L)));

        List<BreedingResponse> breedings = service.getBreedings().breedings();

        assertThat(breedings).extracting(BreedingResponse::id).containsExactly(4L, 3L);
        BreedingResponse withClutches = breedings.get(0);
        assertThat(withClutches.maleId()).isNull();
        assertThat(withClutches.maleName()).isEqualTo("분양처 수컷");
        assertThat(withClutches.femaleId()).isEqualTo(12L);
        assertThat(withClutches.femaleName()).isEqualTo("레몬");
        assertThat(withClutches.clutches()).extracting(ClutchResponse::eggCount).containsExactly(2, 1);
        assertThat(withClutches.hatchlingIds()).isEmpty();
        BreedingResponse withHatchlings = breedings.get(1);
        assertThat(withHatchlings.femaleId()).isNull();
        assertThat(withHatchlings.femaleName()).isEqualTo("지운 암컷");
        assertThat(withHatchlings.clutches()).isEmpty();
        assertThat(withHatchlings.hatchlingIds()).containsExactly(31L, 32L);
    }

    @Test
    void 번식_기록이_없으면_빈_목록이고_산란_기록을_조회하지_않는다() {
        given(breedingRepository.findAllWithParentsByUserId(USER_ID)).willReturn(List.of());

        assertThat(service.getBreedings().breedings()).isEmpty();
        verify(clutchRepository, never()).findAllByBreedingIdInOrderByLaidDateAscIdAsc(any());
    }

    @Test
    void clutches를_보내면_기존_산란_기록을_지운_뒤_보낸_목록으로_바꾸고_상태도_바꾼다() {
        Breeding breeding = breeding(4L, null, "수컷", null, "암컷");
        given(breedingRepository.findByIdAndUserId(4L, USER_ID)).willReturn(Optional.of(breeding));
        ClutchRequest clutch = new ClutchRequest(KST_TODAY, 2, new BigDecimal("30.0"), KST_TODAY.plusDays(55));

        BreedingResponse response = service.update(4L,
                new BreedingUpdateRequest(BreedingStatus.INCUBATING, List.of(clutch)));

        InOrder order = inOrder(clutchRepository);
        order.verify(clutchRepository).deleteAllByBreedingId(4L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Clutch>> captor = ArgumentCaptor.forClass(List.class);
        order.verify(clutchRepository).saveAll(captor.capture());
        Clutch saved = captor.getValue().getFirst();
        assertThat(saved.getBreeding()).isSameAs(breeding);
        assertThat(saved.getLaidDate()).isEqualTo(KST_TODAY);
        assertThat(saved.getEggCount()).isEqualTo(2);
        assertThat(saved.getIncubationTemp()).isEqualByComparingTo("30.0");
        assertThat(saved.getExpectedHatchDate()).isEqualTo(KST_TODAY.plusDays(55));
        assertThat(response.status()).isEqualTo(BreedingStatus.INCUBATING);
    }

    @Test
    void clutches를_보내지_않으면_산란_기록은_그대로다() {
        Breeding breeding = breeding(4L, null, "수컷", null, "암컷");
        given(breedingRepository.findByIdAndUserId(4L, USER_ID)).willReturn(Optional.of(breeding));

        service.update(4L, new BreedingUpdateRequest(BreedingStatus.FAILED, null));

        assertThat(breeding.getStatus()).isEqualTo(BreedingStatus.FAILED);
        verify(clutchRepository, never()).deleteAllByBreedingId(anyLong());
        verify(clutchRepository, never()).saveAll(anyList());
    }

    @Test
    void 빈_clutches를_보내면_산란_기록을_모두_지운다() {
        given(breedingRepository.findByIdAndUserId(4L, USER_ID))
                .willReturn(Optional.of(breeding(4L, null, "수컷", null, "암컷")));

        service.update(4L, new BreedingUpdateRequest(null, List.of()));

        verify(clutchRepository).deleteAllByBreedingId(4L);
        verify(clutchRepository).saveAll(List.of());
    }

    @Test
    void 산란일이_메이팅일_전이거나_오늘_이후면_INVALID_INPUT이고_기존_산란_기록을_지우지_않는다() {
        given(breedingRepository.findByIdAndUserId(4L, USER_ID))
                .willReturn(Optional.of(breeding(4L, null, "수컷", null, "암컷")));

        assertInvalidInput(() -> service.update(4L, new BreedingUpdateRequest(null,
                List.of(new ClutchRequest(PAIRED_DATE.minusDays(1), 2, null, null)))));
        assertInvalidInput(() -> service.update(4L, new BreedingUpdateRequest(null,
                List.of(new ClutchRequest(KST_TODAY.plusDays(1), 2, null, null)))));
        verify(clutchRepository, never()).deleteAllByBreedingId(anyLong());
    }

    @Test
    void 예상_해칭일이_산란일보다_빠르면_INVALID_INPUT() {
        given(breedingRepository.findByIdAndUserId(4L, USER_ID))
                .willReturn(Optional.of(breeding(4L, null, "수컷", null, "암컷")));
        LocalDate laid = LocalDate.of(2026, 9, 1);

        assertInvalidInput(() -> service.update(4L, new BreedingUpdateRequest(null,
                List.of(new ClutchRequest(laid, 2, null, laid.minusDays(1))))));
        verify(clutchRepository, never()).deleteAllByBreedingId(anyLong());
    }

    @Test
    void 해칭_개체는_어미의_종과_급여_주기를_따르고_해칭일을_채우며_상태가_HATCHED가_된다() {
        Species species = mock(Species.class);
        Animal female = animal(12L, "레몬", species, "레오파드 게코", 5);
        Animal male = animal(15L, "망고", null, "다른 종", 9);
        Breeding breeding = breeding(4L, male, "망고", female, "레몬");
        given(breedingRepository.findByIdAndUserId(4L, USER_ID)).willReturn(Optional.of(breeding));
        given(animalRepository.saveAll(anyList())).willAnswer(invocation -> {
            List<Animal> animals = invocation.getArgument(0);
            for (int i = 0; i < animals.size(); i++) {
                withId(animals.get(i), 31L + i);
            }
            return animals;
        });

        List<Long> ids = service.registerHatchlings(4L, new HatchlingsCreateRequest(KST_TODAY,
                List.of(new Hatchling(" 망고 2세 A ", "탠저린"), new Hatchling("망고 2세 B", null)))).animalIds();

        assertThat(ids).containsExactly(31L, 32L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Animal>> captor = ArgumentCaptor.forClass(List.class);
        verify(animalRepository).saveAll(captor.capture());
        Animal first = captor.getValue().getFirst();
        assertThat(first.getUser()).isSameAs(USER);
        assertThat(first.getSpecies()).isSameAs(species);
        assertThat(first.getSpeciesName()).isEqualTo("레오파드 게코");
        assertThat(first.getFeedIntervalDays()).isEqualTo(5);
        assertThat(first.getName()).isEqualTo("망고 2세 A");
        assertThat(first.getMorph()).isEqualTo("탠저린");
        assertThat(first.getHatchDate()).isEqualTo(KST_TODAY);
        assertThat(first.getBreedingId()).isEqualTo(4L);
        assertThat(breeding.getStatus()).isEqualTo(BreedingStatus.HATCHED);
    }

    @Test
    void 어미가_외부_개체면_아빠의_종을_따른다() {
        Animal male = animal(15L, "망고", null, "직접 입력한 종", 9);
        given(breedingRepository.findByIdAndUserId(4L, USER_ID))
                .willReturn(Optional.of(breeding(4L, male, "망고", null, "분양처 암컷")));
        given(animalRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        service.registerHatchlings(4L, new HatchlingsCreateRequest(KST_TODAY, List.of(new Hatchling("새끼", null))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Animal>> captor = ArgumentCaptor.forClass(List.class);
        verify(animalRepository).saveAll(captor.capture());
        assertThat(captor.getValue().getFirst().getSpeciesName()).isEqualTo("직접 입력한 종");
        assertThat(captor.getValue().getFirst().getFeedIntervalDays()).isEqualTo(9);
    }

    @Test
    void 부모가_모두_외부_개체면_INVALID_INPUT이고_상태를_바꾸지_않는다() {
        Breeding breeding = breeding(4L, null, "수컷", null, "암컷");
        given(breedingRepository.findByIdAndUserId(4L, USER_ID)).willReturn(Optional.of(breeding));

        assertInvalidInput(() -> service.registerHatchlings(4L,
                new HatchlingsCreateRequest(KST_TODAY, List.of(new Hatchling("새끼", null)))));
        assertThat(breeding.getStatus()).isEqualTo(BreedingStatus.PAIRED);
        verify(animalRepository, never()).saveAll(anyList());
    }

    @Test
    void 해칭일이_메이팅일_전이거나_오늘_이후면_INVALID_INPUT() {
        given(breedingRepository.findByIdAndUserId(4L, USER_ID))
                .willReturn(Optional.of(breeding(4L, null, "수컷", animal(12L, "레몬"), "레몬")));

        assertInvalidInput(() -> service.registerHatchlings(4L,
                new HatchlingsCreateRequest(PAIRED_DATE.minusDays(1), List.of(new Hatchling("새끼", null)))));
        assertInvalidInput(() -> service.registerHatchlings(4L,
                new HatchlingsCreateRequest(KST_TODAY.plusDays(1), List.of(new Hatchling("새끼", null)))));
        verify(animalRepository, never()).saveAll(anyList());
    }

    @Test
    void 남의_번식_기록이면_수정_삭제_해칭_등록_모두_BREEDING_NOT_FOUND() {
        given(breedingRepository.findByIdAndUserId(4L, USER_ID)).willReturn(Optional.empty());

        assertBreedingNotFound(() -> service.update(4L, new BreedingUpdateRequest(BreedingStatus.FAILED, null)));
        assertBreedingNotFound(() -> service.delete(4L));
        assertBreedingNotFound(() -> service.registerHatchlings(4L,
                new HatchlingsCreateRequest(KST_TODAY, List.of(new Hatchling("새끼", null)))));
        verify(breedingRepository, never()).delete(any());
    }

    private static Breeding breeding(Long id, Animal male, String maleName, Animal female, String femaleName) {
        return withId(Breeding.builder().user(USER).male(male).maleName(maleName).female(female)
                .femaleName(femaleName).pairedDate(PAIRED_DATE).build(), id);
    }

    private static Clutch clutch(Breeding breeding, LocalDate laidDate, int eggCount) {
        return Clutch.builder().breeding(breeding).laidDate(laidDate).eggCount(eggCount).build();
    }

    private static Animal animal(Long id, String name) {
        return animal(id, name, null, "레오파드 게코", 4);
    }

    private static Animal animal(Long id, String name, Species species, String speciesName, int feedIntervalDays) {
        return withId(Animal.builder().user(USER).species(species).speciesName(speciesName).name(name)
                .feedIntervalDays(feedIntervalDays).build(), id);
    }

    private static Animal hatchling(Long id, Long breedingId) {
        return withId(Animal.builder().user(USER).speciesName("레오파드 게코").name("새끼").feedIntervalDays(4)
                .breedingId(breedingId).build(), id);
    }

    private static void assertInvalidInput(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    private static void assertBreedingNotFound(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BREEDING_NOT_FOUND);
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
