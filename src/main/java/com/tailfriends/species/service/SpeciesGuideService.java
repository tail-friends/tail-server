package com.tailfriends.species.service;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.species.domain.GuideSection;
import com.tailfriends.species.domain.Species;
import com.tailfriends.species.domain.SpeciesGuide;
import com.tailfriends.species.dto.SpeciesGuideResponse;
import com.tailfriends.species.repository.SpeciesGuideRepository;
import com.tailfriends.species.repository.SpeciesRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpeciesGuideService {

    private final SpeciesRepository speciesRepository;
    private final SpeciesGuideRepository speciesGuideRepository;

    /**
     * 숨긴 종도 이미 등록된 개체가 있을 수 있어 가이드를 보여준다.
     * section을 주면 그 섹션만 담는다. 작성된 섹션이 없으면 sections는 빈 목록이다.
     */
    public SpeciesGuideResponse getGuide(Long speciesId, GuideSection section) {
        Species species = speciesRepository.findById(speciesId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPECIES_NOT_FOUND));
        List<SpeciesGuide> guides = speciesGuideRepository.findAllBySpeciesId(speciesId).stream()
                .filter(guide -> section == null || guide.getSection() == section)
                .toList();
        return SpeciesGuideResponse.of(species, guides);
    }
}
