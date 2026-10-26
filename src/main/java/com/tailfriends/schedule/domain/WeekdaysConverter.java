package com.tailfriends.schedule.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 요일 집합을 "MON,THU"처럼 월요일부터 쉼표로 이어 저장한다. 비어 있으면 NULL.
 */
@Converter
public class WeekdaysConverter implements AttributeConverter<Set<Weekday>, String> {

    @Override
    public String convertToDatabaseColumn(Set<Weekday> days) {
        if (days == null || days.isEmpty()) {
            return null;
        }
        return EnumSet.copyOf(days).stream().map(Weekday::name).collect(Collectors.joining(","));
    }

    @Override
    public Set<Weekday> convertToEntityAttribute(String column) {
        EnumSet<Weekday> days = EnumSet.noneOf(Weekday.class);
        if (column != null && !column.isBlank()) {
            Arrays.stream(column.split(",")).map(Weekday::valueOf).forEach(days::add);
        }
        return days;
    }
}
