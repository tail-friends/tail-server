package com.tailfriends.photo.dto;

import com.tailfriends.global.util.KstDate;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * takenAt은 올린 날(KST). weightAtTime은 올릴 때 저장한 그날까지의 최근 체중(g), 없으면 null.
 * healthCheck는 이 사진으로 한 가장 최근 건강 체크 요약, 체크하지 않았으면 null.
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

    public static AlbumPhotoResponse of(Photo photo, String thumbnailUrl, HealthCheckSummary healthCheck) {
        return new AlbumPhotoResponse(
                photo.getId(),
                LocalDate.ofInstant(photo.getCreatedAt(), KstDate.ZONE),
                thumbnailUrl,
                photo.getPurpose(),
                photo.getWeightAtTime(),
                healthCheck,
                photo.getOriginalKey() != null);
    }
}
