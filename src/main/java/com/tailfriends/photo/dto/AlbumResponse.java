package com.tailfriends.photo.dto;

import java.util.List;

public record AlbumResponse(List<AlbumPhotoResponse> photos, Long nextCursor) {
}
