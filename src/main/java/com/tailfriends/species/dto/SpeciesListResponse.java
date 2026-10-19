package com.tailfriends.species.dto;

import java.util.List;

public record SpeciesListResponse(
        List<SpeciesResponse> species
) {
}
