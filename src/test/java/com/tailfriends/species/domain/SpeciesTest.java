package com.tailfriends.species.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SpeciesTest {

    @Test
    void 수정은_보낸_값만_바꾸고_나머지는_그대로_둔다() {
        Species species = leopardGecko();

        species.update(new SpeciesChange(null, null, null, 5, null,
                new BigDecimal("30.0"), new BigDecimal("34.0"), null, null, null, null, null, null, null, null, null, null, null));

        assertThat(species.getName()).isEqualTo("레오파드 게코");
        assertThat(species.getShape()).isEqualTo(Shape.GECKO);
        assertThat(species.getFeedIntervalDays()).isEqualTo(5);
        assertThat(species.getHotMin()).isEqualByComparingTo("30.0");
        assertThat(species.getHotMax()).isEqualByComparingTo("34.0");
    }

    @Test
    void 온습도_범위의_최소가_최대보다_크면_INVALID_INPUT() {
        Species species = leopardGecko();

        assertThatThrownBy(() -> species.update(new SpeciesChange(null, null, null, null, null,
                null, null, null, null, 70, 40, null, null, null, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 숨기면_hidden이_되고_수정으로_다시_보이게_할_수_있다() {
        Species species = leopardGecko();

        species.hide();
        assertThat(species.isHidden()).isTrue();

        species.update(new SpeciesChange(null, null, null, null, null,
                null, null, null, null, null, null, null, false, null, null, null, null, null));
        assertThat(species.isHidden()).isFalse();
    }

    private static Species leopardGecko() {
        return Species.builder()
                .name("레오파드 게코")
                .shape(Shape.GECKO)
                .feedIntervalDays(4)
                .defaultHome(Home.DESERT)
                .build();
    }
}
