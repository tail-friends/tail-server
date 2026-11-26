package com.tailfriends.admin.repository;

/**
 * 종별 등록 개체 수. 네이티브 쿼리 결과를 받는다. 직접 입력한 종은 speciesId가 null이다.
 */
public interface SpeciesCount {

    Long getSpeciesId();

    String getName();

    Long getCount();
}
