package com.tailfriends.log.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.Sex;
import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogDetails;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.domain.ShedStage;
import com.tailfriends.log.domain.Stool;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogCsvWriterTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 18);
    private static final Animal MANGO = Animal.builder()
            .name("망고").speciesName("레오파드 게코").sex(Sex.FEMALE).feedIntervalDays(4).build();

    @Test
    void UTF8_BOM으로_시작하고_머리줄은_명세의_열_순서다() {
        byte[] csv = LogCsvWriter.write(List.of());

        assertThat(Arrays.copyOf(csv, 3)).containsExactly(0xEF, 0xBB, 0xBF);
        assertThat(lines(csv)).containsExactly(
                "개체명,종,날짜,종류,먹이,개수,안 먹음,체중(g),탈피 상태,온도(℃),습도(%),메모");
    }

    @Test
    void 기록_종류마다_해당하는_열만_채운다() {
        byte[] csv = LogCsvWriter.write(List.of(
                log(LogType.FEED, "", details("귀뚜라미", 6, true, null, null, null, null, null, null)),
                log(LogType.WEIGHT, null, details(null, null, null, null, null, new BigDecimal("57.8"), null, null,
                        null)),
                log(LogType.SHED, "꼬리 끝", details(null, null, null, ShedStage.SHED, false, null, null, null, null)),
                log(LogType.SHED, null, details(null, null, null, ShedStage.SHED, true, null, null, null, null)),
                log(LogType.SHED, null, details(null, null, null, ShedStage.BLUE, null, null, null, null, null)),
                log(LogType.ENV, null, details(null, null, null, null, null, null, new BigDecimal("32.0"),
                        new BigDecimal("26.5"), 55)),
                log(LogType.CLEAN, null, cleanDetails()),
                log(LogType.NOTE, "허물 벗음", details(null, null, null, null, null, null, null, null, null))));

        assertThat(lines(csv)).containsExactly(
                "개체명,종,날짜,종류,먹이,개수,안 먹음,체중(g),탈피 상태,온도(℃),습도(%),메모",
                "망고,레오파드 게코,2026-10-18,급여,귀뚜라미,6,예,,,,,",
                "망고,레오파드 게코,2026-10-18,체중,,,,57.8,,,,",
                "망고,레오파드 게코,2026-10-18,탈피,,,,,부분 탈피,,,꼬리 끝",
                "망고,레오파드 게코,2026-10-18,탈피,,,,,완탈,,,",
                "망고,레오파드 게코,2026-10-18,탈피,,,,,블루 단계,,,",
                "망고,레오파드 게코,2026-10-18,온습도,,,,,,핫존 32.0 / 쿨존 26.5,55,",
                "망고,레오파드 게코,2026-10-18,청소,,,,,,,,",
                "망고,레오파드 게코,2026-10-18,메모,,,,,,,,허물 벗음");
    }

    @Test
    void 안_먹음이_아니면_아니오이고_온도는_적은_것만_쓴다() {
        byte[] csv = LogCsvWriter.write(List.of(
                log(LogType.FEED, null, details("밀웜", null, false, null, null, null, null, null, null)),
                log(LogType.ENV, null, details(null, null, null, null, null, null, null, new BigDecimal("25.0"),
                        null))));

        assertThat(lines(csv)).element(1).isEqualTo("망고,레오파드 게코,2026-10-18,급여,밀웜,,아니오,,,,,");
        assertThat(lines(csv)).element(2).isEqualTo("망고,레오파드 게코,2026-10-18,온습도,,,,,,쿨존 25.0,,");
    }

    @Test
    void 쉼표_따옴표_줄바꿈이_있으면_따옴표로_감싼다() {
        assertThat(LogCsvWriter.escape("밀웜, 귀뚜라미")).isEqualTo("\"밀웜, 귀뚜라미\"");
        assertThat(LogCsvWriter.escape("\"잘 먹음\"")).isEqualTo("\"\"\"잘 먹음\"\"\"");
        assertThat(LogCsvWriter.escape("첫 줄\n둘째 줄")).isEqualTo("\"첫 줄\n둘째 줄\"");
        assertThat(LogCsvWriter.escape(null)).isEmpty();
    }

    @Test
    void 수식으로_시작하는_글은_엑셀이_실행하지_않게_따옴표를_붙인다() {
        assertThat(LogCsvWriter.escape("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(LogCsvWriter.escape("+1")).isEqualTo("'+1");
        assertThat(LogCsvWriter.escape("-2")).isEqualTo("'-2");
        assertThat(LogCsvWriter.escape("@SUM")).isEqualTo("'@SUM");
        assertThat(LogCsvWriter.escape("귀뚜라미")).isEqualTo("귀뚜라미");
    }

    private static List<String> lines(byte[] csv) {
        String text = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
        assertThat(text).endsWith("\r\n");
        return List.of(text.split("\r\n"));
    }

    private static Log log(LogType type, String memo, LogDetails details) {
        return Log.create(MANGO, null, null, type, DAY, memo, details);
    }

    private static LogDetails details(String food, Integer count, Boolean refused, ShedStage stage,
                                      Boolean complete, BigDecimal weight, BigDecimal hot, BigDecimal cool,
                                      Integer humidity) {
        return new LogDetails(food, count, refused, null, stage, complete, weight, null, null, hot, cool, humidity);
    }

    private static LogDetails cleanDetails() {
        return new LogDetails(null, null, null, null, null, null, null, true, Stool.NORMAL, null, null, null);
    }
}
