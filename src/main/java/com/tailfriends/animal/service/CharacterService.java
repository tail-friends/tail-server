package com.tailfriends.animal.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.AnimalCharacter;
import com.tailfriends.animal.domain.CharacterLook;
import com.tailfriends.animal.dto.CharacterResponse;
import com.tailfriends.animal.dto.CharacterSaveRequest;
import com.tailfriends.animal.repository.AnimalCharacterRepository;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterService {

    private final CurrentUser currentUser;
    private final AnimalRepository animalRepository;
    private final AnimalCharacterRepository characterRepository;

    /**
     * 캐릭터가 없으면 만들고, 있으면 값 전체를 바꾼다.
     */
    @Transactional
    public CharacterResponse save(Long animalId, CharacterSaveRequest request) {
        Long userId = currentUser.id();
        Animal animal = animalRepository.findByIdAndUserId(animalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        if (request.photoId() != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "아직 사진을 연결할 수 없어요. photoId 없이 보내 주세요");
        }

        CharacterLook look = request.toLook();
        AnimalCharacter character = characterRepository.findById(animalId)
                .map(existing -> {
                    existing.change(look);
                    return existing;
                })
                .orElseGet(() -> characterRepository.save(AnimalCharacter.of(animal, look)));
        return CharacterResponse.from(character);
    }
}
