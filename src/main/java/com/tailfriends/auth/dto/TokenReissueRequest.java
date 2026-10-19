package com.tailfriends.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenReissueRequest(
        @NotBlank String refreshToken
) {
}
