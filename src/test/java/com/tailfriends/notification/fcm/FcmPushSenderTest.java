package com.tailfriends.notification.fcm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

class FcmPushSenderTest {

    @Test
    void 키가_없으면_보내지_않고_지울_토큰도_없다() {
        FcmPushSender sender = new FcmPushSender(RestClient.create(), new FcmProperties(""));

        assertThat(sender.send(List.of("token-a"), "꼬리친구", "오늘 망고 밥 주는 날이에요")).isEmpty();
    }

    @Test
    void 등록_해제된_토큰과_형식이_틀린_토큰만_지울_토큰으로_본다() {
        assertThat(FcmPushSender.isUnregistered(error(HttpStatus.NOT_FOUND,
                "{\"error\":{\"status\":\"NOT_FOUND\",\"details\":[{\"errorCode\":\"UNREGISTERED\"}]}}"))).isTrue();
        assertThat(FcmPushSender.isUnregistered(error(HttpStatus.BAD_REQUEST,
                "{\"error\":{\"status\":\"INVALID_ARGUMENT\",\"message\":\"The registration token is not valid\"}}")))
                .isTrue();
        assertThat(FcmPushSender.isUnregistered(error(HttpStatus.TOO_MANY_REQUESTS,
                "{\"error\":{\"status\":\"RESOURCE_EXHAUSTED\"}}"))).isFalse();
        assertThat(FcmPushSender.isUnregistered(error(HttpStatus.UNAUTHORIZED,
                "{\"error\":{\"status\":\"UNAUTHENTICATED\"}}"))).isFalse();
    }

    @Test
    void 키가_Base64_JSON이_아니면_서버를_띄우지_않는다() {
        assertThatThrownBy(
                        () -> new FcmPushSender(RestClient.create(), new FcmProperties("not-base64!")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FCM_CREDENTIALS");
    }

    private static HttpClientErrorException error(HttpStatus status, String body) {
        return HttpClientErrorException.create(status, status.getReasonPhrase(), new HttpHeaders(),
                body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
