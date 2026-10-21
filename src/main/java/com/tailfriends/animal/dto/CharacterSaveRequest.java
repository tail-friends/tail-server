package com.tailfriends.animal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tailfriends.animal.domain.CharacterLook;
import com.tailfriends.animal.domain.PatternType;
import com.tailfriends.animal.domain.RenderStyle;
import com.tailfriends.species.domain.Shape;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * shape, patternType, renderStyle은 대소문자를 가리지 않는다. (명세 예시는 소문자)
 * photoId는 사진 업로드가 생기면 연결한다. 지금은 비워서 보낸다.
 */
public record CharacterSaveRequest(
        Long photoId,
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_VALUES) RenderStyle renderStyle,
        @NotNull @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_VALUES) Shape shape,
        @NotNull @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_VALUES) PatternType patternType,
        @NotNull @Valid Colors colors,
        @Valid Extra extra
) {

    private static final String HEX_COLOR = "^#[0-9A-Fa-f]{6}$";
    private static final String HEX_COLOR_MESSAGE = "#RRGGBB 형식이어야 합니다";

    public CharacterLook toLook() {
        Extra values = extra != null ? extra : new Extra(null, null, null);
        return new CharacterLook(renderStyle, shape, patternType,
                colors.body(), colors.pattern(), colors.belly(), colors.eye(),
                values.headSize(), values.tailWidth(), values.patternDensity());
    }

    public record Colors(
            @NotNull @Pattern(regexp = HEX_COLOR, message = HEX_COLOR_MESSAGE) String body,
            @NotNull @Pattern(regexp = HEX_COLOR, message = HEX_COLOR_MESSAGE) String pattern,
            @NotNull @Pattern(regexp = HEX_COLOR, message = HEX_COLOR_MESSAGE) String belly,
            @NotNull @Pattern(regexp = HEX_COLOR, message = HEX_COLOR_MESSAGE) String eye
    ) {
    }

    /**
     * 0~1, 소수 둘째 자리까지. 지금 그림에 쓰지 않아도 저장한다.
     */
    public record Extra(
            @DecimalMin("0") @DecimalMax("1") @Digits(integer = 1, fraction = 2) BigDecimal headSize,
            @DecimalMin("0") @DecimalMax("1") @Digits(integer = 1, fraction = 2) BigDecimal tailWidth,
            @DecimalMin("0") @DecimalMax("1") @Digits(integer = 1, fraction = 2) BigDecimal patternDensity
    ) {
    }
}
