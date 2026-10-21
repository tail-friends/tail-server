package com.tailfriends.admin.service;

import com.tailfriends.admin.dto.AdminSpeciesGuideRequest;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.species.domain.GuideSection;
import com.tailfriends.species.domain.Species;
import com.tailfriends.species.domain.SpeciesGuide;
import com.tailfriends.species.dto.SpeciesGuideResponse;
import com.tailfriends.species.repository.SpeciesGuideRepository;
import com.tailfriends.species.repository.SpeciesRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSpeciesGuideService {

    private final SpeciesRepository speciesRepository;
    private final SpeciesGuideRepository speciesGuideRepository;

    /**
     * 보낸 섹션으로 가이드 전체를 바꾼다. 없던 섹션은 만들고, 보내지 않은 섹션은 지운다.
     * 내용이 같은 섹션은 수정 날짜가 바뀌지 않는다.
     */
    @Transactional
    public SpeciesGuideResponse saveGuide(Long speciesId, AdminSpeciesGuideRequest request) {
        Species species = speciesRepository.findById(speciesId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPECIES_NOT_FOUND));

        Map<GuideSection, String> bodies = new EnumMap<>(GuideSection.class);
        for (AdminSpeciesGuideRequest.Section section : request.sections()) {
            if (bodies.put(section.key(), section.body()) != null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 섹션을 두 번 보낼 수 없어요");
            }
        }

        List<SpeciesGuide> saved = new ArrayList<>();
        for (SpeciesGuide guide : speciesGuideRepository.findAllBySpeciesId(speciesId)) {
            String body = bodies.remove(guide.getSection());
            if (body == null) {
                speciesGuideRepository.delete(guide);
            } else {
                guide.changeBody(body);
                saved.add(guide);
            }
        }
        bodies.forEach((section, body) ->
                saved.add(speciesGuideRepository.save(SpeciesGuide.of(species, section, body))));

        // 수정 날짜는 flush 때 채워지므로 응답을 만들기 전에 반영한다.
        speciesGuideRepository.flush();
        return SpeciesGuideResponse.of(species, saved);
    }
}
