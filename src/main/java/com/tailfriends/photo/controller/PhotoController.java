package com.tailfriends.photo.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.dto.CharacterAnalysisResponse;
import com.tailfriends.photo.dto.PhotoUploadResponse;
import com.tailfriends.photo.service.CharacterAnalysisService;
import com.tailfriends.photo.service.PhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Photo")
@RestController
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;
    private final CharacterAnalysisService characterAnalysisService;

    @Operation(summary = "사진 업로드",
            description = "JPG·PNG, 최대 20MB. 128px 썸네일을 만들고 keepOriginal이면서 원본 사진 보관에 동의했으면 긴 변 1600px 원본을 보관한다. 동의하지 않았으면 originalKept=false. 분석은 따로 호출한다.")
    @PostMapping(value = "/api/v1/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PhotoUploadResponse> upload(
            @RequestPart MultipartFile file,
            @RequestParam Long animalId,
            @RequestParam PhotoPurpose purpose,
            @RequestParam(defaultValue = "false") boolean keepOriginal) {
        return ApiResponse.success(photoService.upload(animalId, purpose, keepOriginal, file));
    }

    @Operation(summary = "캐릭터 분석 (AI)",
            description = "사진을 AI로 분석해 캐릭터 값을 돌려준다. 저장하지 않는다. 사용자당 하루 횟수 제한(KST). "
                    + "실패하면 앱은 기기에서 뽑은 색으로 진행한다.")
    @PostMapping("/api/v1/photos/{photoId}/character-analysis")
    public ApiResponse<CharacterAnalysisResponse> analyzeCharacter(
            @PathVariable Long photoId) {
        return ApiResponse.success(characterAnalysisService.analyze(photoId));
    }
}
