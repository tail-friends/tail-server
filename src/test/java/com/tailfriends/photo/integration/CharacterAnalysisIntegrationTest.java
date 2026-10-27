package com.tailfriends.photo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.photo.ai.CharacterAnalysis;
import com.tailfriends.photo.ai.CharacterAnalyzer;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.repository.AiUsageRepository;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
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
class CharacterAnalysisIntegrationTest extends IntegrationTestSupport {

    private static final int CONCURRENT_REQUESTS = 8;

    private static final CharacterAnalysis GECKO = new CharacterAnalysis(true, "gecko", "spots",
            new CharacterAnalysis.Colors("#E0A33A", "#3A2A1E", "#F2E6C8", "#2B2B2B"),
            new CharacterAnalysis.Extra(new BigDecimal("0.60"), new BigDecimal("0.80"), new BigDecimal("0.40")),
            "탠저린 레오파드 게코");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private AiUsageRepository aiUsageRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private CharacterAnalyzer characterAnalyzer;

    @Test
    void 원본을_보관하지_않은_캐릭터_사진도_분석용_사본으로_분석하고_하루_5번까지만_허용한다() throws Exception {
        User user = saveUser();
        String token = token(user);
        long photoId = upload(token, createAnimal(token));
        Photo photo = photoRepository.findById(photoId).orElseThrow();
        assertThat(photo.getOriginalKey()).isNull();
        assertThat(photo.getAnalysisKey()).endsWith("_analysis.jpg");

        ArgumentCaptor<byte[]> sent = ArgumentCaptor.forClass(byte[].class);
        given(characterAnalyzer.analyze(sent.capture())).willReturn(GECKO);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/photos/{id}/character-analysis", photoId)
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.shape").value("gecko"))
                    .andExpect(jsonPath("$.data.colors.body").value("#E0A33A"))
                    .andExpect(jsonPath("$.data.guess").value("탠저린 레오파드 게코"));
        }
        mockMvc.perform(post("/api/v1/photos/{id}/character-analysis", photoId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("AI_LIMIT_EXCEEDED"));

        BufferedImage analyzed = ImageIO.read(new ByteArrayInputStream(sent.getValue()));
        assertThat(analyzed.getWidth()).isEqualTo(1024);
        assertThat(aiUsageRepository.countByUserIdAndSuccessTrueAndCreatedAtGreaterThanEqual(
                user.getId(), Instant.now().minusSeconds(3600))).isEqualTo(5);
    }

    @Test
    void 동시에_요청해도_하루_5번만_AI를_부른다() throws Exception {
        String token = token(saveUser());
        long photoId = upload(token, createAnimal(token));
        // AI가 답하는 동안 다른 요청이 들어오도록 조금 기다렸다 답한다
        given(characterAnalyzer.analyze(any())).willAnswer(invocation -> {
            Thread.sleep(200);
            return GECKO;
        });

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        List<Integer> statuses = new ArrayList<>();
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                Callable<Integer> task = () -> {
                    start.await();
                    return mockMvc.perform(post("/api/v1/photos/{id}/character-analysis", photoId)
                                    .header(HttpHeaders.AUTHORIZATION, token))
                            .andReturn().getResponse().getStatus();
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

        assertThat(statuses).filteredOn(status -> status == 200).hasSize(5);
        assertThat(statuses).filteredOn(status -> status == 429).hasSize(CONCURRENT_REQUESTS - 5);
        verify(characterAnalyzer, times(5)).analyze(any());
    }

    @Test
    void AI가_실패한_시도는_5번에_세지_않지만_실패를_포함해_하루_10번까지만_시도한다() throws Exception {
        String token = token(saveUser());
        long photoId = upload(token, createAnimal(token));

        given(characterAnalyzer.analyze(any())).willThrow(new BusinessException(ErrorCode.AI_UNAVAILABLE));
        for (int i = 0; i < 5; i++) {
            analyze(token, photoId).andExpect(status().isServiceUnavailable());
        }
        // 실패 5번 뒤에도 성공 횟수는 남아 있다
        willReturn(GECKO).given(characterAnalyzer).analyze(any());
        analyze(token, photoId).andExpect(status().isOk());

        willThrow(new BusinessException(ErrorCode.AI_UNAVAILABLE)).given(characterAnalyzer).analyze(any());
        for (int i = 0; i < 4; i++) {
            analyze(token, photoId).andExpect(status().isServiceUnavailable());
        }
        // 시도 10번을 다 써서 성공이 1번뿐이어도 더 부르지 않는다
        analyze(token, photoId)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("AI_LIMIT_EXCEEDED"));
        verify(characterAnalyzer, times(10)).analyze(any());
    }

    @Test
    void 파충류가_없으면_422() throws Exception {
        String token = token(saveUser());
        long photoId = upload(token, createAnimal(token));
        given(characterAnalyzer.analyze(any())).willReturn(CharacterAnalysis.notReptile());

        mockMvc.perform(post("/api/v1/photos/{id}/character-analysis", photoId)
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("NO_REPTILE_IN_PHOTO"));
    }

    @Test
    void 남의_사진이면_404() throws Exception {
        String owner = token(saveUser());
        long photoId = upload(owner, createAnimal(owner));

        mockMvc.perform(post("/api/v1/photos/{id}/character-analysis", photoId)
                        .header(HttpHeaders.AUTHORIZATION, token(saveUser())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PHOTO_NOT_FOUND"));
    }

    private ResultActions analyze(String token, long photoId) throws Exception {
        return mockMvc.perform(post("/api/v1/photos/{id}/character-analysis", photoId)
                .header(HttpHeaders.AUTHORIZATION, token));
    }

    private long upload(String token, long animalId) throws Exception {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2000, 1000, BufferedImage.TYPE_INT_RGB), "png", png);
        String response = mockMvc.perform(multipart("/api/v1/photos")
                        .file(new MockMultipartFile("file", "mango.png", "image/png", png.toByteArray()))
                        .param("animalId", String.valueOf(animalId))
                        .param("purpose", "CHARACTER")
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
