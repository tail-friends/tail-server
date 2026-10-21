package com.tailfriends.photo.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.dto.PhotoUploadResponse;
import com.tailfriends.photo.service.PhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @Operation(summary = "사진 업로드",
            description = "JPG·PNG, 최대 20MB. 128px 썸네일을 만들고 keepOriginal이면 긴 변 1600px 원본을 보관한다. 분석은 따로 호출한다.")
    @PostMapping(value = "/api/v1/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PhotoUploadResponse> upload(
            @AuthenticationPrincipal Long userId,
            @RequestPart MultipartFile file,
            @RequestParam Long animalId,
            @RequestParam PhotoPurpose purpose,
            @RequestParam(defaultValue = "false") boolean keepOriginal) {
        return ApiResponse.success(photoService.upload(userId, animalId, purpose, keepOriginal, file));
    }
}
