package com.tailfriends.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.PhotoStorage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalysisCopyCleanerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T09:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private PhotoStorage photoStorage;

    @Test
    void 올린_지_하루_지난_분석용_사본을_지운다() {
        Photo expired = photo("a_analysis.jpg");
        given(photoRepository.findAllByAnalysisKeyNotNullAndCreatedAtBefore(Instant.parse("2026-10-20T09:00:00Z")))
                .willReturn(List.of(expired));

        new AnalysisCopyCleaner(photoRepository, photoStorage, CLOCK).deleteExpired();

        verify(photoStorage).delete("a_analysis.jpg");
        assertThat(expired.getAnalysisKey()).isNull();
    }

    @Test
    void 파일을_지우지_못하면_키를_남겨_다음에_다시_시도한다() {
        Photo failed = photo("b_analysis.jpg");
        Photo ok = photo("c_analysis.jpg");
        given(photoRepository.findAllByAnalysisKeyNotNullAndCreatedAtBefore(Instant.parse("2026-10-20T09:00:00Z")))
                .willReturn(List.of(failed, ok));
        willThrow(new IllegalStateException("디스크 오류")).given(photoStorage).delete("b_analysis.jpg");

        new AnalysisCopyCleaner(photoRepository, photoStorage, CLOCK).deleteExpired();

        assertThat(failed.getAnalysisKey()).isEqualTo("b_analysis.jpg");
        assertThat(ok.getAnalysisKey()).isNull();
    }

    private static Photo photo(String analysisKey) {
        return Photo.builder()
                .purpose(PhotoPurpose.CHARACTER)
                .thumbnailKey("thumb.jpg")
                .analysisKey(analysisKey)
                .build();
    }
}
