package com.tailfriends.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class UserSettingsTest {

    private final User user = User.builder().provider(Provider.KAKAO).providerId("1").nickname("망고집사").build();

    @Test
    void 기본값은_오전_9시_섭씨_기능명세서_경고_기준() {
        UserSettings settings = UserSettings.defaultsFor(user);

        assertThat(settings.getNotifyTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(settings.getTempUnit()).isEqualTo(TempUnit.C);
        assertThat(settings.alertRules()).isEqualTo(new AlertRules(3, 10, 30, 2));
    }

    @Test
    void null인_값은_그대로_두고_보낸_값만_바꾼다() {
        UserSettings settings = UserSettings.defaultsFor(user);

        settings.update(LocalTime.of(8, 30), null, 5, null, null, null);

        assertThat(settings.getNotifyTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(settings.getTempUnit()).isEqualTo(TempUnit.C);
        assertThat(settings.alertRules()).isEqualTo(new AlertRules(5, 10, 30, 2));
    }
}
