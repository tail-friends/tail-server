package com.tailfriends.photo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

class PhotoFileRemovalIntegrationTest extends IntegrationTestSupport {

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
    void 개체를_삭제하면_사진_행과_파일이_함께_지워진다() throws Exception {
        String token = tokenOfNewUser();
        long animalId = createAnimal(token);
        long otherAnimalId = createAnimal(token);
        Photo photo = upload(token, animalId);
        Photo otherPhoto = upload(token, otherAnimalId);
        assertThat(fileOf(photo)).exists();

        mockMvc.perform(delete("/api/v1/animals/" + animalId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        assertThat(photoRepository.findById(photo.getId())).isEmpty();
        assertThat(fileOf(photo)).doesNotExist();
        assertThat(fileOf(otherPhoto)).exists();
    }

    @Test
    void 탈퇴하면_사진_행과_파일이_함께_지워진다() throws Exception {
        String token = tokenOfNewUser();
        Photo photo = upload(token, createAnimal(token));
        assertThat(fileOf(photo)).exists();

        mockMvc.perform(delete("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        assertThat(photoRepository.findById(photo.getId())).isEmpty();
        assertThat(fileOf(photo)).doesNotExist();
    }

    @Test
    void 개체를_삭제하면_AI_분석용_사본_파일도_지워진다() throws Exception {
        String token = tokenOfNewUser();
        long animalId = createAnimal(token);
        Photo photo = upload(token, animalId, "CHARACTER");
        Path analysisFile = Path.of(storageProperties.localPath()).resolve(photo.getAnalysisKey());
        assertThat(analysisFile).exists();

        mockMvc.perform(delete("/api/v1/animals/" + animalId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        assertThat(analysisFile).doesNotExist();
        assertThat(fileOf(photo)).doesNotExist();
    }

    private Path fileOf(Photo photo) {
        return Path.of(storageProperties.localPath()).resolve(photo.getThumbnailKey());
    }

    private Photo upload(String token, long animalId) throws Exception {
        return upload(token, animalId, "ALBUM");
    }

    private Photo upload(String token, long animalId, String purpose) throws Exception {
        String response = mockMvc.perform(multipart("/api/v1/photos").file(png())
                        .param("animalId", String.valueOf(animalId))
                        .param("purpose", purpose)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long photoId = ((Number) JsonPath.read(response, "$.data.photoId")).longValue();
        return photoRepository.findById(photoId).orElseThrow();
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
