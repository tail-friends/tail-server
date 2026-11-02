package com.tailfriends.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.inventory.domain.InventoryUnit;
import com.tailfriends.inventory.dto.InventoryCreateResponse;
import com.tailfriends.inventory.dto.InventoryItemResponse;
import com.tailfriends.inventory.dto.InventoryListResponse;
import com.tailfriends.inventory.service.InventoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryController.class)
@WebMvcTestSupport
class InventoryControllerTest {

    private static final InventoryItemResponse CRICKET =
            new InventoryItemResponse(5L, "귀뚜라미", 20, InventoryUnit.COUNT, 30, true);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    @WithMockUserId(7L)
    void 목록을_명세_형식으로_돌려준다() throws Exception {
        given(inventoryService.getItems()).willReturn(new InventoryListResponse(List.of(CRICKET)));

        mockMvc.perform(get("/api/v1/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(5))
                .andExpect(jsonPath("$.data.items[0].name").value("귀뚜라미"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(20))
                .andExpect(jsonPath("$.data.items[0].unit").value("COUNT"))
                .andExpect(jsonPath("$.data.items[0].alertBelow").value(30))
                .andExpect(jsonPath("$.data.items[0].low").value(true));
    }

    @Test
    @WithMockUserId(7L)
    void 등록하면_201과_ID를_돌려주고_같은_이름이면_409() throws Exception {
        given(inventoryService.create(any())).willReturn(new InventoryCreateResponse(5L));

        mockMvc.perform(post("/api/v1/inventory").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"귀뚜라미\", \"quantity\": 100, \"unit\": \"COUNT\", \"alertBelow\": 30 }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(5));

        given(inventoryService.create(any())).willThrow(new BusinessException(ErrorCode.DUPLICATE_ITEM));
        mockMvc.perform(post("/api/v1/inventory").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"귀뚜라미\", \"quantity\": 100, \"unit\": \"COUNT\" }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_ITEM"));
    }

    @Test
    @WithMockUserId(7L)
    void 이름이_없거나_길거나_수량이_범위_밖이거나_단위가_틀리면_400() throws Exception {
        String[] bodies = {
                "{ \"quantity\": 1, \"unit\": \"COUNT\" }",
                "{ \"name\": \"%s\", \"quantity\": 1, \"unit\": \"COUNT\" }".formatted("귀".repeat(41)),
                "{ \"name\": \"귀뚜라미\", \"quantity\": -1, \"unit\": \"COUNT\" }",
                "{ \"name\": \"귀뚜라미\", \"quantity\": 100000, \"unit\": \"COUNT\" }",
                "{ \"name\": \"귀뚜라미\", \"quantity\": 1, \"unit\": \"BOX\" }"
        };
        for (String body : bodies) {
            mockMvc.perform(post("/api/v1/inventory").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        }
    }

    @Test
    @WithMockUserId(7L)
    void 채우면_수정된_품목을_돌려주고_없는_품목은_404() throws Exception {
        given(inventoryService.update(eq(5L), any())).willReturn(CRICKET);
        given(inventoryService.update(eq(99L), any()))
                .willThrow(new BusinessException(ErrorCode.INVENTORY_ITEM_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/inventory/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"add\": 100 }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("귀뚜라미"));
        mockMvc.perform(patch("/api/v1/inventory/99").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"add\": 100 }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("INVENTORY_ITEM_NOT_FOUND"));
        mockMvc.perform(patch("/api/v1/inventory/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"add\": 0 }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserId(7L)
    void 삭제하면_본문_없이_204() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/5"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(inventoryService).delete(5L);
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/inventory"))
                .andExpect(status().isUnauthorized());
    }
}
