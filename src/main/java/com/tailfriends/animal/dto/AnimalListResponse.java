package com.tailfriends.animal.dto;

import java.util.List;

public record AnimalListResponse(
        List<AnimalSummaryResponse> animals
) {
}
