package com.tailfriends.enclosure.dto;

import com.tailfriends.enclosure.domain.EnclosureDecoration;

public record DecorationResponse(
        Long itemId,
        double x,
        double y,
        boolean flip
) {

    public static DecorationResponse from(EnclosureDecoration decoration) {
        return new DecorationResponse(decoration.getItem().getId(), decoration.getX().doubleValue(),
                decoration.getY().doubleValue(), decoration.isFlip());
    }
}
