package com.tailfriends.notification.domain;

import java.util.List;
import java.util.Optional;

/**
 * 급여일 알림 문구. 오늘 급여일인 개체와 급여일이 2일 지난 개체를 한 알림으로 묶는다(재알림은 2일째 한 번).
 */
public final class FeedReminder {

    public static final String TITLE = "꼬리친구";
    public static final int OVERDUE_REMIND_DAYS = 2;
    static final int MAX_NAMES = 3;

    private FeedReminder() {
    }

    /**
     * @return 보낼 개체가 없으면 빈 값
     */
    public static Optional<String> body(List<String> dueToday, List<String> overdue) {
        if (dueToday.isEmpty() && overdue.isEmpty()) {
            return Optional.empty();
        }
        if (overdue.isEmpty()) {
            return Optional.of("오늘 %s 밥 주는 날이에요".formatted(names(dueToday)));
        }
        String late = "%s 급여일이 %d일 지났어요".formatted(names(overdue), OVERDUE_REMIND_DAYS);
        if (dueToday.isEmpty()) {
            return Optional.of(late);
        }
        return Optional.of("오늘 %s 밥 주는 날이에요. %s".formatted(names(dueToday), late));
    }

    /**
     * 세 마리까지 이름을 쓰고 나머지는 "외 n마리".
     */
    static String names(List<String> names) {
        String shown = String.join(", ", names.subList(0, Math.min(names.size(), MAX_NAMES)));
        return names.size() > MAX_NAMES ? "%s 외 %d마리".formatted(shown, names.size() - MAX_NAMES) : shown;
    }
}
