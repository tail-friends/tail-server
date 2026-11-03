package com.tailfriends.enclosure.service;

import com.tailfriends.enclosure.domain.BackgroundType;
import com.tailfriends.enclosure.domain.Enclosure;
import com.tailfriends.enclosure.dto.BackgroundResponse;
import com.tailfriends.enclosure.dto.EnclosureListResponse;
import com.tailfriends.enclosure.dto.EnclosureSummaryResponse;
import com.tailfriends.enclosure.repository.DecorationCount;
import com.tailfriends.enclosure.repository.EnclosureDecorationRepository;
import com.tailfriends.enclosure.repository.EnclosureRepository;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.species.domain.Home;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사육장과 장식 배치. 기능명세서 F-28. 사진 테마(F-27)와 합사(F-29)는 아직 없다.
 */
@Service
@RequiredArgsConstructor
public class EnclosureService {

    /**
     * 기본 테마가 먼저(desert → pond 순서), 그다음 사진 테마를 만든 순서.
     */
    private static final Comparator<Enclosure> LIST_ORDER = Comparator
            .comparingInt((Enclosure enclosure) ->
                    enclosure.getTheme() == null ? Integer.MAX_VALUE : enclosure.getTheme().ordinal())
            .thenComparing(Enclosure::getId);

    private final CurrentUser currentUser;
    private final EnclosureRepository enclosureRepository;
    private final EnclosureDecorationRepository decorationRepository;

    /**
     * 내 사육장. 기본 테마 사육장이 다 있지 않으면 없는 것을 만든다(처음 볼 때 5개).
     * 그래서 쓰기 트랜잭션이다.
     */
    @Transactional
    public EnclosureListResponse getEnclosures() {
        Long userId = currentUser.id();
        if (enclosureRepository.countByUserIdAndBackgroundType(userId, BackgroundType.BUILT_IN)
                < Home.values().length) {
            for (Home theme : Home.values()) {
                enclosureRepository.insertBuiltInIfAbsent(userId, theme.name());
            }
        }
        List<Enclosure> enclosures = enclosureRepository.findAllByUserId(userId).stream()
                .sorted(LIST_ORDER)
                .toList();
        Map<Long, Long> counts = decorationRepository
                .countByEnclosureIds(enclosures.stream().map(Enclosure::getId).toList()).stream()
                .collect(Collectors.toMap(DecorationCount::enclosureId, DecorationCount::count));
        return new EnclosureListResponse(enclosures.stream()
                .map(enclosure -> new EnclosureSummaryResponse(enclosure.getId(), BackgroundResponse.from(enclosure),
                        counts.getOrDefault(enclosure.getId(), 0L)))
                .toList());
    }
}
