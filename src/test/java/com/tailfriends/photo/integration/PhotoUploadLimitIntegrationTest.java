package com.tailfriends.photo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@TestPropertySource(properties = "storage.daily-upload-limit=2")
class PhotoUploadLimitIntegrationTest extends IntegrationTestSupport {

    private static final int CONCURRENT_UPLOADS = 6;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 하루_제한까지_올리고_넘으면_429_PHOTO_LIMIT_EXCEEDED이고_다른_사용자는_따로_센다() throws Exception {
        String token = tokenOfNewUser();
        long animalId = createAnimal(token);

        upload(token, animalId).andExpect(status().isCreated());
        upload(token, animalId).andExpect(status().isCreated());
        upload(token, animalId)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("PHOTO_LIMIT_EXCEEDED"));

        String other = tokenOfNewUser();
        upload(other, createAnimal(other)).andExpect(status().isCreated());
    }

    @Test
    void 어제_올린_사진은_오늘_제한에_세지_않는다() throws Exception {
        String token = tokenOfNewUser();
        long animalId = createAnimal(token);
        upload(token, animalId).andExpect(status().isCreated());
        upload(token, animalId).andExpect(status().isCreated());
        jdbcTemplate.update("update photos set created_at = created_at - interval '1 day' where animal_id = ?",
                animalId);

        upload(token, animalId).andExpect(status().isCreated());
    }

    @Test
    void 동시에_올려도_하루_제한만큼만_저장한다() throws Exception {
        String token = tokenOfNewUser();
        long animalId = createAnimal(token);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_UPLOADS);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_UPLOADS; i++) {
                Callable<Integer> task = () -> {
                    start.await();
                    return upload(token, animalId).andReturn().getResponse().getStatus();
                };
                results.add(executor.submit(task));
            }
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get());
            }
            assertThat(statuses).filteredOn(status -> status == 201).hasSize(2);
            assertThat(statuses).filteredOn(status -> status == 429).hasSize(CONCURRENT_UPLOADS - 2);
        } finally {
            executor.shutdownNow();
        }
        assertThat(jdbcTemplate.queryForObject("select count(*) from photos where animal_id = ?", Integer.class,
                animalId)).isEqualTo(2);
    }

    private ResultActions upload(String token, long animalId) throws Exception {
        return mockMvc.perform(multipart("/api/v1/photos").file(png())
                .param("animalId", String.valueOf(animalId))
                .param("purpose", "ALBUM")
                .header(HttpHeaders.AUTHORIZATION, token));
    }

    private String tokenOfNewUser() {
        User user = userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("망고집사")
                .build());
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }

    private long createAnimal(String token) throws Exception {
        String response = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": 1, \"feedIntervalDays\": 4 }"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }

    private static MockMultipartFile png() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("file", "mango.png", "image/png", output.toByteArray());
    }
}
