package com.tailfriends.log.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 오프라인에 쌓인 기록. 항목은 기록 작성과 같은 형식이고, 항목의 검증은 서비스가 항목마다 따로 한다.
 */
public record LogSyncRequest(
        @NotNull @NotEmpty @Size(max = 200) List<@NotNull LogCreateRequest> logs
) {
}
