package com.tailfriends.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedReminderScheduler {

    private final FeedReminderService feedReminderService;

    /**
     * 10분마다 그 구간에 알림 시간이 있는 사용자에게 급여일 푸시를 보낸다.
     */
    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
    public void sendDueReminders() {
        try {
            feedReminderService.sendDueReminders();
        } catch (RuntimeException e) {
            log.error("급여일 푸시를 보내지 못했어요", e);
        }
    }
}
