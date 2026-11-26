package com.tailfriends.auth.controller;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.auth.dto.OAuthLoginResponse;
import com.tailfriends.auth.dto.TokenResponse;
import com.tailfriends.auth.service.OAuthService;
import com.tailfriends.auth.service.TokenService;
import com.tailfriends.notification.service.PushTokenService;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@WebMvcTestSupport
@Import(RefreshTokenCookies.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OAuthService oauthService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private PushTokenService pushTokenService;

    @Test
    void 인증_없이_소셜_로그인하고_명세_형식으로_응답한다() throws Exception {
        given(oauthService.login(any(), anyBoolean())).willReturn(new OAuthLoginResponse("access", "refresh", true,
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
        given(oauthService.login(any(), anyBoolean())).willThrow(new BusinessException(ErrorCode.OAUTH_FAILED));

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
    void 재발급_요청에_본문과_쿠키_모두_리프레시_토큰이_없으면_401을_돌려준다() throws Exception {
        mockMvc.perform(put("/api/v1/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\" \"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
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
        verify(pushTokenService).unregister("push");
    }

    @Test
    @WithMockUserId(5L)
    void 로그아웃에_푸시_토큰이_없으면_푸시_토큰은_건드리지_않는다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"my-refresh\"}"))
                .andExpect(status().isNoContent());

        verify(pushTokenService, never()).unregister(any());
    }

    @Test
    void 웹_로그인은_리프레시_토큰을_httpOnly_쿠키로_주고_본문에서는_뺀다() throws Exception {
        given(oauthService.login(any(), anyBoolean())).willReturn(new OAuthLoginResponse("access", "refresh", false,
                new OAuthLoginResponse.LoginUser(1L, "운영자")));

        mockMvc.perform(post("/api/v1/auth/oauth/callback")
                        .header(RefreshTokenCookies.CLIENT_TYPE_HEADER, "WEB")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"KAKAO\",\"code\":\"code\","
                                + "\"redirectUri\":\"https://admin.example/cb\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access"))
                .andExpect(jsonPath("$.data.refreshToken").value(nullValue()))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        startsWith("refresh_token=refresh;"), containsString("Path=/api/v1/auth"),
                        containsString("HttpOnly"), containsString("Secure"), containsString("SameSite=Lax"))));
    }

    @Test
    void 앱_로그인은_쿠키를_주지_않는다() throws Exception {
        given(oauthService.login(any(), anyBoolean())).willReturn(new OAuthLoginResponse("access", "refresh", false,
                new OAuthLoginResponse.LoginUser(1L, "망고집사")));

        mockMvc.perform(post("/api/v1/auth/oauth/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"KAKAO\",\"code\":\"code\",\"redirectUri\":\"app://cb\"}"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void 웹_재발급은_본문_없이_쿠키의_리프레시_토큰을_쓰고_새_쿠키를_준다() throws Exception {
        given(tokenService.reissue("old-refresh")).willReturn(new TokenResponse("new-access", "new-refresh"));

        mockMvc.perform(put("/api/v1/auth/reissue")
                        .header(RefreshTokenCookies.CLIENT_TYPE_HEADER, "web")
                        .cookie(new Cookie("refresh_token", "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").value(nullValue()))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, startsWith("refresh_token=new-refresh;")));
    }

    @Test
    void 본문의_리프레시_토큰이_쿠키보다_먼저다() throws Exception {
        given(tokenService.reissue("body-refresh")).willReturn(new TokenResponse("new-access", "new-refresh"));

        mockMvc.perform(put("/api/v1/auth/reissue")
                        .cookie(new Cookie("refresh_token", "cookie-refresh"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"body-refresh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));
    }

    @Test
    @WithMockUserId(5L)
    void 웹_로그아웃은_쿠키의_리프레시_토큰을_폐기하고_쿠키를_지운다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(RefreshTokenCookies.CLIENT_TYPE_HEADER, "WEB")
                        .cookie(new Cookie("refresh_token", "cookie-refresh")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        allOf(startsWith("refresh_token=;"), containsString("Max-Age=0"))));

        verify(tokenService).revoke("cookie-refresh");
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
