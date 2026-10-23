package com.tailfriends.animal.controller;

import com.tailfriends.animal.dto.CharacterResponse;
import com.tailfriends.animal.dto.CharacterSaveRequest;
import com.tailfriends.animal.service.CharacterService;
import com.tailfriends.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Animal")
@RestController
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @Operation(summary = "캐릭터 저장", description = "없으면 만들고 있으면 바꾼다. 색은 #RRGGBB, extra는 0~1. 지금은 photoId 없이 보낸다.")
    @PutMapping("/api/v1/animals/{animalId}/character")
    public ApiResponse<CharacterResponse> save(@PathVariable Long animalId,
                                               @Valid @RequestBody CharacterSaveRequest request) {
        return ApiResponse.success(characterService.save(animalId, request));
    }
}
