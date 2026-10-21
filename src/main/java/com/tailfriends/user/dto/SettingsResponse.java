package com.tailfriends.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tailfriends.user.domain.AlertRules;
import com.tailfriends.user.domain.TempUnit;
import java.time.LocalTime;

/**
 * 노션 명세 형식. keepOriginalDefault·pushEnabled는 동의(원본 사진 보관·알림 수신)의 현재 값이다.
 */
public record SettingsResponse(
        @JsonFormat(pattern = "HH:mm") LocalTime notifyTime,
        TempUnit tempUnit,
        boolean keepOriginalDefault,
        AlertRules alertRules,
        boolean pushEnabled
) {
}
