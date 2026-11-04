package com.tailfriends.enclosure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.enclosure.domain.BackgroundType;
import com.tailfriends.enclosure.domain.Enclosure;
import com.tailfriends.enclosure.domain.EnclosureObject;
import com.tailfriends.enclosure.dto.BackgroundObjectRequest;
import com.tailfriends.enclosure.dto.EnclosureCreateRequest;
import com.tailfriends.enclosure.repository.EnclosureObjectRepository;
import com.tailfriends.enclosure.repository.EnclosureRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.photo.ai.EnclosureObjectType;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.species.domain.Home;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

class PhotoThemeServiceTest {

    private static final Long USER_ID = 1L;

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PhotoRepository photoRepository = mock(PhotoRepository.class);
    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final EnclosureRepository enclosureRepository = mock(EnclosureRepository.class);
    private final EnclosureObjectRepository objectRepository = mock(EnclosureObjectRepository.class);
    private final PhotoThemeService service = new PhotoThemeService(CurrentUserStub.of(USER_ID), userRepository,
            photoRepository, animalRepository, enclosureRepository, objectRepository);

    @Test
    @SuppressWarnings("unchecked")
    void 사진_테마를_저장하면_색을_대문자로_장식을_셋째_자리로_저장하고_평소_사육장으로_정한다() {
        Animal animal = BeanUtils.instantiateClass(Animal.class);
        ReflectionTestUtils.setField(animal, "home", Home.FOREST);
        given(photoRepository.findByIdAndUserId(310L, USER_ID)).willReturn(Optional.of(photo(PhotoPurpose.ENCLOSURE)));
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.of(animal));
        givenUserWithPhotoThemes(3);
        given(enclosureRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 7L));

        Long id = service.create(new EnclosureCreateRequest(310L, "#c9a26b", "#5B4A3A",
                List.of(new BackgroundObjectRequest(EnclosureObjectType.ROCK, new BigDecimal("0.7"),
                        new BigDecimal("0.12345"))), 12L)).id();

        assertThat(id).isEqualTo(7L);
        ArgumentCaptor<Enclosure> saved = ArgumentCaptor.forClass(Enclosure.class);
        verify(enclosureRepository).save(saved.capture());
        assertThat(saved.getValue().getBackgroundType()).isEqualTo(BackgroundType.PHOTO);
        assertThat(saved.getValue().getPhotoId()).isEqualTo(310L);
        assertThat(saved.getValue().getFloorColor()).isEqualTo("#C9A26B");
        assertThat(saved.getValue().getTheme()).isNull();
        ArgumentCaptor<List<EnclosureObject>> objects = ArgumentCaptor.forClass(List.class);
        verify(objectRepository).saveAll(objects.capture());
        assertThat(objects.getValue()).singleElement().satisfies(object -> {
            assertThat(object.getType()).isEqualTo(EnclosureObjectType.ROCK);
            assertThat(object.getX()).isEqualTo(new BigDecimal("0.700"));
            assertThat(object.getY()).isEqualTo(new BigDecimal("0.123"));
        });
        assertThat(animal.getHome()).isNull();
        assertThat(animal.getEnclosureId()).isEqualTo(7L);
    }

    @Test
    void 사진_테마가_이미_10개면_저장하지_않고_ENCLOSURE_LIMIT_EXCEEDED() {
        given(photoRepository.findByIdAndUserId(310L, USER_ID)).willReturn(Optional.of(photo(PhotoPurpose.ENCLOSURE)));
        givenUserWithPhotoThemes(10);

        assertError(() -> service.create(request(null)), ErrorCode.ENCLOSURE_LIMIT_EXCEEDED);
        verify(enclosureRepository, never()).save(any());
    }

    @Test
    void 사육장용으로_올린_사진이_아니면_INVALID_INPUT() {
        given(photoRepository.findByIdAndUserId(310L, USER_ID)).willReturn(Optional.of(photo(PhotoPurpose.CHARACTER)));

        assertError(() -> service.create(request(null)), ErrorCode.INVALID_INPUT);
        verify(enclosureRepository, never()).save(any());
    }

    @Test
    void 남의_사진이면_PHOTO_NOT_FOUND() {
        given(photoRepository.findByIdAndUserId(310L, USER_ID)).willReturn(Optional.empty());

        assertError(() -> service.create(request(null)), ErrorCode.PHOTO_NOT_FOUND);
    }

    @Test
    void 평소_사육장으로_정할_개체가_남의_것이면_저장하지_않고_ANIMAL_NOT_FOUND() {
        given(photoRepository.findByIdAndUserId(310L, USER_ID)).willReturn(Optional.of(photo(PhotoPurpose.ENCLOSURE)));
        given(animalRepository.findByIdAndUserId(99L, USER_ID)).willReturn(Optional.empty());

        assertError(() -> service.create(request(99L)), ErrorCode.ANIMAL_NOT_FOUND);
        verify(enclosureRepository, never()).save(any());
    }

    @Test
    void 사진_테마를_지운다() {
        Enclosure photoTheme = Enclosure.photoTheme(null, 310L, "#C9A26B", "#5B4A3A");
        given(enclosureRepository.findByIdAndUserIdForUpdate(7L, USER_ID)).willReturn(Optional.of(photoTheme));

        service.delete(7L);

        verify(enclosureRepository).delete(photoTheme);
    }

    @Test
    void 기본_테마는_지우지_않고_BUILT_IN_THEME() {
        Enclosure builtIn = BeanUtils.instantiateClass(Enclosure.class);
        ReflectionTestUtils.setField(builtIn, "backgroundType", BackgroundType.BUILT_IN);
        ReflectionTestUtils.setField(builtIn, "theme", Home.DESERT);
        given(enclosureRepository.findByIdAndUserIdForUpdate(3L, USER_ID)).willReturn(Optional.of(builtIn));

        assertError(() -> service.delete(3L), ErrorCode.BUILT_IN_THEME);
        verify(enclosureRepository, never()).delete(any());
    }

    @Test
    void 남의_사육장은_지우지_않고_ENCLOSURE_NOT_FOUND() {
        given(enclosureRepository.findByIdAndUserIdForUpdate(7L, USER_ID)).willReturn(Optional.empty());

        assertError(() -> service.delete(7L), ErrorCode.ENCLOSURE_NOT_FOUND);
    }

    private void givenUserWithPhotoThemes(long count) {
        given(userRepository.findByIdForUpdate(USER_ID))
                .willReturn(Optional.of(BeanUtils.instantiateClass(User.class)));
        given(enclosureRepository.countByUserIdAndBackgroundType(USER_ID, BackgroundType.PHOTO)).willReturn(count);
    }

    private static EnclosureCreateRequest request(Long setAsHomeFor) {
        return new EnclosureCreateRequest(310L, "#C9A26B", "#5B4A3A", null, setAsHomeFor);
    }

    private static Photo photo(PhotoPurpose purpose) {
        Photo photo = Photo.builder().purpose(purpose).thumbnailKey("p_thumb.jpg").build();
        ReflectionTestUtils.setField(photo, "id", 310L);
        return photo;
    }

    private static Enclosure withId(Enclosure enclosure, Long id) {
        ReflectionTestUtils.setField(enclosure, "id", id);
        return enclosure;
    }

    private static void assertError(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(code);
    }
}
