package com.tailfriends.enclosure.dto;

import java.util.List;

/**
 * 합사 보기로 함께 띄울 개체 ID. 보낸 순서 그대로.
 */
public record EnclosureAnimalsResponse(List<Long> animalIds) {
}
