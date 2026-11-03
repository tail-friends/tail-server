package com.tailfriends.enclosure.dto;

import java.util.List;

public record EnclosureListResponse(
        List<EnclosureSummaryResponse> enclosures
) {
}
