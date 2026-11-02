package com.tailfriends.photo.dto;

import com.tailfriends.global.util.KstDate;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * takenAt은 올린 날(KST). weightAtTime은 올릴 때 저장한 그날까지의 최근 체중(g), 없으면 null.
 * healthCheck는 사진 건강 체크가 생길 때까지 null.
 */
public record AlbumPhotoResponse(
        Long photoId,
        LocalDate takenAt,
        String thumbnailUrl,
        PhotoPurpose purpose,
        BigDecimal weightAtTime,
        HealthCheckSummary healthCheck,
        boolean originalKept
) {

    public static AlbumPhotoResponse of(Photo photo, String thumbnailUrl) {
        return new AlbumPhotoResponse(
                photo.getId(),
                LocalDate.ofInstant(photo.getCreatedAt(), KstDate.ZONE),
                thumbnailUrl,
                photo.getPurpose(),
                photo.getWeightAtTime(),
                null,
                photo.getOriginalKey() != null);
    }
}
