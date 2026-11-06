package com.tailfriends.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.admin.dto.AdminShopItemListResponse;
import com.tailfriends.admin.dto.AdminShopItemResponse;
import com.tailfriends.admin.service.AdminShopItemService;
import com.tailfriends.animal.domain.RenderStyle;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.point.domain.ShopItemCategory;
import com.tailfriends.user.domain.Role;
import com.tailfriends.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminShopItemController.class)
@WebMvcTestSupport
class AdminShopItemControllerTest {

    private static final AdminShopItemResponse ITEM = new AdminShopItemResponse(21L, "유목 S",
            ShopItemCategory.DRIFTWOOD, 50, "https://cdn.example.com/a.png", RenderStyle.ILLUSTRATION_2D, true);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private AdminShopItemService adminShopItemService;

    @BeforeEach
    void setUp() {
        // @WithMockUserId의 기본 ID(1)를 관리자로 둔다. 일반 사용자는 다른 ID로 테스트한다
        given(userRepository.findRoleById(1L)).willReturn(Optional.of(Role.ADMIN));
    }

    @Test
    @WithMockUserId(role = "ADMIN")
    void 관리자는_판매_중지한_것까지_목록을_본다() throws Exception {
        given(adminShopItemService.getItems()).willReturn(new AdminShopItemListResponse(List.of(ITEM)));

        mockMvc.perform(get("/api/v1/admin/shop-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(21))
                .andExpect(jsonPath("$.data.items[0].price").value(50))
                .andExpect(jsonPath("$.data.items[0].onSale").value(true));
    }

    @Test
    @WithMockUserId(role = "ADMIN")
    void 추가하면_201과_아이템을_돌려준다() throws Exception {
        given(adminShopItemService.create(any())).willReturn(ITEM);

        mockMvc.perform(post("/api/v1/admin/shop-items").contentType(MediaType.APPLICATION_JSON).content("""
                        { "name": "유목 S", "category": "DRIFTWOOD", "price": 50,
                          "imageUrl": "https://cdn.example.com/a.png" }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(21))
                .andExpect(jsonPath("$.data.renderStyle").value("ILLUSTRATION_2D"));
        verify(adminShopItemService).create(argThat(request ->
                request.category() == ShopItemCategory.DRIFTWOOD && request.renderStyle() == null));
    }

    @Test
    @WithMockUserId(role = "ADMIN")
    void 추가_요청이_규칙에_맞지_않으면_400() throws Exception {
        for (String body : List.of(
                "{ \"name\": \" \", \"category\": \"PLANT\", \"price\": 50, \"imageUrl\": \"https://a.png\" }",
                "{ \"name\": \"풀\", \"price\": 50, \"imageUrl\": \"https://a.png\" }",
                "{ \"name\": \"풀\", \"category\": \"PLANT\", \"price\": 0, \"imageUrl\": \"https://a.png\" }",
                "{ \"name\": \"풀\", \"category\": \"PLANT\", \"price\": 100001, \"imageUrl\": \"https://a.png\" }",
                "{ \"name\": \"풀\", \"category\": \"PLANT\", \"price\": 50, \"imageUrl\": \"http://a.png\" }",
                "{ \"name\": \"풀\", \"category\": \"PLANT\", \"price\": 50 }",
                "{ \"name\": \"풀\", \"category\": \"ROCK\", \"price\": 50, \"imageUrl\": \"https://a.png\" }")) {
            mockMvc.perform(post("/api/v1/admin/shop-items").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        }
        verify(adminShopItemService, never()).create(any());
    }

    @Test
    @WithMockUserId(role = "ADMIN")
    void 수정하면_아이템을_돌려주고_없으면_404() throws Exception {
        given(adminShopItemService.update(eq(21L), any())).willReturn(ITEM);
        given(adminShopItemService.update(eq(99L), any())).willThrow(new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/admin/shop-items/21").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"onSale\": false }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(21));
        verify(adminShopItemService).update(eq(21L), argThat(request ->
                Boolean.FALSE.equals(request.onSale()) && request.name() == null));
        mockMvc.perform(patch("/api/v1/admin/shop-items/99").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"price\": 80 }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ITEM_NOT_FOUND"));
    }

    @Test
    @WithMockUserId(role = "ADMIN")
    void 수정에_공백_이름이나_https가_아닌_주소는_400() throws Exception {
        for (String body : List.of("{ \"name\": \"  \" }", "{ \"imageUrl\": \"ftp://a.png\" }",
                "{ \"price\": 0 }")) {
            mockMvc.perform(patch("/api/v1/admin/shop-items/21").contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
        verify(adminShopItemService, never()).update(any(), any());
    }

    @Test
    @WithMockUserId(2L)
    void 일반_사용자는_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/shop-items"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mockMvc.perform(patch("/api/v1/admin/shop-items/21").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"onSale\": false }"))
                .andExpect(status().isForbidden());
        verify(adminShopItemService, never()).update(any(), any());
    }
}
