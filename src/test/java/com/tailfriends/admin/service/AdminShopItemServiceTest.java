package com.tailfriends.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tailfriends.admin.dto.AdminShopItemCreateRequest;
import com.tailfriends.admin.dto.AdminShopItemResponse;
import com.tailfriends.admin.dto.AdminShopItemUpdateRequest;
import com.tailfriends.animal.domain.RenderStyle;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.point.domain.ShopItem;
import com.tailfriends.point.domain.ShopItemCategory;
import com.tailfriends.point.repository.ShopItemRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdminShopItemServiceTest {

    private static final String IMAGE_URL = "https://cdn.example.com/driftwood-s.png";

    private final ShopItemRepository shopItemRepository = mock(ShopItemRepository.class);
    private final AdminShopItemService service = new AdminShopItemService(shopItemRepository);

    @Test
    void 이름의_앞뒤_공백을_빼고_그림_방식과_판매_여부를_생략하면_기본값으로_추가한다() {
        given(shopItemRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        AdminShopItemResponse response = service.create(new AdminShopItemCreateRequest(
                " 유목 S ", ShopItemCategory.DRIFTWOOD, 50, IMAGE_URL, null, null));

        assertThat(response.name()).isEqualTo("유목 S");
        assertThat(response.renderStyle()).isEqualTo(RenderStyle.ILLUSTRATION_2D);
        assertThat(response.onSale()).isTrue();
    }

    @Test
    void 판매_중지_상태로_추가할_수_있다() {
        given(shopItemRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.create(new AdminShopItemCreateRequest(
                "유목 S", ShopItemCategory.DRIFTWOOD, 50, IMAGE_URL, null, false)).onSale()).isFalse();
    }

    @Test
    void 보낸_필드만_바꾸고_판매를_중지할_수_있다() {
        ShopItem item = ShopItem.builder()
                .name("유목 S")
                .category(ShopItemCategory.DRIFTWOOD)
                .price(50)
                .imageUrl(IMAGE_URL)
                .renderStyle(RenderStyle.ILLUSTRATION_2D)
                .onSale(true)
                .build();
        given(shopItemRepository.findById(21L)).willReturn(Optional.of(item));

        AdminShopItemResponse response = service.update(21L,
                new AdminShopItemUpdateRequest(" 유목 M ", null, 80, null, null, false));

        assertThat(response.name()).isEqualTo("유목 M");
        assertThat(response.price()).isEqualTo(80);
        assertThat(response.onSale()).isFalse();
        assertThat(response.category()).isEqualTo(ShopItemCategory.DRIFTWOOD);
        assertThat(response.imageUrl()).isEqualTo(IMAGE_URL);
    }

    @Test
    void 없는_아이템을_고치면_ITEM_NOT_FOUND() {
        given(shopItemRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L,
                new AdminShopItemUpdateRequest(null, null, 80, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ITEM_NOT_FOUND);
    }
}
