package com.tailfriends.notification.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * FCM HTTP v1 API로 보낸다. 서비스 계정 키로 OAuth 액세스 토큰을 받아 헤더에 넣는다.
 * 키가 없으면 보내지 않고 로그만 남긴다.
 */
@Slf4j
@Component
public class FcmPushSender implements PushSender {

    static final String BASE_URL = "https://fcm.googleapis.com/v1/projects/";
    static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private final RestClient fcmRestClient;
    private final GoogleCredentials credentials;
    private final String projectId;

    @Autowired
    public FcmPushSender(RestClient fcmRestClient, FcmProperties properties) {
        this(fcmRestClient, load(properties.credentials()));
    }

    FcmPushSender(RestClient fcmRestClient, ServiceAccountCredentials credentials) {
        this.fcmRestClient = fcmRestClient;
        this.credentials = credentials == null ? null : credentials.createScoped(SCOPE);
        this.projectId = credentials == null ? null : credentials.getProjectId();
    }

    @Override
    public List<String> send(List<String> tokens, String title, String body) {
        if (credentials == null) {
            log.info("FCM 키(FCM_CREDENTIALS)가 없어 푸시를 보내지 않아요: {}건", tokens.size());
            return List.of();
        }
        String accessToken;
        try {
            credentials.refreshIfExpired();
            accessToken = credentials.getAccessToken().getTokenValue();
        } catch (IOException | RuntimeException e) {
            log.warn("FCM 액세스 토큰을 받지 못했어요", e);
            return List.of();
        }
        List<String> invalid = new ArrayList<>();
        for (String token : tokens) {
            if (!sendOne(accessToken, token, title, body)) {
                invalid.add(token);
            }
        }
        return invalid;
    }

    /**
     * @return 토큰을 더 쓸 수 없으면 false. 그 밖의 실패는 로그만 남기고 true
     */
    private boolean sendOne(String accessToken, String token, String title, String body) {
        Map<String, Object> message = Map.of("message", Map.of(
                "token", token,
                "notification", Map.of("title", title, "body", body)));
        try {
            fcmRestClient.post()
                    .uri(BASE_URL + projectId + "/messages:send")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(message)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            if (isUnregistered(e)) {
                return false;
            }
            log.warn("FCM 발송 실패: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return true;
        } catch (RestClientException e) {
            log.warn("FCM 발송 실패: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 앱을 지웠거나 토큰이 바뀐 경우. FCM은 404 UNREGISTERED, 형식이 틀린 토큰은 400 INVALID_ARGUMENT를 준다.
     */
    static boolean isUnregistered(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        return e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND) && body.contains("UNREGISTERED")
                || e.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST) && body.contains("INVALID_ARGUMENT")
                && body.contains("token");
    }

    private static ServiceAccountCredentials load(String base64) {
        if (!StringUtils.hasText(base64)) {
            return null;
        }
        try {
            byte[] json = Base64.getDecoder().decode(base64.strip());
            return ServiceAccountCredentials.fromStream(new ByteArrayInputStream(json));
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("FCM_CREDENTIALS가 Base64로 인코딩한 서비스 계정 키(JSON)가 아니에요", e);
        }
    }
}
