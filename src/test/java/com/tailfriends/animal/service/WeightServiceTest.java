package com.tailfriends.animal.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.log.repository.LogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WeightServiceTest {

    // UTC 2026-10-19 15:30 = KST 2026-10-20 00:30
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T15:30:00Z"), ZoneOffset.UTC);

    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final LogRepository logRepository = mock(LogRepository.class);
    private final WeightService weightService = new WeightService(CurrentUserStub.of(1L), animalRepository, logRepository, CLOCK);

    @Test
    void 기간을_안_주면_한국_날짜로_오늘까지_최근_1년을_본다() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(mock(Animal.class)));

        weightService.getWeights(12L, null, null);

        verify(logRepository).findWeights(12L, LocalDate.of(2025, 10, 20), LocalDate.of(2026, 10, 20));
    }

    @Test
    void to만_주면_그_1년_전부터_본다() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(mock(Animal.class)));

        weightService.getWeights(12L, null, LocalDate.of(2026, 3, 1));

        verify(logRepository).findWeights(12L, LocalDate.of(2025, 3, 1), LocalDate.of(2026, 3, 1));
    }

    @Test
    void from이_to보다_늦으면_INVALID_INPUT() {
        given(animalRepository.findByIdAndUserId(any(), any())).willReturn(Optional.of(mock(Animal.class)));

        assertThatThrownBy(() -> weightService.getWeights(12L, LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 남의_개체면_ANIMAL_NOT_FOUND() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> weightService.getWeights(12L, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ANIMAL_NOT_FOUND);
    }
}
