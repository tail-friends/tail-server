package com.tailfriends.species.service;

import com.tailfriends.species.dto.SpeciesListResponse;
import com.tailfriends.species.dto.SpeciesResponse;
import com.tailfriends.species.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpeciesService {

    private final SpeciesRepository speciesRepository;

    public SpeciesListResponse getSpeciesList() {
        return new SpeciesListResponse(speciesRepository.findAllByHiddenFalseOrderByIdAsc().stream()
                .map(SpeciesResponse::from)
                .toList());
    }
}
