package com.tailfriends.enclosure.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class PhotoThemeIntegrationTest extends IntegrationTestSupport {

    private static final int CONCURRENT_REQUESTS = 12;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 저장한_사진_테마는_기본_테마_뒤에_나오고_상세에만_objects가_있고_setAsHomeFor_개체의_평소_사육장이_된다()
            throws Exception {
        String token = tokenOf(newUser());
        long animalId = createAnimal(token);
        long photoId = upload(token, animalId, "ENCLOSURE");

        long id = idOf(create(token, """
                { "photoId": %d, "floorColor": "#c9a26b", "wallColor": "#5B4A3A",
                  "objects": [ { "type": "ROCK", "x": 0.7, "y": 0.8 }, { "type": "LAMP", "x": 0.12345, "y": 0 } ],
                  "setAsHomeFor": %d }
                """.formatted(photoId, animalId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        list(token)
                .andExpect(jsonPath("$.data.enclosures[5].id").value(id))
                .andExpect(jsonPath("$.data.enclosures[5].background.type").value("PHOTO"))
                .andExpect(jsonPath("$.data.enclosures[5].background.photoId").value(photoId))
                .andExpect(jsonPath("$.data.enclosures[5].background.floorColor").value("#C9A26B"))
                .andExpect(jsonPath("$.data.enclosures[5].background.objects").doesNotExist())
                .andExpect(jsonPath("$.data.enclosures[0].background.photoId").doesNotExist());
        mockMvc.perform(get("/api/v1/enclosures/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.background.objects[0].type").value("ROCK"))
                .andExpect(jsonPath("$.data.background.objects[1].type").value("LAMP"))
                .andExpect(jsonPath("$.data.background.objects[1].x").value(0.123));
        assertThat(home(animalId)).containsEntry("home", null).containsEntry("enclosure_id", id);
    }

    @Test
    void 동시에_저장해도_사진_테마는_10개까지만_생긴다() throws Exception {
        User keeper = newUser();
        String token = tokenOf(keeper);
        long photoId = upload(token, createAnimal(token), "ENCLOSURE");
        String body = "{ \"photoId\": %d, \"floorColor\": \"#C9A26B\", \"wallColor\": \"#5B4A3A\" }".formatted(photoId);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        List<Integer> statuses = new ArrayList<>();
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                Callable<Integer> task = () -> {
                    start.await();
                    return create(token, body).andReturn().getResponse().getStatus();
                };
                results.add(executor.submit(task));
            }
            start.countDown();
            for (Future<Integer> result : results) {
                statuses.add(result.get());
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(statuses).filteredOn(status -> status == 201).hasSize(10);
        assertThat(statuses).filteredOn(status -> status == 409).hasSize(CONCURRENT_REQUESTS - 10);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from enclosures where user_id = ? and background_type = 'PHOTO'",
                Integer.class, keeper.getId())).isEqualTo(10);
    }

    @Test
    void 사진_테마를_지우면_배경_장식도_지워지고_쓰던_개체는_종의_기본_테마로_돌아간다() throws Exception {
        String token = tokenOf(newUser());
        long animalId = createAnimal(token);
        long photoId = upload(token, animalId, "ENCLOSURE");
        long id = idOf(create(token, """
                { "photoId": %d, "floorColor": "#C9A26B", "wallColor": "#5B4A3A",
                  "objects": [ { "type": "ROCK", "x": 0.7, "y": 0.8 } ], "setAsHomeFor": %d }
                """.formatted(photoId, animalId)).andReturn().getResponse().getContentAsString());

        mockMvc.perform(delete("/api/v1/enclosures/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        assertThat(home(animalId)).containsEntry("home", null).containsEntry("enclosure_id", null);
        assertThat(jdbcTemplate.queryForObject("select count(*) from enclosure_objects where enclosure_id = ?",
                Integer.class, id)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from photos where id = ?", Integer.class, photoId))
                .isEqualTo(1);
        changeHome(token, animalId, "{ \"enclosureId\": %d }".formatted(id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ENCLOSURE_NOT_FOUND"));
    }

    @Test
    void 평소_사육장을_사진_테마_ID나_기본_테마_ID로_바꾼다() throws Exception {
        String token = tokenOf(newUser());
        long animalId = createAnimal(token);
        long id = idOf(create(token, "{ \"photoId\": %d, \"floorColor\": \"#C9A26B\", \"wallColor\": \"#5B4A3A\" }"
                .formatted(upload(token, animalId, "ENCLOSURE"))).andReturn().getResponse().getContentAsString());
        long pondId = idOf(list(token).andReturn().getResponse().getContentAsString(), "$.data.enclosures[4].id");

        changeHome(token, animalId, "{ \"enclosureId\": %d }".formatted(id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.home").value(nullValue()))
                .andExpect(jsonPath("$.data.enclosureId").value(id));
        assertThat(home(animalId)).containsEntry("enclosure_id", id);

        changeHome(token, animalId, "{ \"enclosureId\": %d }".formatted(pondId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.home").value("pond"))
                .andExpect(jsonPath("$.data.enclosureId").value(nullValue()));
        assertThat(home(animalId)).containsEntry("home", "POND").containsEntry("enclosure_id", null);
    }

    @Test
    void 사진을_지워도_테마는_남고_photoId가_null이다() throws Exception {
        String token = tokenOf(newUser());
        long photoId = upload(token, createAnimal(token), "ENCLOSURE");
        long id = idOf(create(token, "{ \"photoId\": %d, \"floorColor\": \"#C9A26B\", \"wallColor\": \"#5B4A3A\" }"
                .formatted(photoId)).andReturn().getResponse().getContentAsString());

        jdbcTemplate.update("delete from photos where id = ?", photoId);

        mockMvc.perform(get("/api/v1/enclosures/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.background.type").value("PHOTO"))
                .andExpect(jsonPath("$.data.background.photoId").value(nullValue()));
    }

    @Test
    void 기본_테마는_못_지우고_남의_사진이나_사육장은_404이고_사육장_사진이_아니면_400() throws Exception {
        String owner = tokenOf(newUser());
        long animalId = createAnimal(owner);
        long photoId = upload(owner, animalId, "ENCLOSURE");
        long id = idOf(create(owner, "{ \"photoId\": %d, \"floorColor\": \"#C9A26B\", \"wallColor\": \"#5B4A3A\" }"
                .formatted(photoId)).andReturn().getResponse().getContentAsString());
        long desertId = idOf(list(owner).andReturn().getResponse().getContentAsString(), "$.data.enclosures[0].id");
        String other = tokenOf(newUser());

        mockMvc.perform(delete("/api/v1/enclosures/" + desertId).header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUILT_IN_THEME"));
        mockMvc.perform(delete("/api/v1/enclosures/" + id).header(HttpHeaders.AUTHORIZATION, other))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ENCLOSURE_NOT_FOUND"));
        create(other, "{ \"photoId\": %d, \"floorColor\": \"#C9A26B\", \"wallColor\": \"#5B4A3A\" }"
                .formatted(photoId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PHOTO_NOT_FOUND"));
        create(owner, "{ \"photoId\": %d, \"floorColor\": \"#C9A26B\", \"wallColor\": \"#5B4A3A\" }"
                .formatted(upload(owner, animalId, "CHARACTER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    private ResultActions create(String token, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/enclosures").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions list(String token) throws Exception {
        return mockMvc.perform(get("/api/v1/enclosures").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    private ResultActions changeHome(String token, long animalId, String body) throws Exception {
        return mockMvc.perform(patch("/api/v1/animals/" + animalId + "/home")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Map<String, Object> home(long animalId) {
        return jdbcTemplate.queryForMap("select home, enclosure_id from animals where id = ?", animalId);
    }

    private static long idOf(String body) {
        return idOf(body, "$.data.id");
    }

    private static long idOf(String body, String path) {
        return ((Number) JsonPath.read(body, path)).longValue();
    }

    private long upload(String token, long animalId, String purpose) throws Exception {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB), "png", png);
        String response = mockMvc.perform(multipart("/api/v1/photos")
                        .file(new MockMultipartFile("file", "tank.png", "image/png", png.toByteArray()))
                        .param("animalId", String.valueOf(animalId))
                        .param("purpose", purpose)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(response, "$.data.photoId");
    }

    private long createAnimal(String token) throws Exception {
        String response = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": 1, \"feedIntervalDays\": 4 }"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(response);
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
