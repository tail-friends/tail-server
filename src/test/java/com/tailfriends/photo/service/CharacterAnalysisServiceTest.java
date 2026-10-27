package com.tailfriends.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.photo.ai.CharacterAnalysis;
import com.tailfriends.photo.ai.CharacterAnalyzer;
import com.tailfriends.photo.domain.AiKind;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.dto.CharacterAnalysisResponse;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.PhotoStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CharacterAnalysisServiceTest {

    private static final Long USAGE_ID = 77L;
    private static final byte[] JPEG = {1, 2, 3};
    private static final CharacterAnalysis GECKO = new CharacterAnalysis(true, "gecko", "spots",
            new CharacterAnalysis.Colors("#E0A33A", "#3A2A1E", "#F2E6C8", "#2B2B2B"),
            new CharacterAnalysis.Extra(new BigDecimal("0.60"), new BigDecimal("0.80"), new BigDecimal("0.40")),
            "탠저린 레오파드 게코");

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private PhotoStorage photoStorage;

    @Mock
    private CharacterAnalyzer characterAnalyzer;

    @Mock
    private AiUsageLimiter aiUsageLimiter;

    private CharacterAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new CharacterAnalysisService(CurrentUserStub.of(1L), photoRepository, photoStorage, characterAnalyzer,
                aiUsageLimiter);
    }

    @Test
    void AI를_부르기_전에_횟수를_잡고_분석용_사본으로_분석한다() {
        given(photoRepository.findByIdAndUserId(301L, 1L)).willReturn(Optional.of(photo(null, "p_analysis.jpg")));
        given(aiUsageLimiter.reserve(1L, AiKind.CHARACTER)).willReturn(USAGE_ID);
        given(photoStorage.read("p_analysis.jpg")).willReturn(JPEG);
        given(characterAnalyzer.analyze(JPEG)).willReturn(GECKO);

        CharacterAnalysisResponse response = service.analyze(301L);

        assertThat(response.shape()).isEqualTo("gecko");
        assertThat(response.guess()).isEqualTo("탠저린 레오파드 게코");
        verify(aiUsageLimiter, never()).release(anyLong());
    }

    @Test
    void 원본이_있으면_원본으로_분석한다() {
        given(photoRepository.findByIdAndUserId(301L, 1L))
                .willReturn(Optional.of(photo("p_original.jpg", null)));
        given(photoStorage.read("p_original.jpg")).willReturn(JPEG);
        given(characterAnalyzer.analyze(JPEG)).willReturn(GECKO);

        service.analyze(301L);

        verify(photoStorage).read("p_original.jpg");
    }

    @Test
    void 오늘_횟수를_다_쓰면_AI를_부르지_않고_AI_LIMIT_EXCEEDED() {
        given(photoRepository.findByIdAndUserId(301L, 1L)).willReturn(Optional.of(photo(null, "p_analysis.jpg")));
        given(aiUsageLimiter.reserve(1L, AiKind.CHARACTER))
                .willThrow(new BusinessException(ErrorCode.AI_LIMIT_EXCEEDED));

        assertError(() -> service.analyze(301L), ErrorCode.AI_LIMIT_EXCEEDED);
        verify(characterAnalyzer, never()).analyze(any());
    }

    @Test
    void 파충류가_없다는_답도_성공이라_횟수를_풀지_않고_NO_REPTILE_IN_PHOTO() {
        given(photoRepository.findByIdAndUserId(301L, 1L)).willReturn(Optional.of(photo(null, "p_analysis.jpg")));
        given(aiUsageLimiter.reserve(1L, AiKind.CHARACTER)).willReturn(USAGE_ID);
        given(photoStorage.read("p_analysis.jpg")).willReturn(JPEG);
        given(characterAnalyzer.analyze(JPEG)).willReturn(CharacterAnalysis.notReptile());

        assertError(() -> service.analyze(301L), ErrorCode.NO_REPTILE_IN_PHOTO);
        verify(aiUsageLimiter, never()).release(anyLong());
    }

    @Test
    void AI를_쓸_수_없으면_잡은_횟수를_풀고_AI_UNAVAILABLE() {
        given(photoRepository.findByIdAndUserId(301L, 1L)).willReturn(Optional.of(photo(null, "p_analysis.jpg")));
        given(aiUsageLimiter.reserve(1L, AiKind.CHARACTER)).willReturn(USAGE_ID);
        given(photoStorage.read("p_analysis.jpg")).willReturn(JPEG);
        given(characterAnalyzer.analyze(JPEG)).willThrow(new BusinessException(ErrorCode.AI_UNAVAILABLE));

        assertError(() -> service.analyze(301L), ErrorCode.AI_UNAVAILABLE);
        verify(aiUsageLimiter).release(USAGE_ID);
    }

    @Test
    void 사진을_읽지_못해도_잡은_횟수를_푼다() {
        given(photoRepository.findByIdAndUserId(301L, 1L)).willReturn(Optional.of(photo(null, "p_analysis.jpg")));
        given(aiUsageLimiter.reserve(1L, AiKind.CHARACTER)).willReturn(USAGE_ID);
        given(photoStorage.read("p_analysis.jpg")).willThrow(new UncheckedIOException(new IOException()));

        assertThatThrownBy(() -> service.analyze(301L)).isInstanceOf(UncheckedIOException.class);
        verify(aiUsageLimiter).release(USAGE_ID);
    }

    @Test
    void 분석할_사진이_없으면_INVALID_INPUT() {
        given(photoRepository.findByIdAndUserId(301L, 1L)).willReturn(Optional.of(photo(null, null)));

        assertError(() -> service.analyze(301L), ErrorCode.INVALID_INPUT);
        verify(aiUsageLimiter, never()).reserve(any(), any());
        verify(characterAnalyzer, never()).analyze(any());
    }

    @Test
    void 남의_사진이면_PHOTO_NOT_FOUND() {
        given(photoRepository.findByIdAndUserId(eq(301L), eq(1L))).willReturn(Optional.empty());

        assertError(() -> service.analyze(301L), ErrorCode.PHOTO_NOT_FOUND);
    }

    private static Photo photo(String originalKey, String analysisKey) {
        return Photo.builder()
                .purpose(PhotoPurpose.CHARACTER)
                .thumbnailKey("p_thumb.jpg")
                .originalKey(originalKey)
                .analysisKey(analysisKey)
                .build();
    }

    private static void assertError(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(code);
    }
}
