package com.tailfriends.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AlertRulesTest {

    @Test
    void 종의_거식_기준이_더_크면_거식_기준만_올린다() {
        assertThat(AlertRules.DEFAULT.withRefusalCountAtLeast(8)).isEqualTo(new AlertRules(8, 10, 30, 2));
    }

    @Test
    void 종의_거식_기준이_없거나_작으면_그대로다() {
        AlertRules rules = new AlertRules(5, 10, 30, 2);

        assertThat(rules.withRefusalCountAtLeast(null)).isEqualTo(rules);
        assertThat(rules.withRefusalCountAtLeast(4)).isEqualTo(rules);
    }
}
