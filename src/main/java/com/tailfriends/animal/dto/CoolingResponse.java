package com.tailfriends.animal.dto;

import com.tailfriends.animal.domain.Animal;
import java.time.LocalDate;

/**
 * 쿨링 기간. endDate가 null이면 끝나는 날 없이 계속이다.
 * 지금 쿨링 중인지는 feed.status가 COOLING인지로 본다.
 */
public record CoolingResponse(LocalDate startDate, LocalDate endDate) {

    /**
     * 쿨링을 정하지 않았으면 null.
     */
    public static CoolingResponse of(Animal animal) {
        if (animal.getCoolingStart() == null) {
            return null;
        }
        return new CoolingResponse(animal.getCoolingStart(), animal.getCoolingEnd());
    }
}
