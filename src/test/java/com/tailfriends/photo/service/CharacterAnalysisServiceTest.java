package com.tailfriends.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.photo.ai.AiProperties;
import com.tailfriends.photo.ai.CharacterAnalysis;
import com.tailfriends.photo.ai.CharacterAnalyzer;
import com.tailfriends.photo.domain.AiUsage;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.dto.CharacterAnalysisResponse;
import com.tailfriends.photo.repository.AiUsageRepository;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.PhotoStorage;
import com.tailfriends.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CharacterAnalysisServiceTest {

    // 2026-10-20 16:00 UTC = 2026-10-21 01:00 KST. 오늘(KST)은 2026-10-20 15:00 UTC부터
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T16:00:00Z"), ZoneOffset.UTC);
    private static final Instant START_OF_TODAY = Instant.parse("2026-10-20T15:00:00Z");
    private static final byte[] JPEG = {1, 2, 3};
    private static final CharacterAnalysis GECKO = new CharacterAnalysis(true, "gecko", "spots",
            new CharacterAnalysis.Colors("#E0A33A", "#3A2A1E", "#F2E6C8", "#2B2B2B"),
            new CharacterAnalysis.Extra(new BigDecimal("0.60"), new BigDecimal("0.80"), new BigDecimal("0.40")),
            "탠저린 레오파드 게코");

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private AiUsageRepository aiUsageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PhotoStorage photoStorage;

    @Mock
    private CharacterAnalyzer characterAnalyzer;

    private CharacterAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new CharacterAnalysisService(CurrentUserStub.of(1L), photoRepository, aiUsageRepository, userRepository, photoStorage,
                characterAnalyzer, new AiProperties("gemini-test", 5, "key", 10), CLOCK);
    }

    @Test
    void 분석용_사본으로_분석하고_성공으로_기록한다() {
        given(photoRepository.findByIdAndUserId(301L, 1L)).willReturn(Optional.of(photo(null, "p_analysis.jpg")));
        given(aiUsageRepository.countByUserIdAndSuccessTrueAndCreatedAtGreaterThanEqual(1L, START_OF_TODAY))
                .willReturn(4L);
        given(photoStorage.read("p_analysis.jpg")).willReturn(JPEG);
        given(characterAnalyzer.analyze(JPEG)).willReturn(GECKO);

        CharacterAnalysisResponse response = service.analyze(301L);

        assertThat(response.shape()).isEqualTo("gecko");
        assertThat(response.guess()).isEqualTo("탠저린 레오파드 게코");
        verify(aiUsageRepository).save(argThat(AiUsage::isSuccess));
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
        given(aiUsageRepository.countByUserIdAndSuccessTrueAndCreatedAtGreaterThanEqual(1L, START_OF_TODAY))
                .willReturn(5L);

        assertError(() -> service.analyze(301L), ErrorCode.AI_LIMIT_EXCEEDED);
        verify(characterAnalyzer, never()).analyze(any());
    }

    @Test
    void 파충류가_없으면_성공으로_기록하고_NO_REPTILE_IN_PHOTO() {
        given(photoRepository.findByIdAndUserId(301L, 1L)).willReturn(Optional.of(photo(null, "p_analysis.jpg")));
        given(photoStorage.read("p_analysis.jpg")).willReturn(JPEG);
        given(characterAnalyzer.analyze(JPEG)).willReturn(CharacterAnalysis.notReptile());

        assertError(() -> service.analyze(301L), ErrorCode.NO_REPTILE_IN_PHOTO);
        verify(aiUsageRepository).save(argThat(AiUsage::isSuccess));
    }

    @Test
    void AI를_쓸_수_없으면_실패로_기록하고_AI_UNAVAILABLE() {
        given(photoRepository.findByIdAndUserId(301L, 1L)).willReturn(Optional.of(photo(null, "p_analysis.jpg")));
        given(photoStorage.read("p_analysis.jpg")).willReturn(JPEG);
        given(characterAnalyzer.analyze(JPEG)).willThrow(new BusinessException(ErrorCode.AI_UNAVAILABLE));

        assertError(() -> service.analyze(301L), ErrorCode.AI_UNAVAILABLE);
        verify(aiUsageRepository).save(argThat(usage -> !usage.isSuccess()));
    }

    @Test
    void 분석할_사진이_없으면_INVALID_INPUT() {
        given(photoRepository.findByIdAndUserId(301L, 1L)).willReturn(Optional.of(photo(null, null)));

        assertError(() -> service.analyze(301L), ErrorCode.INVALID_INPUT);
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
