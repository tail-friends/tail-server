package com.tailfriends.breeding.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class BreedingIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.now(KstDate.ZONE);
    private static final LocalDate PAIRED = TODAY.minusDays(60);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 메이팅부터_산란_해칭까지_기록하고_새끼는_어미의_종과_급여_주기로_등록된다() throws Exception {
        String token = tokenOfNewUser();
        long female = createAnimal(token, "레몬", 5);
        long breedingId = createBreeding(token, """
                { "maleName": "분양처 수컷", "femaleId": %d, "pairedDate": "%s", "memo": "첫 메이팅" }
                """.formatted(female, PAIRED));

        perform(token, patch("/api/v1/breeding/" + breedingId), """
                { "status": "INCUBATING",
                  "clutches": [ { "laidDate": "%s", "eggCount": 2, "incubationTemp": 30.0,
                                  "expectedHatchDate": "%s" } ] }
                """.formatted(PAIRED.plusDays(30), PAIRED.plusDays(85)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INCUBATING"))
                .andExpect(jsonPath("$.data.clutches[0].eggCount").value(2))
                .andExpect(jsonPath("$.data.clutches[0].incubationTemp").value(30.0));

        String body = perform(token, post("/api/v1/breeding/" + breedingId + "/hatchlings"), """
                { "hatchDate": "%s", "hatchlings": [ { "name": "레몬 2세 A", "morph": "탠저린" }, { "name": "레몬 2세 B" } ] }
                """.formatted(TODAY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.animalIds", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        List<Number> hatchlings = JsonPath.read(body, "$.data.animalIds");
        long first = hatchlings.get(0).longValue();

        mockMvc.perform(get("/api/v1/animals/" + first).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("레몬 2세 A"))
                .andExpect(jsonPath("$.data.morph").value("탠저린"))
                .andExpect(jsonPath("$.data.species").value("레오파드 게코"))
                .andExpect(jsonPath("$.data.sex").value("UNKNOWN"))
                .andExpect(jsonPath("$.data.hatchDate").value(TODAY.toString()))
                .andExpect(jsonPath("$.data.feedIntervalDays").value(5));
        list(token)
                .andExpect(jsonPath("$.data.breedings", hasSize(1)))
                .andExpect(jsonPath("$.data.breedings[0].maleId").isEmpty())
                .andExpect(jsonPath("$.data.breedings[0].maleName").value("분양처 수컷"))
                .andExpect(jsonPath("$.data.breedings[0].femaleId").value(female))
                .andExpect(jsonPath("$.data.breedings[0].femaleName").value("레몬"))
                .andExpect(jsonPath("$.data.breedings[0].pairedDate").value(PAIRED.toString()))
                .andExpect(jsonPath("$.data.breedings[0].status").value("HATCHED"))
                .andExpect(jsonPath("$.data.breedings[0].memo").value("첫 메이팅"))
                .andExpect(jsonPath("$.data.breedings[0].clutches", hasSize(1)))
                .andExpect(jsonPath("$.data.breedings[0].hatchlingIds[0]").value(first))
                .andExpect(jsonPath("$.data.breedings[0].hatchlingIds[1]").value(hatchlings.get(1).longValue()));
    }

    @Test
    void 산란_기록은_보낼_때마다_통째로_바뀌고_빈_배열이면_모두_지워진다() throws Exception {
        String token = tokenOfNewUser();
        long breedingId = createBreeding(token, """
                { "maleName": "수컷", "femaleName": "암컷", "pairedDate": "%s" }
                """.formatted(PAIRED));
        String twoClutches = """
                { "clutches": [ { "laidDate": "%s", "eggCount": 1 }, { "laidDate": "%s", "eggCount": 2 } ] }
                """.formatted(PAIRED.plusDays(40), PAIRED.plusDays(20));

        perform(token, patch("/api/v1/breeding/" + breedingId), twoClutches).andExpect(status().isOk());
        perform(token, patch("/api/v1/breeding/" + breedingId), twoClutches)
                .andExpect(jsonPath("$.data.clutches", hasSize(2)))
                // 산란일 오래된 순
                .andExpect(jsonPath("$.data.clutches[0].eggCount").value(2))
                .andExpect(jsonPath("$.data.clutches[1].eggCount").value(1))
                .andExpect(jsonPath("$.data.status").value("PAIRED"));
        perform(token, patch("/api/v1/breeding/" + breedingId), "{ \"clutches\": [] }")
                .andExpect(jsonPath("$.data.clutches", hasSize(0)));
        list(token).andExpect(jsonPath("$.data.breedings[0].clutches", hasSize(0)));
    }

    @Test
    void 번식_기록을_지워도_해칭_개체는_남고_부모_개체를_지워도_이름은_남는다() throws Exception {
        String token = tokenOfNewUser();
        long male = createAnimal(token, "망고", 4);
        long female = createAnimal(token, "레몬", 4);
        long breedingId = createBreeding(token, """
                { "maleId": %d, "femaleId": %d, "pairedDate": "%s" }
                """.formatted(male, female, PAIRED));
        perform(token, patch("/api/v1/breeding/" + breedingId),
                "{ \"clutches\": [ { \"laidDate\": \"%s\", \"eggCount\": 2 } ] }".formatted(PAIRED.plusDays(30)))
                .andExpect(status().isOk());
        String body = perform(token, post("/api/v1/breeding/" + breedingId + "/hatchlings"),
                "{ \"hatchDate\": \"%s\", \"hatchlings\": [ { \"name\": \"새끼\" } ] }".formatted(TODAY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long hatchling = ((Number) JsonPath.read(body, "$.data.animalIds[0]")).longValue();

        // 개체 이름을 바꾸면 지금 이름, 지우면 마지막으로 저장한 이름
        perform(token, patch("/api/v1/animals/" + male), "{ \"name\": \"망고 아빠\" }").andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/animals/" + female).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        list(token)
                .andExpect(jsonPath("$.data.breedings[0].maleId").value(male))
                .andExpect(jsonPath("$.data.breedings[0].maleName").value("망고 아빠"))
                .andExpect(jsonPath("$.data.breedings[0].femaleId").isEmpty())
                .andExpect(jsonPath("$.data.breedings[0].femaleName").value("레몬"));

        mockMvc.perform(delete("/api/v1/breeding/" + breedingId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        list(token).andExpect(jsonPath("$.data.breedings", hasSize(0)));
        mockMvc.perform(get("/api/v1/animals/" + hatchling).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
        Integer clutches = jdbcTemplate.queryForObject(
                "select count(*) from clutches where breeding_id = ?", Integer.class, breedingId);
        Long breedingOfHatchling = jdbcTemplate.queryForObject(
                "select breeding_id from animals where id = ?", Long.class, hatchling);
        assertThat(clutches).isZero();
        assertThat(breedingOfHatchling).isNull();
    }

    @Test
    void 부모가_모두_외부_개체면_해칭_개체를_등록할_수_없다() throws Exception {
        String token = tokenOfNewUser();
        long breedingId = createBreeding(token, """
                { "maleName": "수컷", "femaleName": "암컷", "pairedDate": "%s" }
                """.formatted(PAIRED));

        perform(token, post("/api/v1/breeding/" + breedingId + "/hatchlings"),
                "{ \"hatchDate\": \"%s\", \"hatchlings\": [ { \"name\": \"새끼\" } ] }".formatted(TODAY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        list(token).andExpect(jsonPath("$.data.breedings[0].status").value("PAIRED"));
    }

    @Test
    void 남의_번식_기록은_404이고_남의_개체를_부모로_고를_수_없다() throws Exception {
        String owner = tokenOfNewUser();
        String other = tokenOfNewUser();
        long ownerAnimal = createAnimal(owner, "레몬", 4);
        long breedingId = createBreeding(owner, """
                { "maleName": "수컷", "femaleId": %d, "pairedDate": "%s" }
                """.formatted(ownerAnimal, PAIRED));

        list(other).andExpect(jsonPath("$.data.breedings", hasSize(0)));
        perform(other, patch("/api/v1/breeding/" + breedingId), "{ \"status\": \"FAILED\" }")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("BREEDING_NOT_FOUND"));
        perform(other, post("/api/v1/breeding/" + breedingId + "/hatchlings"),
                "{ \"hatchDate\": \"%s\", \"hatchlings\": [ { \"name\": \"새끼\" } ] }".formatted(TODAY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("BREEDING_NOT_FOUND"));
        mockMvc.perform(delete("/api/v1/breeding/" + breedingId).header(HttpHeaders.AUTHORIZATION, other))
                .andExpect(status().isNotFound());
        perform(other, post("/api/v1/breeding"), """
                { "maleName": "수컷", "femaleId": %d, "pairedDate": "%s" }
                """.formatted(ownerAnimal, PAIRED))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
        list(owner).andExpect(jsonPath("$.data.breedings[0].status").value("PAIRED"));
    }

    @Test
    void 번식_기록이_있는_사용자도_탈퇴할_수_있다() throws Exception {
        String token = tokenOfNewUser();
        long female = createAnimal(token, "레몬", 4);
        long breedingId = createBreeding(token, """
                { "maleName": "수컷", "femaleId": %d, "pairedDate": "%s" }
                """.formatted(female, PAIRED));
        perform(token, post("/api/v1/breeding/" + breedingId + "/hatchlings"),
                "{ \"hatchDate\": \"%s\", \"hatchlings\": [ { \"name\": \"새끼\" } ] }".formatted(TODAY))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        Integer left = jdbcTemplate.queryForObject(
                "select count(*) from breedings where id = ?", Integer.class, breedingId);
        assertThat(left).isZero();
    }

    private ResultActions list(String token) throws Exception {
        return mockMvc.perform(get("/api/v1/breeding").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    private ResultActions perform(String token, MockHttpServletRequestBuilder request, String body)
            throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private long createBreeding(String token, String body) throws Exception {
        String response = perform(token, post("/api/v1/breeding"), body)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }

    private long createAnimal(String token, String name, int feedIntervalDays) throws Exception {
        String body = perform(token, post("/api/v1/animals"),
                "{ \"name\": \"%s\", \"speciesId\": 1, \"feedIntervalDays\": %d }".formatted(name, feedIntervalDays))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private String tokenOfNewUser() {
        User user = userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("사용자")
                .build());
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
