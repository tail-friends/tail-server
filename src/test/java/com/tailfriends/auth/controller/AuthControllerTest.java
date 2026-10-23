package com.tailfriends.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.auth.dto.OAuthLoginResponse;
import com.tailfriends.auth.dto.TokenResponse;
import com.tailfriends.auth.service.OAuthService;
import com.tailfriends.auth.service.TokenService;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@WebMvcTestSupport
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OAuthService oauthService;

    @MockitoBean
    private TokenService tokenService;

    @Test
    void 인증_없이_소셜_로그인하고_명세_형식으로_응답한다() throws Exception {
        given(oauthService.login(any())).willReturn(new OAuthLoginResponse("access", "refresh", true,
                new OAuthLoginResponse.LoginUser(1L, "망고집사")));

        mockMvc.perform(post("/api/v1/auth/oauth/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "provider": "KAKAO", "code": "auth-code", "redirectUri": "https://app.example/callback" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.data.isNewUser").value(true))
                .andExpect(jsonPath("$.data.user.id").value(1))
                .andExpect(jsonPath("$.data.user.nickname").value("망고집사"));
    }

    @Test
    void 로그인_방식이_정해진_값이_아니면_400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/oauth/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "provider": "NAVER", "code": "auth-code", "redirectUri": "https://app.example/callback" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 인가_코드가_틀리면_401_OAUTH_FAILED() throws Exception {
        given(oauthService.login(any())).willThrow(new BusinessException(ErrorCode.OAUTH_FAILED));

        mockMvc.perform(post("/api/v1/auth/oauth/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "provider": "GOOGLE", "code": "expired", "redirectUri": "https://app.example/callback" }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("OAUTH_FAILED"));
    }

    @Test
    void 인증_없이_토큰을_재발급한다() throws Exception {
        given(tokenService.reissue("old-refresh")).willReturn(new TokenResponse("new-access", "new-refresh"));

        mockMvc.perform(put("/api/v1/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"old-refresh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));
    }

    @Test
    void 유효하지_않은_리프레시_토큰이면_401을_돌려준다() throws Exception {
        given(tokenService.reissue(any())).willThrow(new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        mockMvc.perform(put("/api/v1/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void 재발급_요청에_리프레시_토큰이_없으면_400을_돌려준다() throws Exception {
        mockMvc.perform(put("/api/v1/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId(5L)
    void 로그아웃하면_내_리프레시_토큰을_폐기하고_204를_돌려준다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"my-refresh\",\"pushToken\":\"push\"}"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(tokenService).revoke("my-refresh");
    }

    @Test
    void 로그아웃은_인증이_필요하다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"my-refresh\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
