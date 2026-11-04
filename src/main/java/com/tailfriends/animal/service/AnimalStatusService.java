package com.tailfriends.animal.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.FeedSchedule;
import com.tailfriends.animal.domain.Mood;
import com.tailfriends.animal.domain.ShedSummary;
import com.tailfriends.animal.dto.CharacterSummaryResponse;
import com.tailfriends.animal.dto.WeightSummaryResponse;
import com.tailfriends.animal.repository.AnimalCharacterRepository;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.log.repository.LogRepository;
import com.tailfriends.species.domain.Home;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개체 상세의 탈피·체중·캐릭터 요약과 펫 기분. 오늘은 한국 날짜 기준이다.
 */
@Service
@RequiredArgsConstructor
public class AnimalStatusService {

    private final LogRepository logRepository;
    private final AnimalCharacterRepository animalCharacterRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AnimalStatus statusOf(Animal animal, FeedSchedule feed, boolean hasOpenAlert) {
        ShedSummary shed = ShedSummary.from(
                logRepository.findLatestSheds(animal.getId(), PageRequest.of(0, ShedSummary.RECENT_LOGS)));
        WeightSummaryResponse weight = WeightSummaryResponse.from(
                logRepository.findLatestWeights(animal.getId(), PageRequest.of(0, 2)));
        CharacterSummaryResponse character = animalCharacterRepository.findById(animal.getId())
                .map(saved -> CharacterSummaryResponse.of(saved, homeOf(animal), animal.getEnclosureId()))
                .orElse(null);
        Mood mood = Mood.of(feed, hasOpenAlert, shed.lastDate(), KstDate.today(clock));
        return new AnimalStatus(shed, weight, character, mood);
    }

    /**
     * 평소 사육장 테마. 사진 테마면 null(enclosureId로 준다).
     */
    private static Home homeOf(Animal animal) {
        if (animal.getEnclosureId() != null) {
            return null;
        }
        if (animal.getHome() != null) {
            return animal.getHome();
        }
        return animal.getSpecies() == null ? null : animal.getSpecies().getDefaultHome();
    }

    public record AnimalStatus(
            ShedSummary shed,
            WeightSummaryResponse weight,
            CharacterSummaryResponse character,
            Mood mood
    ) {
    }
}
