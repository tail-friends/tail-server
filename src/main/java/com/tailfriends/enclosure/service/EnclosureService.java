package com.tailfriends.enclosure.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.enclosure.domain.BackgroundType;
import com.tailfriends.enclosure.domain.Enclosure;
import com.tailfriends.enclosure.domain.EnclosureAnimal;
import com.tailfriends.enclosure.domain.EnclosureDecoration;
import com.tailfriends.enclosure.domain.EnclosureObject;
import com.tailfriends.enclosure.dto.BackgroundResponse;
import com.tailfriends.enclosure.dto.DecorationListResponse;
import com.tailfriends.enclosure.dto.DecorationRequest;
import com.tailfriends.enclosure.dto.DecorationResponse;
import com.tailfriends.enclosure.dto.EnclosureAnimalsResponse;
import com.tailfriends.enclosure.dto.EnclosureAnimalsUpdateRequest;
import com.tailfriends.enclosure.dto.EnclosureListResponse;
import com.tailfriends.enclosure.dto.EnclosureResponse;
import com.tailfriends.enclosure.dto.DecorationsUpdateRequest;
import com.tailfriends.enclosure.dto.EnclosureSummaryResponse;
import com.tailfriends.enclosure.repository.DecorationCount;
import com.tailfriends.enclosure.repository.EnclosureAnimalRepository;
import com.tailfriends.enclosure.repository.EnclosureDecorationRepository;
import com.tailfriends.enclosure.repository.EnclosureObjectRepository;
import com.tailfriends.enclosure.repository.EnclosureRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.point.repository.ShopItemRepository;
import com.tailfriends.point.repository.UserItemRepository;
import com.tailfriends.species.domain.Home;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사육장, 장식 배치(F-28), 합사 보기(F-29). 사진 테마 저장·삭제(F-27)는 PhotoThemeService.
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
    private final EnclosureObjectRepository objectRepository;
    private final UserItemRepository userItemRepository;
    private final ShopItemRepository shopItemRepository;
    private final EnclosureAnimalRepository enclosureAnimalRepository;
    private final AnimalRepository animalRepository;

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

    /**
     * 배경과 장식(그리는 순서). 사진 테마면 배경에 사진에서 읽은 objects도 넣는다.
     * animalIds는 합사 보기로 함께 띄울 개체(저장한 순서)다.
     */
    @Transactional(readOnly = true)
    public EnclosureResponse getEnclosure(Long enclosureId) {
        Enclosure enclosure = enclosureRepository.findByIdAndUserId(enclosureId, currentUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENCLOSURE_NOT_FOUND));
        List<DecorationResponse> decorations = decorationRepository.findAllByEnclosureIdOrderByIdAsc(enclosureId)
                .stream()
                .map(DecorationResponse::from)
                .toList();
        List<EnclosureObject> objects = enclosure.isBuiltIn()
                ? List.of()
                : objectRepository.findAllByEnclosureIdOrderByIdAsc(enclosureId);
        return new EnclosureResponse(enclosure.getId(), BackgroundResponse.of(enclosure, objects), decorations,
                enclosureAnimalRepository.findAnimalIdsByEnclosureId(enclosureId));
    }

    /**
     * 합사 보기로 함께 띄울 개체를 통째로 바꾼다. 화면에서만 함께 보이므로 한 개체를 여러 사육장에 넣을 수 있다.
     * 같은 개체를 두 번 보내면 INVALID_INPUT, 내 개체가 아니거나 없는 개체가 하나라도 있으면 ANIMAL_NOT_FOUND.
     * 사육장 행을 잠가서, 같은 사육장의 설정이 동시에 와도 한쪽만 남는다.
     */
    @Transactional
    public EnclosureAnimalsResponse updateAnimals(Long enclosureId, EnclosureAnimalsUpdateRequest request) {
        Long userId = currentUser.id();
        Enclosure enclosure = enclosureRepository.findByIdAndUserIdForUpdate(enclosureId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENCLOSURE_NOT_FOUND));
        List<Long> animalIds = request.animalIds();
        if (new HashSet<>(animalIds).size() != animalIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 개체를 두 번 넣을 수 없어요");
        }
        Map<Long, Animal> owned = animalIds.isEmpty()
                ? Map.of()
                : animalRepository.findAllByIdInAndUserId(animalIds, userId).stream()
                        .collect(Collectors.toMap(Animal::getId, Function.identity()));
        if (owned.size() != animalIds.size()) {
            throw new BusinessException(ErrorCode.ANIMAL_NOT_FOUND);
        }

        enclosureAnimalRepository.deleteAllByEnclosureId(enclosureId);
        enclosureAnimalRepository.saveAll(animalIds.stream()
                .map(animalId -> EnclosureAnimal.of(enclosure, owned.get(animalId)))
                .toList());
        return new EnclosureAnimalsResponse(List.copyOf(animalIds));
    }

    /**
     * 장식 배치를 통째로 바꾼다. 산 아이템만 놓을 수 있고(판매를 중지했어도 산 것이면 된다), 하나라도 아니면 ITEM_NOT_OWNED.
     * 사육장 행을 잠가서, 같은 사육장의 저장이 동시에 와도 한쪽 배치만 남는다.
     */
    @Transactional
    public DecorationListResponse updateDecorations(Long enclosureId, DecorationsUpdateRequest request) {
        Long userId = currentUser.id();
        Enclosure enclosure = enclosureRepository.findByIdAndUserIdForUpdate(enclosureId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENCLOSURE_NOT_FOUND));
        Set<Long> ownedItemIds = userItemRepository.findItemIdsByUserId(userId);
        if (!request.decorations().stream().allMatch(decoration -> ownedItemIds.contains(decoration.itemId()))) {
            throw new BusinessException(ErrorCode.ITEM_NOT_OWNED);
        }

        decorationRepository.deleteAllByEnclosureId(enclosureId);
        List<EnclosureDecoration> saved = decorationRepository.saveAll(request.decorations().stream()
                .map(decoration -> toEntity(enclosure, decoration))
                .toList());
        return new DecorationListResponse(saved.stream().map(DecorationResponse::from).toList());
    }

    private EnclosureDecoration toEntity(Enclosure enclosure, DecorationRequest decoration) {
        return EnclosureDecoration.builder()
                .enclosure(enclosure)
                .item(shopItemRepository.getReferenceById(decoration.itemId()))
                .x(toRatio(decoration.x()))
                .y(toRatio(decoration.y()))
                .flip(Boolean.TRUE.equals(decoration.flip()))
                .build();
    }

    /**
     * DB 컬럼(NUMERIC(4,3))에 맞춰 소수점 셋째 자리로 반올림한다. 0~1은 요청 검증에서 확인했다.
     */
    private static BigDecimal toRatio(BigDecimal value) {
        return value.setScale(3, RoundingMode.HALF_UP);
    }
}
