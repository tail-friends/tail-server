package com.tailfriends.log.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.domain.ShedStage;
import com.tailfriends.log.domain.Stool;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 기록 한 건. 종류에 해당하지 않는 필드는 응답에서 빠진다. memo는 항상 내려준다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LogResponse(
        Long id,
        LogType type,
        LocalDate date,
        String food,
        Integer count,
        Boolean refused,
        Boolean dusted,
        ShedStage stage,
        Boolean complete,
        BigDecimal value,
        Boolean full,
        Stool stool,
        BigDecimal hot,
        BigDecimal cool,
        Integer humidity,
        @JsonInclude(JsonInclude.Include.ALWAYS) String memo
) {

    public static LogResponse from(Log log) {
        return new LogResponse(
                log.getId(),
                log.getType(),
                log.getLogDate(),
                log.getFood(),
                log.getFoodCount(),
                log.getRefused(),
                log.getDusted(),
                log.getShedStage(),
                log.getShedComplete(),
                log.getWeightG(),
                log.getCleanFull(),
                log.getStool(),
                log.getHotC(),
                log.getCoolC(),
                log.getHumidity(),
                log.getMemo());
    }
}
