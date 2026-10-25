package com.tailfriends.species.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.tailfriends.species.domain.Home;
import com.tailfriends.species.domain.Shape;
import com.tailfriends.species.domain.Species;
import com.tailfriends.species.dto.SpeciesResponse;
import com.tailfriends.species.repository.SpeciesRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpeciesServiceTest {

    @Mock
    private SpeciesRepository speciesRepository;

    @InjectMocks
    private SpeciesService speciesService;

    @Test
    void 체형과_기본_사육장은_소문자로_응답한다() {
        given(speciesRepository.findAllByHiddenFalseOrderByDisplayOrderAscNameAsc()).willReturn(List.of(
                Species.builder()
                        .name("레오파드 게코")
                        .shape(Shape.GECKO)
                        .feedIntervalDays(4)
                        .defaultHome(Home.DESERT)
                        .build()));

        SpeciesResponse response = speciesService.getSpeciesList().species().get(0);

        assertThat(response.name()).isEqualTo("레오파드 게코");
        assertThat(response.shape()).isEqualTo("gecko");
        assertThat(response.defaultHome()).isEqualTo("desert");
        assertThat(response.feedIntervalDays()).isEqualTo(4);
    }

    @Test
    void 온습도_값이_없으면_범위를_null로_응답한다() {
        given(speciesRepository.findAllByHiddenFalseOrderByDisplayOrderAscNameAsc()).willReturn(List.of(
                Species.builder()
                        .name("볼 파이톤")
                        .shape(Shape.SNAKE)
                        .feedIntervalDays(10)
                        .defaultHome(Home.ROCK)
                        .build()));

        SpeciesResponse.EnvRange envRange = speciesService.getSpeciesList().species().get(0).envRange();

        assertThat(envRange.hot()).isNull();
        assertThat(envRange.cool()).isNull();
        assertThat(envRange.humidity()).isNull();
    }

    @Test
    void 온습도_값이_있으면_최소_최대_범위로_응답한다() {
        given(speciesRepository.findAllByHiddenFalseOrderByDisplayOrderAscNameAsc()).willReturn(List.of(
                Species.builder()
                        .name("레오파드 게코")
                        .shape(Shape.GECKO)
                        .feedIntervalDays(4)
                        .defaultHome(Home.DESERT)
                        .hotMin(new BigDecimal("30.0"))
                        .hotMax(new BigDecimal("33.0"))
                        .humidityMin(30)
                        .humidityMax(40)
                        .build()));

        SpeciesResponse.EnvRange envRange = speciesService.getSpeciesList().species().get(0).envRange();

        assertThat(envRange.hot().min()).isEqualByComparingTo("30.0");
        assertThat(envRange.hot().max()).isEqualByComparingTo("33.0");
        assertThat(envRange.cool()).isNull();
        assertThat(envRange.humidity().min()).isEqualTo(30);
    }
}
