package com.tailfriends.enclosure.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class EnclosureAnimalIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 합사_개체를_설정하면_상세에_보낸_순서로_나오고_다시_설정하면_통째로_바뀐다() throws Exception {
        String token = tokenOf(newUser());
        long mango = createAnimal(token, "망고");
        long choco = createAnimal(token, "초코");
        long lemon = createAnimal(token, "레몬");
        long desert = enclosureId(token, 0);

        setAnimals(token, desert, "[%d, %d]".formatted(choco, mango))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.animalIds").value(contains((int) choco, (int) mango)));
        detail(token, desert).andExpect(jsonPath("$.data.animalIds").value(contains((int) choco, (int) mango)));

        setAnimals(token, desert, "[%d]".formatted(lemon)).andExpect(status().isOk());
        detail(token, desert).andExpect(jsonPath("$.data.animalIds").value(contains((int) lemon)));

        setAnimals(token, desert, "[]").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.animalIds").isEmpty());
        detail(token, desert).andExpect(jsonPath("$.data.animalIds").isEmpty());
        assertThat(count("select count(*) from enclosure_animals where enclosure_id = ?", desert)).isZero();
    }

    @Test
    void 한_개체를_여러_사육장에_넣을_수_있다() throws Exception {
        String token = tokenOf(newUser());
        long mango = createAnimal(token, "망고");
        long desert = enclosureId(token, 0);
        long forest = enclosureId(token, 1);

        setAnimals(token, desert, "[%d]".formatted(mango)).andExpect(status().isOk());
        setAnimals(token, forest, "[%d]".formatted(mango)).andExpect(status().isOk());

        detail(token, desert).andExpect(jsonPath("$.data.animalIds").value(contains((int) mango)));
        detail(token, forest).andExpect(jsonPath("$.data.animalIds").value(contains((int) mango)));
    }

    @Test
    void 남의_개체나_같은_개체_두_번이나_5마리는_거절하고_원래_설정이_남는다() throws Exception {
        String token = tokenOf(newUser());
        String other = tokenOf(newUser());
        long mango = createAnimal(token, "망고");
        long stranger = createAnimal(other, "남의 개체");
        long desert = enclosureId(token, 0);
        setAnimals(token, desert, "[%d]".formatted(mango)).andExpect(status().isOk());

        setAnimals(token, desert, "[%d, %d]".formatted(mango, stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
        setAnimals(token, desert, "[%d, %d]".formatted(mango, mango))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        setAnimals(token, desert, "[1, 2, 3, 4, 5]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));

        detail(token, desert).andExpect(jsonPath("$.data.animalIds").value(contains((int) mango)));
    }

    @Test
    void 남의_사육장에는_설정할_수_없다() throws Exception {
        String token = tokenOf(newUser());
        String other = tokenOf(newUser());
        long otherAnimal = createAnimal(other, "초코");
        long desert = enclosureId(token, 0);

        setAnimals(other, desert, "[%d]".formatted(otherAnimal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ENCLOSURE_NOT_FOUND"));
        assertThat(count("select count(*) from enclosure_animals where enclosure_id = ?", desert)).isZero();
    }

    @Test
    void 개체를_지우면_합사에서도_빠진다() throws Exception {
        String token = tokenOf(newUser());
        long mango = createAnimal(token, "망고");
        long choco = createAnimal(token, "초코");
        long desert = enclosureId(token, 0);
        setAnimals(token, desert, "[%d, %d]".formatted(mango, choco)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/animals/" + mango).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        detail(token, desert).andExpect(jsonPath("$.data.animalIds").value(contains((int) choco)));
    }

    @Test
    void 탈퇴하면_합사_설정도_지워진다() throws Exception {
        User keeper = newUser();
        String token = tokenOf(keeper);
        long mango = createAnimal(token, "망고");
        long desert = enclosureId(token, 0);
        setAnimals(token, desert, "[%d]".formatted(mango)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        assertThat(count("select count(*) from enclosure_animals where enclosure_id = ?", desert)).isZero();
        assertThat(count("select count(*) from animals where user_id = ?", keeper.getId())).isZero();
    }

    private ResultActions setAnimals(String token, long enclosureId, String animalIds) throws Exception {
        return mockMvc.perform(put("/api/v1/enclosures/" + enclosureId + "/animals")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"animalIds\": " + animalIds + " }"));
    }

    private ResultActions detail(String token, long enclosureId) throws Exception {
        return mockMvc.perform(get("/api/v1/enclosures/" + enclosureId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    private long enclosureId(String token, int index) throws Exception {
        String body = mockMvc.perform(get("/api/v1/enclosures").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.enclosures[" + index + "].id")).longValue();
    }

    private long createAnimal(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"%s\", \"speciesId\": 1, \"feedIntervalDays\": 4 }".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private int count(String sql, long id) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return value == null ? 0 : value;
    }

    private User newUser() {
        return userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("사용자")
                .build());
    }

    private String tokenOf(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
