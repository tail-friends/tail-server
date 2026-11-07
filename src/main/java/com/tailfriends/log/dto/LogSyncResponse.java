package com.tailfriends.log.dto;

import java.util.List;

public record LogSyncResponse(
        List<LogSyncResult> results
) {
}
