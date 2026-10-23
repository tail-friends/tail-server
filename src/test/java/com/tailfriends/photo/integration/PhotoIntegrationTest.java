package com.tailfriends.photo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.StorageProperties;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

class PhotoIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private StorageProperties storageProperties;

    @Test
    void 원본_보관에_동의했으면_썸네일과_원본이_저장되고_썸네일_주소로_로그인_없이_받을_수_있다() throws Exception {
        String token = tokenOfNewUser();
        agreeKeepOriginal(token, true);
        long animalId = createAnimal(token);

        String response = mockMvc.perform(multipart("/api/v1/photos").file(png(2000, 1000))
                        .param("animalId", String.valueOf(animalId))
                        .param("purpose", "CHARACTER")
                        .param("keepOriginal", "true")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.originalKept").value(true))
                .andReturn().getResponse().getContentAsString();

        long photoId = ((Number) JsonPath.read(response, "$.data.photoId")).longValue();
        String thumbnailUrl = JsonPath.read(response, "$.data.thumbnailUrl");
        Photo photo = photoRepository.findById(photoId).orElseThrow();
        assertThat(thumbnailUrl).isEqualTo("http://localhost/files/" + photo.getThumbnailKey());
        assertThat(photo.getOriginalKey()).endsWith("_original.jpg");

        byte[] thumbnail = mockMvc.perform(get("/files/" + photo.getThumbnailKey()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andReturn().getResponse().getContentAsByteArray();
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(thumbnail));
        assertThat(decoded.getWidth()).isEqualTo(128);
        assertThat(decoded.getHeight()).isEqualTo(64);

        byte[] original = mockMvc.perform(get("/files/" + photo.getOriginalKey()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(ImageIO.read(new ByteArrayInputStream(original)).getWidth()).isEqualTo(1600);
    }

    @Test
    void 원본_보관에_동의하지_않았으면_keepOriginal을_보내도_썸네일만_저장한다() throws Exception {
        String token = tokenOfNewUser();
        agreeKeepOriginal(token, false);
        long animalId = createAnimal(token);

        String response = mockMvc.perform(multipart("/api/v1/photos").file(png(20, 10))
                        .param("animalId", String.valueOf(animalId))
                        .param("purpose", "ALBUM")
                        .param("keepOriginal", "true")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.originalKept").value(false))
                .andReturn().getResponse().getContentAsString();

        long photoId = ((Number) JsonPath.read(response, "$.data.photoId")).longValue();
        assertThat(photoRepository.findById(photoId).orElseThrow().getOriginalKey()).isNull();
    }

    @Test
    void 보관한_원본을_모두_지우면_썸네일만_남고_원본_보관_동의가_철회된다() throws Exception {
        String token = tokenOfNewUser();
        agreeKeepOriginal(token, true);
        long animalId = createAnimal(token);
        String response = mockMvc.perform(multipart("/api/v1/photos").file(png(20, 10))
                        .param("animalId", String.valueOf(animalId))
                        .param("purpose", "ALBUM")
                        .param("keepOriginal", "true")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.originalKept").value(true))
                .andReturn().getResponse().getContentAsString();
        long photoId = ((Number) JsonPath.read(response, "$.data.photoId")).longValue();
        Photo before = photoRepository.findById(photoId).orElseThrow();
        Path original = storageRoot().resolve(before.getOriginalKey());
        assertThat(original).exists();

        mockMvc.perform(delete("/api/v1/users/me/originals").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletedCount").value(1));

        Photo after = photoRepository.findById(photoId).orElseThrow();
        assertThat(after.getOriginalKey()).isNull();
        assertThat(original).doesNotExist();
        assertThat(storageRoot().resolve(after.getThumbnailKey())).exists();
        mockMvc.perform(get("/api/v1/auth/consents").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.keepOriginal").value(false));
    }

    @Test
    void 사진이_아니면_400_INVALID_FILE이고_저장하지_않는다() throws Exception {
        String token = tokenOfNewUser();
        long animalId = createAnimal(token);
        long before = photoRepository.count();

        mockMvc.perform(multipart("/api/v1/photos")
                        .file(new MockMultipartFile("file", "a.jpg", "image/jpeg",
                                "not an image".getBytes(StandardCharsets.UTF_8)))
                        .param("animalId", String.valueOf(animalId))
                        .param("purpose", "ALBUM")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FILE"));
        assertThat(photoRepository.count()).isEqualTo(before);
    }

    @Test
    void 남의_개체에_올리면_404() throws Exception {
        long othersAnimalId = createAnimal(tokenOfNewUser());

        mockMvc.perform(multipart("/api/v1/photos").file(png(10, 10))
                        .param("animalId", String.valueOf(othersAnimalId))
                        .param("purpose", "ALBUM")
                        .header(HttpHeaders.AUTHORIZATION, tokenOfNewUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
    }

    private Path storageRoot() {
        return Path.of(storageProperties.localPath());
    }

    private void agreeKeepOriginal(String token, boolean keepOriginal) throws Exception {
        mockMvc.perform(put("/api/v1/auth/consents").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "privacy": true, "photoAnalysis": true, "keepOriginal": %s, "notification": false }
                                """.formatted(keepOriginal)))
                .andExpect(status().isOk());
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

    private static MockMultipartFile png(int width, int height) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("file", "mango.png", "image/png", output.toByteArray());
    }
}
