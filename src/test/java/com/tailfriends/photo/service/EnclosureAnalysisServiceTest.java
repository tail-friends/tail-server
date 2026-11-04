package com.tailfriends.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.photo.ai.EnclosureAnalysis;
import com.tailfriends.photo.ai.EnclosureAnalyzer;
import com.tailfriends.photo.ai.EnclosureObjectType;
import com.tailfriends.photo.domain.AiKind;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.dto.EnclosureAnalysisResponse;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.PhotoStorage;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnclosureAnalysisServiceTest {

    private static final Long USAGE_ID = 77L;
    private static final byte[] JPEG = {1, 2, 3};
    private static final EnclosureAnalysis DESERT = new EnclosureAnalysis(true, "#C9A26B", "#5B4A3A", List.of(
            new EnclosureAnalysis.DetectedObject(EnclosureObjectType.ROCK, new BigDecimal("0.700"),
                    new BigDecimal("0.800")),
            new EnclosureAnalysis.DetectedObject(EnclosureObjectType.WATER_DISH, new BigDecimal("0.125"),
                    new BigDecimal("0.850"))));

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private PhotoStorage photoStorage;

    @Mock
    private EnclosureAnalyzer enclosureAnalyzer;

    @Mock
    private AiUsageLimiter aiUsageLimiter;

    private EnclosureAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new EnclosureAnalysisService(CurrentUserStub.of(1L), photoRepository, photoStorage, enclosureAnalyzer,
                aiUsageLimiter);
    }

    @Test
    void 사육장_분석_횟수를_잡고_분석해_명세_형식으로_돌려준다() {
        given(photoRepository.findByIdAndUserId(310L, 1L))
                .willReturn(Optional.of(photo(PhotoPurpose.ENCLOSURE, "p_analysis.jpg")));
        given(aiUsageLimiter.reserve(1L, AiKind.ENCLOSURE)).willReturn(USAGE_ID);
        given(photoStorage.read("p_analysis.jpg")).willReturn(JPEG);
        given(enclosureAnalyzer.analyze(JPEG)).willReturn(DESERT);

        EnclosureAnalysisResponse response = service.analyze(310L);

        assertThat(response.floorColor()).isEqualTo("#C9A26B");
        assertThat(response.wallColor()).isEqualTo("#5B4A3A");
        assertThat(response.objects()).containsExactly(
                new EnclosureAnalysisResponse.ObjectResponse("ROCK", 0.7, 0.8),
                new EnclosureAnalysisResponse.ObjectResponse("WATER_DISH", 0.125, 0.85));
        verify(aiUsageLimiter, never()).release(anyLong());
    }

    @Test
    void 사육장_사진이_아니라는_답도_성공이라_횟수를_풀지_않고_NO_ENCLOSURE_IN_PHOTO() {
        given(photoRepository.findByIdAndUserId(310L, 1L))
                .willReturn(Optional.of(photo(PhotoPurpose.ENCLOSURE, "p_analysis.jpg")));
        given(aiUsageLimiter.reserve(1L, AiKind.ENCLOSURE)).willReturn(USAGE_ID);
        given(photoStorage.read("p_analysis.jpg")).willReturn(JPEG);
        given(enclosureAnalyzer.analyze(JPEG)).willReturn(EnclosureAnalysis.notEnclosure());

        assertError(() -> service.analyze(310L), ErrorCode.NO_ENCLOSURE_IN_PHOTO);
        verify(aiUsageLimiter, never()).release(anyLong());
    }

    @Test
    void AI를_쓸_수_없으면_잡은_횟수를_풀고_AI_UNAVAILABLE() {
        given(photoRepository.findByIdAndUserId(310L, 1L))
                .willReturn(Optional.of(photo(PhotoPurpose.ENCLOSURE, "p_analysis.jpg")));
        given(aiUsageLimiter.reserve(1L, AiKind.ENCLOSURE)).willReturn(USAGE_ID);
        given(photoStorage.read("p_analysis.jpg")).willReturn(JPEG);
        given(enclosureAnalyzer.analyze(JPEG)).willThrow(new BusinessException(ErrorCode.AI_UNAVAILABLE));

        assertError(() -> service.analyze(310L), ErrorCode.AI_UNAVAILABLE);
        verify(aiUsageLimiter).release(USAGE_ID);
    }

    @Test
    void 오늘_횟수를_다_쓰면_AI를_부르지_않고_AI_LIMIT_EXCEEDED() {
        given(photoRepository.findByIdAndUserId(310L, 1L))
                .willReturn(Optional.of(photo(PhotoPurpose.ENCLOSURE, "p_analysis.jpg")));
        given(aiUsageLimiter.reserve(1L, AiKind.ENCLOSURE))
                .willThrow(new BusinessException(ErrorCode.AI_LIMIT_EXCEEDED));

        assertError(() -> service.analyze(310L), ErrorCode.AI_LIMIT_EXCEEDED);
        verify(enclosureAnalyzer, never()).analyze(any());
    }

    @Test
    void 사육장용으로_올린_사진이_아니면_횟수를_잡지_않고_INVALID_INPUT() {
        given(photoRepository.findByIdAndUserId(310L, 1L))
                .willReturn(Optional.of(photo(PhotoPurpose.CHARACTER, "p_analysis.jpg")));

        assertError(() -> service.analyze(310L), ErrorCode.INVALID_INPUT);
        verify(aiUsageLimiter, never()).reserve(any(), any());
        verify(enclosureAnalyzer, never()).analyze(any());
    }

    @Test
    void 분석할_사진이_없으면_INVALID_INPUT() {
        given(photoRepository.findByIdAndUserId(310L, 1L))
                .willReturn(Optional.of(photo(PhotoPurpose.ENCLOSURE, null)));

        assertError(() -> service.analyze(310L), ErrorCode.INVALID_INPUT);
        verify(aiUsageLimiter, never()).reserve(any(), any());
    }

    @Test
    void 남의_사진이면_PHOTO_NOT_FOUND() {
        given(photoRepository.findByIdAndUserId(310L, 1L)).willReturn(Optional.empty());

        assertError(() -> service.analyze(310L), ErrorCode.PHOTO_NOT_FOUND);
    }

    private static Photo photo(PhotoPurpose purpose, String analysisKey) {
        return Photo.builder()
                .purpose(purpose)
                .thumbnailKey("p_thumb.jpg")
                .analysisKey(analysisKey)
                .build();
    }

    private static void assertError(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(code);
    }
}
