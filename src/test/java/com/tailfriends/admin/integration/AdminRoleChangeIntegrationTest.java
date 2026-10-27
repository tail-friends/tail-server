package com.tailfriends.admin.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.Role;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 역할을 바꾸면 이미 받은 토큰으로도 바로 적용되는지 확인한다. 토큰의 역할은 발급할 때 값 그대로다.
 */
class AdminRoleChangeIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 관리자_권한을_빼면_같은_토큰으로_관리자_API를_못_쓴다() throws Exception {
        User user = saveUser(Role.ADMIN);
        String token = tokenOf(user);
        mockMvc.perform(get("/api/v1/admin/stats").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        changeRole(user, Role.KEEPER);

        mockMvc.perform(get("/api/v1/admin/stats").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void 관리자로_바꾸면_다시_로그인하지_않아도_관리자_API를_쓴다() throws Exception {
        User user = saveUser(Role.KEEPER);
        String token = tokenOf(user);
        mockMvc.perform(get("/api/v1/admin/stats").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());

        changeRole(user, Role.ADMIN);

        mockMvc.perform(get("/api/v1/admin/stats").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    @Test
    void 일반_API는_역할과_관계없이_그대로_쓴다() throws Exception {
        User user = saveUser(Role.ADMIN);
        String token = tokenOf(user);

        changeRole(user, Role.KEEPER);

        mockMvc.perform(get("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    // 운영에서 역할은 DB에서 직접 바꾼다
    private void changeRole(User user, Role role) {
        jdbcTemplate.update("update users set role = ? where id = ?", role.name(), user.getId());
    }

    private String tokenOf(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }

    private User saveUser(Role role) {
        User user = User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("사용자")
                .build();
        user.changeRole(role);
        return userRepository.save(user);
    }
}
