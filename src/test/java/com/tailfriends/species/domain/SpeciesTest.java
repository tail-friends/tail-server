package com.tailfriends.species.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
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

    @Test
    void 분류와_노출_순서를_비우면_파충류와_1000이다() {
        Species species = leopardGecko();

        assertThat(species.getCategory()).isEqualTo(Category.REPTILE);
        assertThat(species.getDisplayOrder()).isEqualTo(1000);
        assertThat(species.getAliases()).isEmpty();
    }

    @Test
    void 별칭은_앞뒤_공백을_빼고_빈_값과_중복을_뺀다() {
        Species species = Species.builder()
                .name("볼 파이톤")
                .shape(Shape.SNAKE)
                .feedIntervalDays(10)
                .defaultHome(Home.ROCK)
                .aliases(List.of(" 볼파이선 ", "로열 파이톤", "볼파이선", " "))
                .build();

        assertThat(species.getAliases()).containsExactly("볼파이선", "로열 파이톤");
    }

    @Test
    void 별칭은_보낸_목록으로_통째로_바꾸고_보내지_않으면_그대로_둔다() {
        Species species = leopardGecko();

        species.update(aliasesChange(List.of("레오", "표범도마뱀붙이")));
        assertThat(species.getAliases()).containsExactly("레오", "표범도마뱀붙이");

        species.update(aliasesChange(null));
        assertThat(species.getAliases()).containsExactly("레오", "표범도마뱀붙이");

        species.update(aliasesChange(List.of()));
        assertThat(species.getAliases()).isEmpty();
    }

    private static SpeciesChange aliasesChange(List<String> aliases) {
        return new SpeciesChange(null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, aliases, null);
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
