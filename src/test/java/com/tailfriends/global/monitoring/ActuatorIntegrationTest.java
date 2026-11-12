package com.tailfriends.global.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.tailfriends.global.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * 관리 포트는 실제 서버로 띄워야 생기므로 RANDOM_PORT로 확인한다.
 * 테스트에서는 지표 내보내기가 기본으로 꺼져 있어 {@code @AutoConfigureObservability}로 켠다.
 */
@AutoConfigureObservability(tracing = false)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "management.server.port=0")
class ActuatorIntegrationTest extends IntegrationTestSupport {

    @LocalServerPort
    private int serverPort;

    @LocalManagementPort
    private int managementPort;

    private final RestClient restClient = RestClient.builder()
            .defaultStatusHandler(status -> true, (request, response) -> { })
            .build();

    @Test
    void 관리_포트에서_토큰_없이_프로메테우스_지표를_수집한다() {
        ResponseEntity<String> response = get(managementPort, "/actuator/prometheus");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("jvm_memory_used_bytes")
                .contains("application=\"tail-server\"");
    }

    @Test
    void 관리_포트에서_토큰_없이_헬스_상태를_확인한다() {
        ResponseEntity<String> response = get(managementPort, "/actuator/health");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void 관리_포트는_노출하지_않은_엔드포인트를_열지_않는다() {
        ResponseEntity<String> response = get(managementPort, "/actuator/env");

        assertThat(response.getStatusCode().is2xxSuccessful()).isFalse();
    }

    @Test
    void API_포트에서는_액추에이터_엔드포인트를_열지_않는다() {
        ResponseEntity<String> response = get(serverPort, "/actuator/prometheus");

        assertThat(response.getStatusCode().is2xxSuccessful()).isFalse();
    }

    private ResponseEntity<String> get(int port, String path) {
        return restClient.get()
                .uri("http://localhost:" + port + path)
                .retrieve()
                .toEntity(String.class);
    }
}
