package com.tailfriends.enclosure.dto;

import java.util.List;

/**
 * 저장한 순서(그리는 순서)대로.
 */
public record DecorationListResponse(
        List<DecorationResponse> decorations
) {
}
