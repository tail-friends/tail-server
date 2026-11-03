package com.tailfriends.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.dto.AlbumPhotoResponse;
import com.tailfriends.photo.dto.AlbumResponse;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.PhotoStorage;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

class AlbumServiceTest {

    private static final Long USER_ID = 1L;

    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final PhotoRepository photoRepository = mock(PhotoRepository.class);
    private final PhotoStorage photoStorage = mock(PhotoStorage.class);
    private final AlbumService service =
            new AlbumService(CurrentUserStub.of(USER_ID), animalRepository, photoRepository, photoStorage);

    @Test
    void 사육장_사진을_뺀_용도로_최신순_한_페이지를_돌려주고_더_있으면_커서를_준다() {
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.of(mock(Animal.class)));
        given(photoRepository.findAlbumPage(12L,
                EnumSet.of(PhotoPurpose.CHARACTER, PhotoPurpose.HEALTH, PhotoPurpose.ALBUM), Long.MAX_VALUE,
                PageRequest.of(0, 3)))
                .willReturn(List.of(photo(303L), photo(302L), photo(301L)));
        given(photoStorage.urlOf(anyString())).willAnswer(invocation -> "http://files/" + invocation.getArgument(0));

        AlbumResponse response = service.getAlbum(12L, null, 2);

        assertThat(response.photos()).extracting(AlbumPhotoResponse::photoId).containsExactly(303L, 302L);
        assertThat(response.nextCursor()).isEqualTo(302L);
    }

    @Test
    void 사진_한_장을_명세_형식으로_바꾼다() {
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.of(mock(Animal.class)));
        given(photoRepository.findAlbumPage(eq(12L), any(), eq(400L), any())).willReturn(List.of(photo(301L)));
        given(photoStorage.urlOf("photos/301_thumb.jpg")).willReturn("http://files/photos/301_thumb.jpg");

        AlbumResponse response = service.getAlbum(12L, 400L, 20);

        AlbumPhotoResponse photo = response.photos().getFirst();
        // UTC 2026-10-18 16:00 = KST 2026-10-19 01:00
        assertThat(photo.takenAt()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(photo.thumbnailUrl()).isEqualTo("http://files/photos/301_thumb.jpg");
        assertThat(photo.purpose()).isEqualTo(PhotoPurpose.HEALTH);
        assertThat(photo.weightAtTime()).isEqualByComparingTo("57.8");
        assertThat(photo.healthCheck()).isNull();
        assertThat(photo.originalKept()).isTrue();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void 남의_개체면_ANIMAL_NOT_FOUND() {
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAlbum(12L, null, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ANIMAL_NOT_FOUND);
        verify(photoRepository, never()).findAlbumPage(any(), any(), any(Long.class), any());
    }

    private static Photo photo(Long id) {
        Photo photo = Photo.builder()
                .purpose(PhotoPurpose.HEALTH)
                .thumbnailKey("photos/" + id + "_thumb.jpg")
                .originalKey("photos/" + id + "_original.jpg")
                .weightAtTime(new BigDecimal("57.8"))
                .build();
        ReflectionTestUtils.setField(photo, "id", id);
        ReflectionTestUtils.setField(photo, "createdAt", Instant.parse("2026-10-18T16:00:00Z"));
        return photo;
    }
}
