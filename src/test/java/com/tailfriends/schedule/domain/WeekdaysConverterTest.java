package com.tailfriends.schedule.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WeekdaysConverterTest {

    private final WeekdaysConverter converter = new WeekdaysConverter();

    @Test
    void 요일을_월요일부터_쉼표로_이어_저장한다() {
        assertThat(converter.convertToDatabaseColumn(EnumSet.of(Weekday.SUN, Weekday.MON, Weekday.THU)))
                .isEqualTo("MON,THU,SUN");
    }

    @Test
    void 요일이_없으면_NULL로_저장하고_NULL은_빈_집합으로_읽는다() {
        assertThat(converter.convertToDatabaseColumn(Set.of())).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
    }

    @Test
    void 저장한_값을_다시_읽으면_같은_요일이다() {
        assertThat(converter.convertToEntityAttribute("MON,THU")).containsExactly(Weekday.MON, Weekday.THU);
    }
}
