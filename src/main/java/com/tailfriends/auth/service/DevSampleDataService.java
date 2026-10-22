package com.tailfriends.auth.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.AnimalCharacter;
import com.tailfriends.animal.repository.AnimalCharacterRepository;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.auth.dto.SampleDataResponse;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.log.domain.Log;
import com.tailfriends.log.repository.LogRepository;
import com.tailfriends.species.domain.Species;
import com.tailfriends.species.repository.SpeciesRepository;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로컬 샘플 데이터. 테스트 로그인과 같이 dev.login.enabled=true일 때만 빈이 생긴다. (운영 프로필은 항상 false)
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "dev.login.enabled", havingValue = "true")
public class DevSampleDataService {

    static final String MEMO = "테스트용 샘플 개체";

    private final UserRepository userRepository;
    private final SpeciesRepository speciesRepository;
    private final AnimalRepository animalRepository;
    private final AnimalCharacterRepository characterRepository;
    private final LogRepository logRepository;
    private final Clock clock;

    /**
     * 부를 때마다 개체 3마리와 캐릭터, 최근 60일 기록을 새로 만든다. 종 목록에서 이름이 바뀐 종은 종 이름만으로 등록한다.
     */
    @Transactional
    public SampleDataResponse create(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        LocalDate today = KstDate.today(clock);

        List<Long> animalIds = new ArrayList<>();
        int logCount = 0;
        for (SampleAnimal sample : SampleAnimal.values()) {
            Animal animal = animalRepository.save(toAnimal(sample, user, today));
            characterRepository.save(AnimalCharacter.of(animal, sample.getLook()));
            List<Log> logs = sample.logs(animal, user, today);
            logRepository.saveAll(logs);
            animalIds.add(animal.getId());
            logCount += logs.size();
        }
        return new SampleDataResponse(animalIds, logCount);
    }

    private Animal toAnimal(SampleAnimal sample, User user, LocalDate today) {
        Species species = speciesRepository.findByName(sample.getSpeciesName()).orElse(null);
        return Animal.builder()
                .user(user)
                .species(species)
                .speciesName(sample.getSpeciesName())
                .name(sample.getName())
                .morph(sample.getMorph())
                .sex(sample.getSex())
                .hatchDate(sample.getHatchDate(today))
                .feedIntervalDays(sample.getFeedIntervalDays())
                .memo(MEMO)
                .build();
    }
}
