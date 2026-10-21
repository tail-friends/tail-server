package com.tailfriends.animal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.AnimalCharacter;
import com.tailfriends.animal.domain.PatternType;
import com.tailfriends.animal.domain.RenderStyle;
import com.tailfriends.animal.dto.CharacterResponse;
import com.tailfriends.animal.dto.CharacterSaveRequest;
import com.tailfriends.animal.dto.CharacterSaveRequest.Colors;
import com.tailfriends.animal.repository.AnimalCharacterRepository;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.species.domain.Shape;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CharacterServiceTest {

    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final AnimalCharacterRepository characterRepository = mock(AnimalCharacterRepository.class);
    private final CharacterService characterService = new CharacterService(animalRepository, characterRepository);

    @Test
    void 캐릭터가_없으면_새로_만들고_색은_대문자로_저장한다() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(mock(Animal.class)));
        given(characterRepository.findById(12L)).willReturn(Optional.empty());
        given(characterRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        CharacterResponse response = characterService.save(1L, 12L, request(null, "#e0a33a"));

        assertThat(response.renderStyle()).isEqualTo(RenderStyle.ILLUSTRATION_2D);
        assertThat(response.shape()).isEqualTo("gecko");
        assertThat(response.patternType()).isEqualTo("spots");
        assertThat(response.colors().body()).isEqualTo("#E0A33A");
    }

    @Test
    void 캐릭터가_있으면_새로_만들지_않고_값을_바꾼다() {
        AnimalCharacter existing = AnimalCharacter.of(mock(Animal.class), request(null, "#000000").toLook());
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(mock(Animal.class)));
        given(characterRepository.findById(12L)).willReturn(Optional.of(existing));

        characterService.save(1L, 12L, request(null, "#FFFFFF"));

        verify(characterRepository, never()).save(any());
        assertThat(existing.getColorBody()).isEqualTo("#FFFFFF");
    }

    @Test
    void 아직_사진을_연결할_수_없어서_photoId가_있으면_INVALID_INPUT() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(mock(Animal.class)));

        assertThatThrownBy(() -> characterService.save(1L, 12L, request(301L, "#E0A33A")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 남의_개체면_ANIMAL_NOT_FOUND() {
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> characterService.save(1L, 12L, request(null, "#E0A33A")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ANIMAL_NOT_FOUND);
    }

    private static CharacterSaveRequest request(Long photoId, String bodyColor) {
        return new CharacterSaveRequest(photoId, null, Shape.GECKO, PatternType.SPOTS,
                new Colors(bodyColor, "#3A2A1E", "#F2E6C8", "#2B2B2B"), null);
    }
}
