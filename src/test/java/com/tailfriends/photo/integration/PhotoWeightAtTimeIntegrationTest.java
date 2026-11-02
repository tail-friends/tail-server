package com.tailfriends.photo.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 사진을 올릴 때 그날(KST)까지의 가장 최근 체중을 저장하는지 성장 앨범으로 확인한다.
 */
class PhotoWeightAtTimeIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.now(KstDate.ZONE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 올릴_때_오늘까지의_가장_최근_체중을_저장하고_나중에_적은_체중은_반영하지_않는다() throws Exception {
        String token = tokenOfNewUser();
        long animalId = createAnimal(token);
        writeWeight(token, animalId, TODAY.minusDays(3), "50.0");
        writeWeight(token, animalId, TODAY.minusDays(1), "52.3");
        // 날짜가 더 이른 기록을 나중에 적어도 기록일이 최근인 값을 쓴다
        writeWeight(token, animalId, TODAY.minusDays(2), "51.0");

        upload(token, animalId);
        writeWeight(token, animalId, TODAY, "53.0");

        mockMvc.perform(get("/api/v1/photos").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", String.valueOf(animalId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.photos[0].weightAtTime").value(52.3))
                .andExpect(jsonPath("$.data.photos[0].takenAt").value(TODAY.toString()));
    }

    @Test
    void 체중_기록이_없으면_체중이_비어_있다() throws Exception {
        String token = tokenOfNewUser();
        long animalId = createAnimal(token);

        upload(token, animalId);

        mockMvc.perform(get("/api/v1/photos").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", String.valueOf(animalId)))
                .andExpect(jsonPath("$.data.photos[0].weightAtTime").isEmpty());
    }

    private void upload(String token, long animalId) throws Exception {
        mockMvc.perform(multipart("/api/v1/photos").file(png())
                        .param("animalId", String.valueOf(animalId))
                        .param("purpose", "ALBUM")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isCreated());
    }

    private void writeWeight(String token, long animalId, LocalDate date, String weight) throws Exception {
        mockMvc.perform(post("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"animalId\": %d, \"type\": \"WEIGHT\", \"date\": \"%s\", \"value\": %s }"
                                .formatted(animalId, date, weight)))
                .andExpect(status().isCreated());
    }

    private long createAnimal(String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": 1, \"feedIntervalDays\": 4 }"))
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

    private static MockMultipartFile png() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("file", "mango.png", "image/png", output.toByteArray());
    }
}
