package com.tailfriends.admin.service;

import com.tailfriends.admin.dto.AdminSpeciesCreateRequest;
import com.tailfriends.admin.dto.AdminSpeciesListResponse;
import com.tailfriends.admin.dto.AdminSpeciesResponse;
import com.tailfriends.admin.dto.AdminSpeciesUpdateRequest;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.species.domain.Species;
import com.tailfriends.species.domain.SpeciesChange;
import com.tailfriends.species.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSpeciesService {

    private static final String DUPLICATE_NAME_MESSAGE = "이미 있는 종 이름이에요";

    private final SpeciesRepository speciesRepository;

    /**
     * 숨긴 종까지 모두.
     */
    public AdminSpeciesListResponse getSpeciesList() {
        return new AdminSpeciesListResponse(speciesRepository.findAllByOrderByIdAsc().stream()
                .map(AdminSpeciesResponse::from)
                .toList());
    }

    @Transactional
    public AdminSpeciesResponse create(AdminSpeciesCreateRequest request) {
        String name = request.name().strip();
        if (speciesRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, DUPLICATE_NAME_MESSAGE);
        }
        Species species = speciesRepository.save(Species.builder()
                .name(name)
                .shape(request.shape())
                .spiky(Boolean.TRUE.equals(request.spiky()))
                .feedIntervalDays(request.feedIntervalDays())
                .shedIntervalDays(request.shedIntervalDays())
                .hotMin(request.hotMin())
                .hotMax(request.hotMax())
                .coolMin(request.coolMin())
                .coolMax(request.coolMax())
                .humidityMin(request.humidityMin())
                .humidityMax(request.humidityMax())
                .defaultHome(request.defaultHome())
                .build());
        return AdminSpeciesResponse.from(species);
    }

    @Transactional
    public AdminSpeciesResponse update(Long speciesId, AdminSpeciesUpdateRequest request) {
        Species species = findSpecies(speciesId);
        SpeciesChange change = request.toChange();
        if (change.name() != null && speciesRepository.existsByNameAndIdNot(change.name(), speciesId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, DUPLICATE_NAME_MESSAGE);
        }
        species.update(change);
        return AdminSpeciesResponse.from(species);
    }

    /**
     * 삭제 대신 숨긴다. 이미 이 종으로 등록된 개체는 그대로다.
     */
    @Transactional
    public void hide(Long speciesId) {
        findSpecies(speciesId).hide();
    }

    private Species findSpecies(Long speciesId) {
        return speciesRepository.findById(speciesId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPECIES_NOT_FOUND));
    }
}
