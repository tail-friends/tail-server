package com.tailfriends.breeding.dto;

import com.tailfriends.breeding.domain.Clutch;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ClutchResponse(
        LocalDate laidDate,
        int eggCount,
        BigDecimal incubationTemp,
        LocalDate expectedHatchDate
) {

    public static ClutchResponse from(Clutch clutch) {
        return new ClutchResponse(clutch.getLaidDate(), clutch.getEggCount(), clutch.getIncubationTemp(),
                clutch.getExpectedHatchDate());
    }
}
