package com.tailfriends.photo.dto;

public record PhotoUploadResponse(
        Long photoId,
        String thumbnailUrl,
        boolean originalKept
) {
}
