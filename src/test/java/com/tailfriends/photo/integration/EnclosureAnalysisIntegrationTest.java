package com.tailfriends.photo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.photo.ai.EnclosureAnalysis;
import com.tailfriends.photo.ai.EnclosureAnalyzer;
import com.tailfriends.photo.ai.EnclosureObjectType;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Gemini 호출만 가짜로 바꾸고, 사진 저장·분석용 사본·하루 횟수 제한은 실제 DB와 디스크로 확인한다.
 */
class EnclosureAnalysisIntegrationTest extends IntegrationTestSupport {

    private static final EnclosureAnalysis DESERT = new EnclosureAnalysis(true, "#C9A26B", "#5B4A3A", List.of(
            new EnclosureAnalysis.DetectedObject(EnclosureObjectType.ROCK, new BigDecimal("0.700"),
                    new BigDecimal("0.800"))));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private EnclosureAnalyzer enclosureAnalyzer;

    @Test
    void 원본을_보관하지_않은_사육장_사진도_분석하고_캐릭터_분석과_하루_횟수를_합쳐_센다() throws Exception {
        String token = token(saveUser());
        long photoId = upload(token, createAnimal(token), "ENCLOSURE");
        Photo photo = photoRepository.findById(photoId).orElseThrow();
        assertThat(photo.getOriginalKey()).isNull();
        assertThat(photo.getAnalysisKey()).endsWith("_analysis.jpg");

        ArgumentCaptor<byte[]> sent = ArgumentCaptor.forClass(byte[].class);
        given(enclosureAnalyzer.analyze(sent.capture())).willReturn(DESERT);

        for (int i = 0; i < 5; i++) {
            analyze(token, photoId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.floorColor").value("#C9A26B"))
                    .andExpect(jsonPath("$.data.wallColor").value("#5B4A3A"))
                    .andExpect(jsonPath("$.data.objects[0].type").value("ROCK"))
                    .andExpect(jsonPath("$.data.objects[0].x").value(0.7));
        }
        BufferedImage analyzed = ImageIO.read(new ByteArrayInputStream(sent.getValue()));
        assertThat(analyzed.getWidth()).isEqualTo(1024);

        analyze(token, photoId)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("AI_LIMIT_EXCEEDED"));
        mockMvc.perform(post("/api/v1/photos/{id}/character-analysis", upload(token, createAnimal(token), "CHARACTER"))
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void 사육장이_아니면_422() throws Exception {
        String token = token(saveUser());
        long photoId = upload(token, createAnimal(token), "ENCLOSURE");
        given(enclosureAnalyzer.analyze(any())).willReturn(EnclosureAnalysis.notEnclosure());

        analyze(token, photoId)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("NO_ENCLOSURE_IN_PHOTO"));
    }

    @Test
    void 사육장용으로_올리지_않은_사진이면_400() throws Exception {
        String token = token(saveUser());
        long photoId = upload(token, createAnimal(token), "CHARACTER");

        analyze(token, photoId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 남의_사진이면_404() throws Exception {
        String owner = token(saveUser());
        long photoId = upload(owner, createAnimal(owner), "ENCLOSURE");

        analyze(token(saveUser()), photoId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PHOTO_NOT_FOUND"));
    }

    private ResultActions analyze(String token, long photoId) throws Exception {
        return mockMvc.perform(post("/api/v1/photos/{id}/enclosure-analysis", photoId)
                .header(HttpHeaders.AUTHORIZATION, token));
    }

    private long upload(String token, long animalId, String purpose) throws Exception {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2000, 1000, BufferedImage.TYPE_INT_RGB), "png", png);
        String response = mockMvc.perform(multipart("/api/v1/photos")
                        .file(new MockMultipartFile("file", "tank.png", "image/png", png.toByteArray()))
                        .param("animalId", String.valueOf(animalId))
                        .param("purpose", purpose)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.photoId")).longValue();
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("망고집사")
                .build());
    }

    private String token(User user) {
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
}
