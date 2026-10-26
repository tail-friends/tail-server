package com.tailfriends.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.dto.PhotoUploadResponse;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.PhotoStorage;
import com.tailfriends.photo.storage.StorageProperties;
import com.tailfriends.user.domain.ConsentType;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import com.tailfriends.user.service.ConsentService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    private static final byte[] BYTES = {1, 2, 3};
    private static final MockMultipartFile FILE = new MockMultipartFile("file", "mango.jpg", "image/jpeg", BYTES);
    // UTC 2026-10-19 15:30 = KST 2026-10-20 00:30. KST 오늘은 UTC 2026-10-19 15:00부터
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T15:30:00Z"), ZoneOffset.UTC);
    private static final Instant START_OF_TODAY = Instant.parse("2026-10-19T15:00:00Z");
    private static final int DAILY_LIMIT = 30;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PhotoStorage photoStorage;

    @Mock
    private ImageResizer imageResizer;

    @Mock
    private ConsentService consentService;

    @Mock
    private Animal animal;

    @Mock
    private User user;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private CurrentUser currentUser;

    private PhotoService photoService;

    @BeforeEach
    void 현재_사용자() {
        given(currentUser.id()).willReturn(1L);
        lenient().when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        photoService = new PhotoService(currentUser, photoRepository, animalRepository, userRepository, photoStorage,
                imageResizer, consentService, new StorageProperties("storage", "http://files", DAILY_LIMIT), CLOCK);
    }

    @Test
    void 오늘_올린_사진이_하루_제한에_닿으면_파일을_읽지_않고_PHOTO_LIMIT_EXCEEDED() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(animal));
        given(photoRepository.countByUserIdAndCreatedAtGreaterThanEqual(1L, START_OF_TODAY))
                .willReturn((long) DAILY_LIMIT);

        assertThatThrownBy(() -> photoService.upload(12L, PhotoPurpose.ALBUM, false, FILE))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PHOTO_LIMIT_EXCEEDED);
        verify(imageResizer, never()).toJpegs(any(), any(int[].class));
        verify(photoStorage, never()).save(anyString(), any());
    }

    @Test
    void 하루_제한보다_하나_적으면_올릴_수_있고_잠근_사용자로_저장한다() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(animal));
        given(photoRepository.countByUserIdAndCreatedAtGreaterThanEqual(1L, START_OF_TODAY))
                .willReturn((long) DAILY_LIMIT - 1);
        given(imageResizer.toJpegs(BYTES, 128)).willReturn(List.of(new byte[] {9}));
        given(photoRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        photoService.upload(12L, PhotoPurpose.ALBUM, false, FILE);

        verify(photoRepository).save(argThat((Photo photo) -> photo.getUser() == user));
    }

    @Test
    void 사용자가_없으면_UNAUTHORIZED() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(animal));
        given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> photoService.upload(12L, PhotoPurpose.ALBUM, false, FILE))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 원본_보관을_원하지_않으면_썸네일만_저장한다() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(animal));
        given(imageResizer.toJpegs(BYTES, 128)).willReturn(List.of(new byte[] {9}));
        given(photoRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(photoStorage.urlOf(anyString())).willAnswer(invocation -> "http://files/" + invocation.getArgument(0));

        PhotoUploadResponse response = photoService.upload(12L, PhotoPurpose.ALBUM, false, FILE);

        verify(photoStorage).save(argThat(key -> key.startsWith("photos/") && key.endsWith("_thumb.jpg")), any());
        verify(photoStorage, never()).save(argThat(key -> !key.endsWith("_thumb.jpg")), any());
        verify(photoRepository).save(argThat((Photo photo) -> photo.getOriginalKey() == null));
        assertThat(response.originalKept()).isFalse();
        assertThat(response.thumbnailUrl()).startsWith("http://files/photos/").endsWith("_thumb.jpg");
    }

    @Test
    void 원본을_보관하지_않는_캐릭터_사진은_1024px_분석용_사본을_둔다() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(animal));
        given(imageResizer.toJpegs(BYTES, 128, 1024)).willReturn(List.of(new byte[] {9}, new byte[] {7}));
        given(photoRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        PhotoUploadResponse response = photoService.upload(12L, PhotoPurpose.CHARACTER, false, FILE);

        verify(photoStorage).save(argThat(key -> key.endsWith("_analysis.jpg")), eq(new byte[] {7}));
        verify(photoRepository).save(argThat((Photo photo) ->
                photo.getOriginalKey() == null && photo.getAnalysisKey().endsWith("_analysis.jpg")));
        assertThat(response.originalKept()).isFalse();
    }

    @Test
    void 원본_보관을_원하고_동의했으면_1600px_원본도_저장한다() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(animal));
        given(consentService.isAgreed(1L, ConsentType.KEEP_ORIGINAL)).willReturn(true);
        given(imageResizer.toJpegs(BYTES, 128, 1600)).willReturn(List.of(new byte[] {9}, new byte[] {8}));
        given(photoRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        PhotoUploadResponse response = photoService.upload(12L, PhotoPurpose.ALBUM, true, FILE);

        verify(photoStorage).save(argThat(key -> key.endsWith("_original.jpg")), eq(new byte[] {8}));
        verify(photoRepository).save(argThat((Photo photo) -> photo.getOriginalKey().endsWith("_original.jpg")));
        assertThat(response.originalKept()).isTrue();
    }

    @Test
    void 원본_보관을_원해도_동의하지_않았으면_썸네일만_저장한다() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(animal));
        given(consentService.isAgreed(1L, ConsentType.KEEP_ORIGINAL)).willReturn(false);
        given(imageResizer.toJpegs(BYTES, 128)).willReturn(List.of(new byte[] {9}));
        given(photoRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        PhotoUploadResponse response = photoService.upload(12L, PhotoPurpose.ALBUM, true, FILE);

        verify(photoStorage, never()).save(argThat(key -> key.endsWith("_original.jpg")), any());
        verify(photoRepository).save(argThat((Photo photo) -> photo.getOriginalKey() == null));
        assertThat(response.originalKept()).isFalse();
    }

    @Test
    void 원본_보관에_동의하지_않은_캐릭터_사진은_분석용_사본을_둔다() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(animal));
        given(consentService.isAgreed(1L, ConsentType.KEEP_ORIGINAL)).willReturn(false);
        given(imageResizer.toJpegs(BYTES, 128, 1024)).willReturn(List.of(new byte[] {9}, new byte[] {7}));
        given(photoRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        PhotoUploadResponse response = photoService.upload(12L, PhotoPurpose.CHARACTER, true, FILE);

        verify(photoRepository).save(argThat((Photo photo) ->
                photo.getOriginalKey() == null && photo.getAnalysisKey().endsWith("_analysis.jpg")));
        assertThat(response.originalKept()).isFalse();
    }

    @Test
    void DB_저장이_실패하면_저장한_파일을_지운다() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(animal));
        given(consentService.isAgreed(1L, ConsentType.KEEP_ORIGINAL)).willReturn(true);
        given(imageResizer.toJpegs(BYTES, 128, 1600)).willReturn(List.of(new byte[] {9}, new byte[] {8}));
        willThrow(new IllegalStateException("DB 오류")).given(photoRepository).save(any());

        assertThatThrownBy(() -> photoService.upload(12L, PhotoPurpose.ALBUM, true, FILE))
                .isInstanceOf(IllegalStateException.class);
        verify(photoStorage).delete(argThat(key -> key.endsWith("_thumb.jpg")));
        verify(photoStorage).delete(argThat(key -> key.endsWith("_original.jpg")));
    }

    @Test
    void 남의_개체면_ANIMAL_NOT_FOUND() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> photoService.upload(12L, PhotoPurpose.CHARACTER, false, FILE))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ANIMAL_NOT_FOUND);
        verify(photoStorage, never()).save(anyString(), any());
    }

    @Test
    void 빈_파일이면_INVALID_FILE() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(animal));
        MockMultipartFile empty = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> photoService.upload(12L, PhotoPurpose.CHARACTER, false, empty))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);
    }
}
