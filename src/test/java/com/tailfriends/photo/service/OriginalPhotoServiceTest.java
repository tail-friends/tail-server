package com.tailfriends.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.user.domain.ConsentType;
import com.tailfriends.user.service.ConsentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OriginalPhotoServiceTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private PhotoFileRemover photoFileRemover;

    @Mock
    private ConsentService consentService;

    @InjectMocks
    private OriginalPhotoService originalPhotoService;

    @Test
    void 원본을_지우고_썸네일은_남기고_보관_동의를_철회한다() {
        Photo first = photo("photos/a_original.jpg");
        Photo second = photo("photos/b_original.jpg");
        given(photoRepository.findAllByUserIdAndOriginalKeyNotNull(1L)).willReturn(List.of(first, second));

        int deleted = originalPhotoService.deleteAll(1L).deletedCount();

        assertThat(deleted).isEqualTo(2);
        assertThat(first.getOriginalKey()).isNull();
        assertThat(first.getThumbnailKey()).isEqualTo("photos/thumb.jpg");
        verify(photoFileRemover).removeAfterCommit(List.of("photos/a_original.jpg", "photos/b_original.jpg"));
        verify(consentService).changeOptional(1L, ConsentType.KEEP_ORIGINAL, false);
    }

    @Test
    void 보관한_원본이_없어도_동의는_철회하고_0을_돌려준다() {
        given(photoRepository.findAllByUserIdAndOriginalKeyNotNull(1L)).willReturn(List.of());

        assertThat(originalPhotoService.deleteAll(1L).deletedCount()).isZero();
        verify(consentService).changeOptional(1L, ConsentType.KEEP_ORIGINAL, false);
    }

    @Test
    void 사용자가_없으면_아무것도_지우지_않는다() {
        willThrow(new BusinessException(ErrorCode.UNAUTHORIZED))
                .given(consentService).changeOptional(1L, ConsentType.KEEP_ORIGINAL, false);

        assertThatThrownBy(() -> originalPhotoService.deleteAll(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
        verify(photoFileRemover, never()).removeAfterCommit(any());
    }

    private static Photo photo(String originalKey) {
        return Photo.builder()
                .purpose(PhotoPurpose.ALBUM)
                .thumbnailKey("photos/thumb.jpg")
                .originalKey(originalKey)
                .build();
    }
}
