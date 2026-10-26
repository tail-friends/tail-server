package com.tailfriends.schedule.dto;

import java.util.List;

public record ScheduleListResponse(
        List<ScheduleResponse> schedules
) {
}
