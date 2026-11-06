package com.tailfriends.point.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.RenderStyle;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.point.domain.PointHistory;
import com.tailfriends.point.domain.PointReason;
import com.tailfriends.point.domain.ShopItem;
import com.tailfriends.point.domain.ShopItemCategory;
import com.tailfriends.point.domain.UserItem;
import com.tailfriends.point.dto.MyItemResponse;
import com.tailfriends.point.dto.ShopItemResponse;
import com.tailfriends.point.repository.PointHistoryRepository;
import com.tailfriends.point.repository.ShopItemRepository;
import com.tailfriends.point.repository.UserItemRepository;
import com.tailfriends.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

class ShopServiceTest {

    private static final Long USER_ID = 1L;

    private final ShopItemRepository shopItemRepository = mock(ShopItemRepository.class);
    private final UserItemRepository userItemRepository = mock(UserItemRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PointHistoryRepository pointHistoryRepository = mock(PointHistoryRepository.class);
    private final ShopService service = new ShopService(CurrentUserStub.of(USER_ID), shopItemRepository,
            userItemRepository, userRepository, pointHistoryRepository);

    @Test
    void 산_아이템만_owned가_true다() {
        given(shopItemRepository.findAllByOnSaleTrueOrderByPriceAscIdAsc())
                .willReturn(List.of(item(21L, 50), item(22L, 80)));
        given(userItemRepository.findItemIdsByUserId(USER_ID)).willReturn(Set.of(22L));

        List<ShopItemResponse> items = service.getItems(null).items();

        assertThat(items).extracting(ShopItemResponse::id).containsExactly(21L, 22L);
        assertThat(items).extracting(ShopItemResponse::owned).containsExactly(false, true);
    }

    @Test
    void 종류를_주면_그_종류만_조회한다() {
        given(shopItemRepository.findAllByOnSaleTrueAndCategoryOrderByPriceAscIdAsc(ShopItemCategory.PLANT))
                .willReturn(List.of(item(23L, 30)));
        given(userItemRepository.findItemIdsByUserId(USER_ID)).willReturn(Set.of());

        assertThat(service.getItems(ShopItemCategory.PLANT).items()).extracting(ShopItemResponse::id)
                .containsExactly(23L);
        verify(shopItemRepository, never()).findAllByOnSaleTrueOrderByPriceAscIdAsc();
    }

    @Test
    void 사면_가격만큼_빼고_내역을_남기고_잔액을_돌려준다() {
        given(shopItemRepository.findByIdAndOnSaleTrue(21L)).willReturn(Optional.of(item(21L, 50)));
        given(userRepository.usePoints(USER_ID, 50)).willReturn(1);
        given(userItemRepository.insertIfAbsent(USER_ID, 21L)).willReturn(1);
        given(userRepository.findPointsById(USER_ID)).willReturn(Optional.of(70));

        assertThat(service.purchase(21L).balance()).isEqualTo(70);

        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualTo(-50);
        assertThat(captor.getValue().getReason()).isEqualTo(PointReason.PURCHASE);
        assertThat(captor.getValue().getEarnedDate()).isNull();
    }

    @Test
    void 없거나_판매를_중지한_아이템은_ITEM_NOT_FOUND() {
        given(shopItemRepository.findByIdAndOnSaleTrue(99L)).willReturn(Optional.empty());

        assertError(() -> service.purchase(99L), ErrorCode.ITEM_NOT_FOUND);
        verify(userRepository, never()).usePoints(anyLong(), anyInt());
    }

    @Test
    void 이미_샀으면_잔액이_모자라도_ALREADY_OWNED이고_포인트를_빼지_않는다() {
        given(shopItemRepository.findByIdAndOnSaleTrue(21L)).willReturn(Optional.of(item(21L, 50)));
        given(userItemRepository.existsByUserIdAndItemId(USER_ID, 21L)).willReturn(true);
        given(userRepository.usePoints(USER_ID, 50)).willReturn(0);

        assertError(() -> service.purchase(21L), ErrorCode.ALREADY_OWNED);
        verify(userRepository, never()).usePoints(anyLong(), anyInt());
    }

    @Test
    void 잔액이_모자라면_NOT_ENOUGH_POINTS이고_아이템을_넣지_않는다() {
        given(shopItemRepository.findByIdAndOnSaleTrue(21L)).willReturn(Optional.of(item(21L, 50)));
        given(userRepository.usePoints(USER_ID, 50)).willReturn(0);

        assertError(() -> service.purchase(21L), ErrorCode.NOT_ENOUGH_POINTS);
        verify(userItemRepository, never()).insertIfAbsent(anyLong(), anyLong());
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    void 같은_아이템을_동시에_사서_먼저_들어갔으면_ALREADY_OWNED이고_내역을_남기지_않는다() {
        given(shopItemRepository.findByIdAndOnSaleTrue(21L)).willReturn(Optional.of(item(21L, 50)));
        given(userRepository.usePoints(USER_ID, 50)).willReturn(1);
        given(userItemRepository.insertIfAbsent(USER_ID, 21L)).willReturn(0);

        // 뺀 포인트는 예외로 트랜잭션이 롤백되며 돌아온다 (통합 테스트에서 확인)
        assertError(() -> service.purchase(21L), ErrorCode.ALREADY_OWNED);
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    void 보유_목록의_id는_상점_아이템_ID이고_purchasedAt은_산_때다() {
        Instant purchasedAt = Instant.parse("2026-10-17T12:00:00Z");
        UserItem owned = BeanUtils.instantiateClass(UserItem.class);
        ReflectionTestUtils.setField(owned, "id", 7L);
        ReflectionTestUtils.setField(owned, "item", item(21L, 50));
        ReflectionTestUtils.setField(owned, "createdAt", purchasedAt);
        given(userItemRepository.findAllWithItemByUserId(USER_ID)).willReturn(List.of(owned));

        MyItemResponse response = service.getMyItems().items().getFirst();

        assertThat(response.id()).isEqualTo(21L);
        assertThat(response.name()).isEqualTo("유목 S");
        assertThat(response.purchasedAt()).isEqualTo(purchasedAt);
    }

    private static ShopItem item(Long id, int price) {
        ShopItem item = ShopItem.builder()
                .name("유목 S")
                .category(ShopItemCategory.DRIFTWOOD)
                .price(price)
                .imageUrl("https://cdn.example.com/driftwood-s.png")
                .renderStyle(RenderStyle.ILLUSTRATION_2D)
                .onSale(true)
                .build();
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private static void assertError(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(code);
    }
}
