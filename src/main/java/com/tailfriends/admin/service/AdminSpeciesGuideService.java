package com.tailfriends.admin.service;

import com.tailfriends.admin.dto.AdminSpeciesGuideRequest;
import com.tailfriends.admin.dto.AdminSpeciesGuideResponse;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.species.domain.GuideSection;
import com.tailfriends.species.domain.Species;
import com.tailfriends.species.domain.SpeciesGuide;
import com.tailfriends.species.repository.SpeciesGuideRepository;
import com.tailfriends.species.repository.SpeciesRepository;
import com.tailfriends.user.repository.UserRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSpeciesGuideService {

    private final CurrentUser currentUser;
    private final UserRepository userRepository;
    private final SpeciesRepository speciesRepository;
    private final SpeciesGuideRepository speciesGuideRepository;
    private final Clock clock;

    /**
     * 작성 현황(마지막 수정 날짜·운영자)을 담은 가이드. 작성 전이면 sections는 빈 목록이고 작성 현황은 null이다.
     */
    @Transactional(readOnly = true)
    public AdminSpeciesGuideResponse getGuide(Long speciesId) {
        Species species = findSpecies(speciesId);
        return AdminSpeciesGuideResponse.of(species, speciesGuideRepository.findAllBySpeciesId(speciesId));
    }

    /**
     * 보낸 섹션으로 가이드 전체를 바꾼다. 없던 섹션은 만들고, 보내지 않은 섹션은 지운다.
     * 내용이 같은 섹션은 수정 날짜가 바뀌지 않는다. 바뀐 섹션이 하나라도 있으면 가이드의 마지막 수정 시각과
     * 운영자를 지금 요청한 운영자로 바꾼다.
     */
    @Transactional
    public AdminSpeciesGuideResponse saveGuide(Long speciesId, AdminSpeciesGuideRequest request) {
        Long editorId = currentUser.id();
        Species species = findSpecies(speciesId);

        Map<GuideSection, String> bodies = new EnumMap<>(GuideSection.class);
        for (AdminSpeciesGuideRequest.Section section : request.sections()) {
            if (bodies.put(section.key(), section.body()) != null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 섹션을 두 번 보낼 수 없어요");
            }
        }

        boolean changed = false;
        List<SpeciesGuide> saved = new ArrayList<>();
        for (SpeciesGuide guide : speciesGuideRepository.findAllBySpeciesId(speciesId)) {
            String body = bodies.remove(guide.getSection());
            if (body == null) {
                speciesGuideRepository.delete(guide);
                changed = true;
            } else {
                changed |= !body.equals(guide.getBody());
                guide.changeBody(body);
                saved.add(guide);
            }
        }
        changed |= !bodies.isEmpty();
        bodies.forEach((section, body) ->
                saved.add(speciesGuideRepository.save(SpeciesGuide.of(species, section, body))));

        if (changed) {
            species.markGuideUpdated(userRepository.getReferenceById(editorId), clock.instant());
        }
        // 수정 날짜는 flush 때 채워지므로 응답을 만들기 전에 반영한다.
        speciesGuideRepository.flush();
        return AdminSpeciesGuideResponse.of(species, saved);
    }

    private Species findSpecies(Long speciesId) {
        return speciesRepository.findById(speciesId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPECIES_NOT_FOUND));
    }
}
