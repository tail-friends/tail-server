package com.tailfriends.notification.fcm;

import java.util.List;

/**
 * 기기에 푸시를 보낸다. 지금은 FCM.
 */
public interface PushSender {

    /**
     * 토큰마다 따로 보낸다. 한 토큰이 실패해도 나머지는 보낸다.
     *
     * @return 더 이상 쓸 수 없는 토큰(앱 삭제 등). 부르는 쪽이 지운다
     */
    List<String> send(List<String> tokens, String title, String body);
}
