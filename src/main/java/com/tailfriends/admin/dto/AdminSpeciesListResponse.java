package com.tailfriends.admin.dto;

import java.util.List;

public record AdminSpeciesListResponse(
        List<AdminSpeciesResponse> species
) {
}
