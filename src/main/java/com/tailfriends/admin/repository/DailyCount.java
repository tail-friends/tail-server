package com.tailfriends.admin.repository;

/**
 * 날짜(KST, YYYY-MM-DD)별 개수. 네이티브 쿼리 결과를 받는다.
 */
public interface DailyCount {

    String getDay();

    Long getCount();
}
