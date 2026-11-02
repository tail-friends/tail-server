package com.tailfriends.photo.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class AlbumIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 사육장_사진을_빼고_최신순으로_보여주고_커서로_다음_페이지를_준다() throws Exception {
        String token = tokenOfNewUser();
        long animalId = createAnimal(token);
        long character = upload(token, animalId, "CHARACTER");
        upload(token, animalId, "ENCLOSURE");
        long health = upload(token, animalId, "HEALTH");
        long album = upload(token, animalId, "ALBUM");

        String body = album(token, animalId, null, 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.photos", hasSize(2)))
                .andExpect(jsonPath("$.data.photos[0].photoId").value(album))
                .andExpect(jsonPath("$.data.photos[0].purpose").value("ALBUM"))
                .andExpect(jsonPath("$.data.photos[0].thumbnailUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.photos[0].originalKept").value(false))
                .andExpect(jsonPath("$.data.photos[1].photoId").value(health))
                .andExpect(jsonPath("$.data.nextCursor").value(health))
                .andReturn().getResponse().getContentAsString();
        long cursor = ((Number) JsonPath.read(body, "$.data.nextCursor")).longValue();

        album(token, animalId, cursor, 2)
                .andExpect(jsonPath("$.data.photos", hasSize(1)))
                .andExpect(jsonPath("$.data.photos[0].photoId").value(character))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty());
    }

    @Test
    void 사진이_없으면_빈_목록이고_남의_개체는_404() throws Exception {
        String owner = tokenOfNewUser();
        String other = tokenOfNewUser();
        long animalId = createAnimal(owner);

        album(owner, animalId, null, 20)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.photos", hasSize(0)))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty());
        album(other, animalId, null, 20)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
    }

    private ResultActions album(String token, long animalId, Long cursor, int size) throws Exception {
        var request = get("/api/v1/photos").header(HttpHeaders.AUTHORIZATION, token)
                .param("animalId", String.valueOf(animalId))
                .param("size", String.valueOf(size));
        if (cursor != null) {
            request.param("cursor", String.valueOf(cursor));
        }
        return mockMvc.perform(request);
    }

    private long upload(String token, long animalId, String purpose) throws Exception {
        String body = mockMvc.perform(multipart("/api/v1/photos").file(png())
                        .param("animalId", String.valueOf(animalId))
                        .param("purpose", purpose)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.photoId")).longValue();
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
