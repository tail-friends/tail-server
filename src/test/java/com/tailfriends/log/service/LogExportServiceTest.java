package com.tailfriends.log.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.log.repository.LogRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LogExportServiceTest {

    private static final Long USER_ID = 1L;

    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final LogRepository logRepository = mock(LogRepository.class);
    private final LogExportService service =
            new LogExportService(CurrentUserStub.of(USER_ID), animalRepository, logRepository);

    @Test
    void 개체와_기간을_주지_않으면_내_기록_전체를_내보낸다() {
        given(logRepository.findForExport(USER_ID, null, null, null)).willReturn(List.of());

        byte[] csv = service.exportCsv(null, null, null);

        assertThat(csv).isNotEmpty();
        verify(logRepository).findForExport(USER_ID, null, null, null);
    }

    @Test
    void 시작일이_종료일보다_늦으면_INVALID_INPUT() {
        assertThatThrownBy(() -> service.exportCsv(null, LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        verify(logRepository, never()).findForExport(any(), any(), any(), any());
    }

    @Test
    void 시작일과_종료일이_같으면_그날_기록을_내보낸다() {
        LocalDate day = LocalDate.of(2026, 10, 1);
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.of(mock(Animal.class)));

        service.exportCsv(12L, day, day);

        verify(logRepository).findForExport(USER_ID, 12L, day, day);
    }

    @Test
    void 내_개체가_아니면_ANIMAL_NOT_FOUND() {
        given(animalRepository.findByIdAndUserId(99L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.exportCsv(99L, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ANIMAL_NOT_FOUND);
        verify(logRepository, never()).findForExport(any(), any(), any(), any());
    }
}
