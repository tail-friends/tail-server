package com.tailfriends.enclosure.dto;

public record EnclosureSummaryResponse(
        Long id,
        BackgroundResponse background,
        long decorationCount
) {
}
