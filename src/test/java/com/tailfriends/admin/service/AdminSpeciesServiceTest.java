package com.tailfriends.admin.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.admin.dto.AdminSpeciesCreateRequest;
import com.tailfriends.admin.dto.AdminSpeciesUpdateRequest;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.species.domain.Home;
import com.tailfriends.species.domain.Shape;
import com.tailfriends.species.domain.Species;
import com.tailfriends.species.repository.SpeciesRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdminSpeciesServiceTest {

    private final SpeciesRepository speciesRepository = mock(SpeciesRepository.class);
    private final AdminSpeciesService service = new AdminSpeciesService(speciesRepository);

    @Test
    void 이미_있는_이름으로_추가하면_INVALID_INPUT() {
        given(speciesRepository.existsByName("레오파드 게코")).willReturn(true);

        assertThatThrownBy(() -> service.create(new AdminSpeciesCreateRequest(" 레오파드 게코 ", Shape.GECKO, null, 4,
                null, null, null, null, null, null, null, Home.DESERT, null, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verify(speciesRepository, never()).save(any());
    }

    @Test
    void 다른_종의_이름으로_바꾸면_INVALID_INPUT() {
        given(speciesRepository.findById(2L)).willReturn(Optional.of(Species.builder()
                .name("크레스티드 게코").shape(Shape.GECKO).feedIntervalDays(3).defaultHome(Home.FOREST).build()));
        given(speciesRepository.existsByNameAndIdNot("레오파드 게코", 2L)).willReturn(true);

        assertThatThrownBy(() -> service.update(2L, nameChange("레오파드 게코")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 없는_종이면_SPECIES_NOT_FOUND() {
        given(speciesRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.hide(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPECIES_NOT_FOUND);
    }

    private static AdminSpeciesUpdateRequest nameChange(String name) {
        return new AdminSpeciesUpdateRequest(name, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }
}
