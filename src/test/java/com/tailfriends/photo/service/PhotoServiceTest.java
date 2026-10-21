package com.tailfriends.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.dto.PhotoUploadResponse;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.PhotoStorage;
import com.tailfriends.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    private static final byte[] BYTES = {1, 2, 3};
    private static final MockMultipartFile FILE = new MockMultipartFile("file", "mango.jpg", "image/jpeg", BYTES);

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
    private Animal animal;

    @InjectMocks
    private PhotoService photoService;

    @Test
    void 원본_보관을_원하지_않으면_썸네일만_저장한다() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(animal));
        given(imageResizer.toJpegs(BYTES, 128)).willReturn(List.of(new byte[] {9}));
        given(photoRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(photoStorage.urlOf(anyString())).willAnswer(invocation -> "http://files/" + invocation.getArgument(0));

        PhotoUploadResponse response = photoService.upload(1L, 12L, PhotoPurpose.CHARACTER, false, FILE);

        verify(photoStorage).save(argThat(key -> key.startsWith("photos/") && key.endsWith("_thumb.jpg")), any());
        verify(photoStorage, never()).save(argThat(key -> key.endsWith("_original.jpg")), any());
        verify(photoRepository).save(argThat((Photo photo) -> photo.getOriginalKey() == null));
        assertThat(response.originalKept()).isFalse();
        assertThat(response.thumbnailUrl()).startsWith("http://files/photos/").endsWith("_thumb.jpg");
    }

    @Test
    void 원본_보관을_원하면_1600px_원본도_저장한다() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(animal));
        given(imageResizer.toJpegs(BYTES, 128, 1600)).willReturn(List.of(new byte[] {9}, new byte[] {8}));
        given(photoRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        PhotoUploadResponse response = photoService.upload(1L, 12L, PhotoPurpose.ALBUM, true, FILE);

        verify(photoStorage).save(argThat(key -> key.endsWith("_original.jpg")), eq(new byte[] {8}));
        verify(photoRepository).save(argThat((Photo photo) -> photo.getOriginalKey().endsWith("_original.jpg")));
        assertThat(response.originalKept()).isTrue();
    }

    @Test
    void DB_저장이_실패하면_저장한_파일을_지운다() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(animal));
        given(imageResizer.toJpegs(BYTES, 128, 1600)).willReturn(List.of(new byte[] {9}, new byte[] {8}));
        willThrow(new IllegalStateException("DB 오류")).given(photoRepository).save(any());

        assertThatThrownBy(() -> photoService.upload(1L, 12L, PhotoPurpose.ALBUM, true, FILE))
                .isInstanceOf(IllegalStateException.class);
        verify(photoStorage).delete(argThat(key -> key.endsWith("_thumb.jpg")));
        verify(photoStorage).delete(argThat(key -> key.endsWith("_original.jpg")));
    }

    @Test
    void 남의_개체면_ANIMAL_NOT_FOUND() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> photoService.upload(1L, 12L, PhotoPurpose.CHARACTER, false, FILE))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ANIMAL_NOT_FOUND);
        verify(photoStorage, never()).save(anyString(), any());
    }

    @Test
    void 빈_파일이면_INVALID_FILE() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(animal));
        MockMultipartFile empty = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> photoService.upload(1L, 12L, PhotoPurpose.CHARACTER, false, empty))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);
    }
}
