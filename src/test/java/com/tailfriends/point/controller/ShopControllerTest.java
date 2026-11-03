package com.tailfriends.point.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.animal.domain.RenderStyle;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.point.domain.ShopItemCategory;
import com.tailfriends.point.dto.MyItemListResponse;
import com.tailfriends.point.dto.MyItemResponse;
import com.tailfriends.point.dto.PurchaseResponse;
import com.tailfriends.point.dto.ShopItemListResponse;
import com.tailfriends.point.dto.ShopItemResponse;
import com.tailfriends.point.service.ShopService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShopController.class)
@WebMvcTestSupport
class ShopControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShopService shopService;

    @Test
    @WithMockUserId(7L)
    void 상점_목록을_명세_형식으로_돌려준다() throws Exception {
        given(shopService.getItems(null)).willReturn(new ShopItemListResponse(List.of(new ShopItemResponse(21L,
                "유목 S", ShopItemCategory.DRIFTWOOD, 50, "https://cdn.example.com/a.png",
                RenderStyle.ILLUSTRATION_2D, true))));

        mockMvc.perform(get("/api/v1/points/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(21))
                .andExpect(jsonPath("$.data.items[0].name").value("유목 S"))
                .andExpect(jsonPath("$.data.items[0].category").value("DRIFTWOOD"))
                .andExpect(jsonPath("$.data.items[0].price").value(50))
                .andExpect(jsonPath("$.data.items[0].imageUrl").value("https://cdn.example.com/a.png"))
                .andExpect(jsonPath("$.data.items[0].renderStyle").value("ILLUSTRATION_2D"))
                .andExpect(jsonPath("$.data.items[0].owned").value(true));
    }

    @Test
    @WithMockUserId(7L)
    void 종류로_거르고_없는_종류는_400() throws Exception {
        given(shopService.getItems(ShopItemCategory.PLANT)).willReturn(new ShopItemListResponse(List.of()));

        mockMvc.perform(get("/api/v1/points/items").param("category", "PLANT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
        mockMvc.perform(get("/api/v1/points/items").param("category", "ROCK"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        // 없는 종류는 서비스까지 가지 않는다 (PLANT 한 번만)
        verify(shopService).getItems(any());
    }

    @Test
    @WithMockUserId(7L)
    void 사면_잔액을_돌려준다() throws Exception {
        given(shopService.purchase(21L)).willReturn(new PurchaseResponse(70));

        mockMvc.perform(post("/api/v1/points/items/21/purchase"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(70));
    }

    @Test
    @WithMockUserId(7L)
    void 구매_실패는_명세의_에러_코드로_돌려준다() throws Exception {
        given(shopService.purchase(21L)).willThrow(new BusinessException(ErrorCode.NOT_ENOUGH_POINTS));
        given(shopService.purchase(22L)).willThrow(new BusinessException(ErrorCode.ALREADY_OWNED));
        given(shopService.purchase(99L)).willThrow(new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        mockMvc.perform(post("/api/v1/points/items/21/purchase"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("NOT_ENOUGH_POINTS"));
        mockMvc.perform(post("/api/v1/points/items/22/purchase"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_OWNED"));
        mockMvc.perform(post("/api/v1/points/items/99/purchase"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ITEM_NOT_FOUND"));
    }

    @Test
    @WithMockUserId(7L)
    void 보유_아이템을_명세_형식으로_돌려준다() throws Exception {
        given(shopService.getMyItems()).willReturn(new MyItemListResponse(List.of(new MyItemResponse(21L, "유목 S",
                ShopItemCategory.DRIFTWOOD, "https://cdn.example.com/a.png", Instant.parse("2026-10-17T12:00:00Z")))));

        mockMvc.perform(get("/api/v1/points/my-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(21))
                .andExpect(jsonPath("$.data.items[0].category").value("DRIFTWOOD"))
                .andExpect(jsonPath("$.data.items[0].purchasedAt").value("2026-10-17T12:00:00Z"));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/points/items"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/points/items/21/purchase"))
                .andExpect(status().isUnauthorized());
    }
}
