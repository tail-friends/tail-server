package com.tailfriends.enclosure.repository;

/**
 * 사육장별 장식 수. 장식이 없는 사육장은 나오지 않는다.
 */
public record DecorationCount(Long enclosureId, long count) {
}
