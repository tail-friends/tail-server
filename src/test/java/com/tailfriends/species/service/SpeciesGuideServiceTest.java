package com.tailfriends.species.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.species.domain.GuideSection;
import com.tailfriends.species.domain.Home;
import com.tailfriends.species.domain.Shape;
import com.tailfriends.species.domain.Species;
import com.tailfriends.species.domain.SpeciesGuide;
import com.tailfriends.species.dto.SpeciesGuideResponse;
import com.tailfriends.species.repository.SpeciesGuideRepository;
import com.tailfriends.species.repository.SpeciesRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SpeciesGuideServiceTest {

    @Mock
    private SpeciesRepository speciesRepository;

    @Mock
    private SpeciesGuideRepository speciesGuideRepository;

    @InjectMocks
    private SpeciesGuideService speciesGuideService;

    private final Species species = Species.builder()
            .name("레오파드 게코").shape(Shape.GECKO).feedIntervalDays(4).defaultHome(Home.DESERT).build();

    @Test
    void 섹션은_습성_먹이_환경_건강_순서로_담는다() {
        given(speciesRepository.findById(1L)).willReturn(Optional.of(species));
        given(speciesGuideRepository.findAllBySpeciesId(1L)).willReturn(List.of(
                guide(GuideSection.HEALTH), guide(GuideSection.HABIT), guide(GuideSection.ENV)));

        SpeciesGuideResponse response = speciesGuideService.getGuide(1L, null);

        assertThat(response.name()).isEqualTo("레오파드 게코");
        assertThat(response.sections()).extracting(SpeciesGuideResponse.Section::key)
                .containsExactly(GuideSection.HABIT, GuideSection.ENV, GuideSection.HEALTH);
    }

    @Test
    void section을_주면_그_섹션만_담는다() {
        given(speciesRepository.findById(1L)).willReturn(Optional.of(species));
        given(speciesGuideRepository.findAllBySpeciesId(1L)).willReturn(List.of(
                guide(GuideSection.HEALTH), guide(GuideSection.HABIT)));

        SpeciesGuideResponse response = speciesGuideService.getGuide(1L, GuideSection.HEALTH);

        assertThat(response.sections()).extracting(SpeciesGuideResponse.Section::key)
                .containsExactly(GuideSection.HEALTH);
    }

    @Test
    void 없는_종이면_SPECIES_NOT_FOUND() {
        given(speciesRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> speciesGuideService.getGuide(99L, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SPECIES_NOT_FOUND);
    }

    private SpeciesGuide guide(GuideSection section) {
        SpeciesGuide guide = SpeciesGuide.of(species, section, section.getTitle() + " 내용");
        ReflectionTestUtils.setField(guide, "updatedAt", Instant.parse("2026-10-20T00:00:00Z"));
        return guide;
    }
}
