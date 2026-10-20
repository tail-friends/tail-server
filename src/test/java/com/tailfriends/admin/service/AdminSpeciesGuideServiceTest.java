package com.tailfriends.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.admin.dto.AdminSpeciesGuideRequest;
import com.tailfriends.admin.dto.AdminSpeciesGuideRequest.Section;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminSpeciesGuideServiceTest {

    // 2026-10-20 15:30 UTC = 2026-10-21 00:30 KST
    private static final Instant SAVED_AT = Instant.parse("2026-10-20T15:30:00Z");

    @Mock
    private SpeciesRepository speciesRepository;

    @Mock
    private SpeciesGuideRepository speciesGuideRepository;

    @InjectMocks
    private AdminSpeciesGuideService adminSpeciesGuideService;

    private final Species species = Species.builder()
            .name("레오파드 게코").shape(Shape.GECKO).feedIntervalDays(4).defaultHome(Home.DESERT).build();

    @Test
    void 없던_섹션은_만들고_있던_섹션은_내용을_바꾸고_보내지_않은_섹션은_지운다() {
        SpeciesGuide habit = saved(SpeciesGuide.of(species, GuideSection.HABIT, "야행성"));
        SpeciesGuide health = saved(SpeciesGuide.of(species, GuideSection.HEALTH, "탈피 부전 주의"));
        given(speciesRepository.findById(1L)).willReturn(Optional.of(species));
        given(speciesGuideRepository.findAllBySpeciesId(1L)).willReturn(List.of(habit, health));
        given(speciesGuideRepository.save(any())).willAnswer(invocation -> saved(invocation.getArgument(0)));

        SpeciesGuideResponse response = adminSpeciesGuideService.saveGuide(1L, new AdminSpeciesGuideRequest(List.of(
                new Section(GuideSection.FEED, "귀뚜라미, 밀웜"),
                new Section(GuideSection.HABIT, "야행성, 온순함"))));

        assertThat(habit.getBody()).isEqualTo("야행성, 온순함");
        verify(speciesGuideRepository).delete(health);
        verify(speciesGuideRepository).flush();
        assertThat(response.sections()).extracting(SpeciesGuideResponse.Section::key)
                .containsExactly(GuideSection.HABIT, GuideSection.FEED);
        assertThat(response.sections().get(1).title()).isEqualTo("먹이");
        assertThat(response.sections().get(1).updatedAt()).isEqualTo(LocalDate.of(2026, 10, 21));
    }

    @Test
    void 빈_목록을_보내면_모든_섹션을_지운다() {
        SpeciesGuide habit = saved(SpeciesGuide.of(species, GuideSection.HABIT, "야행성"));
        given(speciesRepository.findById(1L)).willReturn(Optional.of(species));
        given(speciesGuideRepository.findAllBySpeciesId(1L)).willReturn(List.of(habit));

        SpeciesGuideResponse response = adminSpeciesGuideService.saveGuide(1L, new AdminSpeciesGuideRequest(List.of()));

        verify(speciesGuideRepository).delete(habit);
        assertThat(response.sections()).isEmpty();
    }

    @Test
    void 같은_섹션을_두_번_보내면_INVALID_INPUT() {
        given(speciesRepository.findById(1L)).willReturn(Optional.of(species));

        assertThatThrownBy(() -> adminSpeciesGuideService.saveGuide(1L, new AdminSpeciesGuideRequest(List.of(
                new Section(GuideSection.FEED, "귀뚜라미"),
                new Section(GuideSection.FEED, "밀웜")))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        verify(speciesGuideRepository, never()).save(any());
    }

    @Test
    void 없는_종이면_SPECIES_NOT_FOUND() {
        given(speciesRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminSpeciesGuideService.saveGuide(99L, new AdminSpeciesGuideRequest(List.of())))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SPECIES_NOT_FOUND);
    }

    private static SpeciesGuide saved(SpeciesGuide guide) {
        ReflectionTestUtils.setField(guide, "updatedAt", SAVED_AT);
        return guide;
    }
}
