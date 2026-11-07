package com.tailfriends.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.health.domain.HealthCheckKey;
import com.tailfriends.health.repository.HealthCheckRepository;
import com.tailfriends.photo.ai.HealthCheckAnalyzer;
import com.tailfriends.photo.domain.AiKind;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.service.AiUsageLimiter;
import com.tailfriends.photo.storage.PhotoStorage;
import com.tailfriends.species.domain.Shape;
import com.tailfriends.species.domain.Species;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class HealthCheckServiceTest {

    private static final Long USER_ID = 1L;

    private final PhotoRepository photoRepository = mock(PhotoRepository.class);
    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final HealthCheckRepository healthCheckRepository = mock(HealthCheckRepository.class);
    private final HealthAlertService healthAlertService = mock(HealthAlertService.class);
    private final HealthCheckAnalyzer analyzer = mock(HealthCheckAnalyzer.class);
    private final AiUsageLimiter aiUsageLimiter = mock(AiUsageLimiter.class);
    private final PhotoStorage photoStorage = mock(PhotoStorage.class);
    private final HealthCheckService service = new HealthCheckService(CurrentUserStub.of(USER_ID), photoRepository,
            animalRepository, healthCheckRepository, healthAlertService, analyzer, aiUsageLimiter, photoStorage,
            new DirectTransactionTemplate());

    @Test
    void 게코는_모든_항목을_보고_다른_체형이나_목록에_없는_종은_꼬리를_뺀다() {
        assertThat(HealthCheckService.keysFor(species(Shape.GECKO))).isEqualTo(EnumSet.allOf(HealthCheckKey.class));
        assertThat(HealthCheckService.keysFor(species(Shape.SNAKE))).doesNotContain(HealthCheckKey.TAIL).hasSize(4);
        assertThat(HealthCheckService.keysFor(null)).doesNotContain(HealthCheckKey.TAIL);
    }

    @Test
    void AI가_답을_주지_못하면_하루_횟수를_풀고_저장하지_않는다() {
        Photo photo = healthPhoto();
        given(photoRepository.findByIdAndUserId(301L, USER_ID)).willReturn(Optional.of(photo));
        given(aiUsageLimiter.reserve(USER_ID, AiKind.HEALTH_CHECK)).willReturn(77L);
        given(photoStorage.read("p_analysis.jpg")).willReturn(new byte[]{1});
        given(analyzer.analyze(any(), any(), any())).willThrow(new BusinessException(ErrorCode.AI_UNAVAILABLE));

        assertThatThrownBy(() -> service.check(301L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_UNAVAILABLE);
        verify(aiUsageLimiter).release(77L);
        verify(healthCheckRepository, never()).save(any());
    }

    @Test
    void 건강_체크용_사진이_아니면_횟수를_쓰지_않고_INVALID_INPUT() {
        Photo album = Photo.builder().purpose(PhotoPurpose.ALBUM).analysisKey("p_analysis.jpg").build();
        given(photoRepository.findByIdAndUserId(301L, USER_ID)).willReturn(Optional.of(album));

        assertThatThrownBy(() -> service.check(301L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verify(aiUsageLimiter, never()).reserve(any(), any());
    }

    private static Photo healthPhoto() {
        Species gecko = species(Shape.GECKO);
        Animal animal = mock(Animal.class);
        given(animal.getId()).willReturn(12L);
        given(animal.getSpecies()).willReturn(gecko);
        Photo photo = Photo.builder().animal(animal).purpose(PhotoPurpose.HEALTH).analysisKey("p_analysis.jpg").build();
        ReflectionTestUtils.setField(photo, "id", 301L);
        return photo;
    }

    private static Species species(Shape shape) {
        Species species = mock(Species.class);
        given(species.getShape()).willReturn(shape);
        return species;
    }

    /**
     * 트랜잭션 없이 콜백을 바로 실행한다.
     */
    private static final class DirectTransactionTemplate extends TransactionTemplate {

        @Override
        public <T> T execute(TransactionCallback<T> action) {
            return action.doInTransaction(new SimpleTransactionStatus());
        }
    }
}
