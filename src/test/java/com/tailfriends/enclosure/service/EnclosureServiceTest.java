package com.tailfriends.enclosure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.enclosure.domain.BackgroundType;
import com.tailfriends.enclosure.domain.Enclosure;
import com.tailfriends.enclosure.domain.EnclosureDecoration;
import com.tailfriends.enclosure.dto.DecorationRequest;
import com.tailfriends.enclosure.dto.DecorationResponse;
import com.tailfriends.enclosure.dto.DecorationsUpdateRequest;
import com.tailfriends.enclosure.dto.EnclosureResponse;
import com.tailfriends.enclosure.dto.EnclosureSummaryResponse;
import com.tailfriends.enclosure.repository.DecorationCount;
import com.tailfriends.enclosure.repository.EnclosureDecorationRepository;
import com.tailfriends.enclosure.repository.EnclosureRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.point.domain.ShopItem;
import com.tailfriends.point.repository.ShopItemRepository;
import com.tailfriends.point.repository.UserItemRepository;
import com.tailfriends.species.domain.Home;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

class EnclosureServiceTest {

    private static final Long USER_ID = 1L;

    private final EnclosureRepository enclosureRepository = mock(EnclosureRepository.class);
    private final EnclosureDecorationRepository decorationRepository = mock(EnclosureDecorationRepository.class);
    private final UserItemRepository userItemRepository = mock(UserItemRepository.class);
    private final ShopItemRepository shopItemRepository = mock(ShopItemRepository.class);
    private final EnclosureService service = new EnclosureService(CurrentUserStub.of(USER_ID), enclosureRepository,
            decorationRepository, userItemRepository, shopItemRepository);

    @Test
    void 기본_테마_사육장이_다_있지_않으면_다섯_테마를_모두_넣어_본다() {
        given(enclosureRepository.countByUserIdAndBackgroundType(USER_ID, BackgroundType.BUILT_IN)).willReturn(2L);
        given(enclosureRepository.findAllByUserId(USER_ID)).willReturn(List.of());

        service.getEnclosures();

        for (Home theme : Home.values()) {
            verify(enclosureRepository).insertBuiltInIfAbsent(USER_ID, theme.name());
        }
    }

    @Test
    void 기본_테마_사육장이_다_있으면_넣지_않는다() {
        given(enclosureRepository.countByUserIdAndBackgroundType(USER_ID, BackgroundType.BUILT_IN)).willReturn(5L);
        given(enclosureRepository.findAllByUserId(USER_ID)).willReturn(List.of());

        service.getEnclosures();

        verify(enclosureRepository, never()).insertBuiltInIfAbsent(anyLong(), anyString());
    }

    @Test
    void 목록은_id가_아니라_테마_순서이고_장식이_없으면_0개다() {
        given(enclosureRepository.countByUserIdAndBackgroundType(USER_ID, BackgroundType.BUILT_IN)).willReturn(5L);
        given(enclosureRepository.findAllByUserId(USER_ID)).willReturn(List.of(
                enclosure(9L, Home.POND), enclosure(3L, Home.FOREST), enclosure(5L, Home.DESERT)));
        given(decorationRepository.countByEnclosureIds(any())).willReturn(List.of(new DecorationCount(3L, 2)));

        List<EnclosureSummaryResponse> enclosures = service.getEnclosures().enclosures();

        assertThat(enclosures).extracting(EnclosureSummaryResponse::id).containsExactly(5L, 3L, 9L);
        assertThat(enclosures).extracting(enclosure -> enclosure.background().theme())
                .containsExactly("desert", "forest", "pond");
        assertThat(enclosures).extracting(EnclosureSummaryResponse::decorationCount).containsExactly(0L, 2L, 0L);
    }

    @Test
    void 상세는_장식을_그리는_순서로_주고_합사_전까지_animalIds는_비어_있다() {
        Enclosure desert = enclosure(3L, Home.DESERT);
        given(enclosureRepository.findByIdAndUserId(3L, USER_ID)).willReturn(Optional.of(desert));
        given(decorationRepository.findAllByEnclosureIdOrderByIdAsc(3L)).willReturn(List.of(
                decoration(desert, 21L, "0.300", true), decoration(desert, 22L, "0.850", false)));

        EnclosureResponse response = service.getEnclosure(3L);

        assertThat(response.background().type()).isEqualTo(BackgroundType.BUILT_IN);
        assertThat(response.decorations()).extracting(DecorationResponse::itemId).containsExactly(21L, 22L);
        assertThat(response.decorations().getFirst().x()).isEqualTo(0.3);
        assertThat(response.animalIds()).isEmpty();
    }

    @Test
    void 없거나_남의_사육장은_ENCLOSURE_NOT_FOUND() {
        given(enclosureRepository.findByIdAndUserId(99L, USER_ID)).willReturn(Optional.empty());
        given(enclosureRepository.findByIdAndUserIdForUpdate(99L, USER_ID)).willReturn(Optional.empty());

        assertError(() -> service.getEnclosure(99L), ErrorCode.ENCLOSURE_NOT_FOUND);
        assertError(() -> service.updateDecorations(99L, new DecorationsUpdateRequest(List.of())),
                ErrorCode.ENCLOSURE_NOT_FOUND);
    }

    @Test
    void 사지_않은_아이템이_하나라도_있으면_ITEM_NOT_OWNED이고_배치를_지우지_않는다() {
        given(enclosureRepository.findByIdAndUserIdForUpdate(3L, USER_ID))
                .willReturn(Optional.of(enclosure(3L, Home.DESERT)));
        given(userItemRepository.findItemIdsByUserId(USER_ID)).willReturn(Set.of(21L));

        assertError(() -> service.updateDecorations(3L, new DecorationsUpdateRequest(List.of(
                request(21L, "0.3", null), request(22L, "0.5", null)))), ErrorCode.ITEM_NOT_OWNED);
        verify(decorationRepository, never()).deleteAllByEnclosureId(anyLong());
    }

    @Test
    @SuppressWarnings("unchecked")
    void 배치를_지우고_보낸_순서대로_넣고_좌표는_셋째_자리로_반올림한다() {
        given(enclosureRepository.findByIdAndUserIdForUpdate(3L, USER_ID))
                .willReturn(Optional.of(enclosure(3L, Home.DESERT)));
        given(userItemRepository.findItemIdsByUserId(USER_ID)).willReturn(Set.of(21L, 22L));
        given(shopItemRepository.getReferenceById(anyLong()))
                .willAnswer(invocation -> shopItem(invocation.getArgument(0)));
        given(decorationRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

        List<DecorationResponse> saved = service.updateDecorations(3L, new DecorationsUpdateRequest(List.of(
                request(22L, "0.33333", true), request(21L, "0.9996", null), request(22L, "0", false))))
                .decorations();

        verify(decorationRepository).deleteAllByEnclosureId(3L);
        ArgumentCaptor<List<EnclosureDecoration>> captor = ArgumentCaptor.forClass(List.class);
        verify(decorationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(EnclosureDecoration::getX)
                .containsExactly(new BigDecimal("0.333"), new BigDecimal("1.000"), new BigDecimal("0.000"));
        assertThat(saved).extracting(DecorationResponse::itemId).containsExactly(22L, 21L, 22L);
        assertThat(saved).extracting(DecorationResponse::flip).containsExactly(true, false, false);
    }

    @Test
    void 빈_배열이면_모두_치운다() {
        given(enclosureRepository.findByIdAndUserIdForUpdate(3L, USER_ID))
                .willReturn(Optional.of(enclosure(3L, Home.DESERT)));
        given(userItemRepository.findItemIdsByUserId(USER_ID)).willReturn(Set.of());
        given(decorationRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.updateDecorations(3L, new DecorationsUpdateRequest(List.of())).decorations()).isEmpty();
        verify(decorationRepository).deleteAllByEnclosureId(3L);
    }

    private static Enclosure enclosure(Long id, Home theme) {
        Enclosure enclosure = BeanUtils.instantiateClass(Enclosure.class);
        ReflectionTestUtils.setField(enclosure, "id", id);
        ReflectionTestUtils.setField(enclosure, "backgroundType", BackgroundType.BUILT_IN);
        ReflectionTestUtils.setField(enclosure, "theme", theme);
        return enclosure;
    }

    private static EnclosureDecoration decoration(Enclosure enclosure, Long itemId, String x, boolean flip) {
        return EnclosureDecoration.builder()
                .enclosure(enclosure)
                .item(shopItem(itemId))
                .x(new BigDecimal(x))
                .y(new BigDecimal("0.850"))
                .flip(flip)
                .build();
    }

    private static ShopItem shopItem(Long id) {
        ShopItem item = BeanUtils.instantiateClass(ShopItem.class);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private static DecorationRequest request(Long itemId, String x, Boolean flip) {
        return new DecorationRequest(itemId, new BigDecimal(x), new BigDecimal("0.85"), flip);
    }

    private static void assertError(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(code);
    }
}
