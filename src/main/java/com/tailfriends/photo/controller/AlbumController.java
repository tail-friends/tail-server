package com.tailfriends.photo.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.photo.dto.AlbumResponse;
import com.tailfriends.photo.service.AlbumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Photo")
@RestController
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    @Operation(summary = "성장 앨범",
            description = "개체의 캐릭터·건강·앨범 사진, 최신순. 사육장 사진은 빠진다. cursor는 이전 응답의 nextCursor.")
    @GetMapping("/api/v1/photos")
    public ApiResponse<AlbumResponse> getAlbum(@RequestParam Long animalId,
                                               @RequestParam(required = false) Long cursor,
                                               @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(albumService.getAlbum(animalId, cursor, size));
    }
}
