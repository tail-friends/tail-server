package com.tailfriends.log.service;

import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.domain.ShedStage;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 사육 기록을 CSV로 쓴다(F-17). 열은 노션 API 명세서의 "기록 내보내기 (CSV)"와 같다.
 * 엑셀에서 한글이 깨지지 않게 UTF-8 BOM을 붙이고, 줄바꿈은 CRLF다(RFC 4180).
 */
final class LogCsvWriter {

    static final List<String> HEADER = List.of(
            "개체명", "종", "날짜", "종류", "먹이", "개수", "안 먹음", "체중(g)", "탈피 상태", "온도(℃)", "습도(%)", "메모");

    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private static final Map<LogType, String> TYPE_LABELS = Map.of(
            LogType.FEED, "급여",
            LogType.SHED, "탈피",
            LogType.WEIGHT, "체중",
            LogType.CLEAN, "청소",
            LogType.ENV, "온습도",
            LogType.NOTE, "메모");

    private LogCsvWriter() {
    }

    static byte[] write(List<Log> logs) {
        StringBuilder csv = new StringBuilder();
        appendRow(csv, HEADER);
        for (Log log : logs) {
            appendRow(csv, row(log));
        }
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[BOM.length + body.length];
        System.arraycopy(BOM, 0, result, 0, BOM.length);
        System.arraycopy(body, 0, result, BOM.length, body.length);
        return result;
    }

    private static List<String> row(Log log) {
        List<String> row = new ArrayList<>(HEADER.size());
        row.add(log.getAnimal().getName());
        row.add(log.getAnimal().getSpeciesName());
        row.add(log.getLogDate().toString());
        row.add(TYPE_LABELS.get(log.getType()));
        boolean feed = log.getType() == LogType.FEED;
        row.add(feed ? log.getFood() : null);
        row.add(feed && log.getFoodCount() != null ? log.getFoodCount().toString() : null);
        row.add(feed ? (Boolean.TRUE.equals(log.getRefused()) ? "예" : "아니오") : null);
        row.add(log.getType() == LogType.WEIGHT ? plain(log.getWeightG()) : null);
        row.add(log.getType() == LogType.SHED ? shedLabel(log) : null);
        row.add(log.getType() == LogType.ENV ? temperature(log.getHotC(), log.getCoolC()) : null);
        row.add(log.getType() == LogType.ENV && log.getHumidity() != null ? log.getHumidity().toString() : null);
        row.add(log.getMemo());
        return row;
    }

    private static String shedLabel(Log log) {
        if (log.getShedStage() == ShedStage.BLUE) {
            return "블루 단계";
        }
        return Boolean.TRUE.equals(log.getShedComplete()) ? "완탈" : "부분 탈피";
    }

    /**
     * 온도 열은 하나라서 핫존과 쿨존을 함께 적는다. 예: "핫존 32.0 / 쿨존 26.0"
     */
    private static String temperature(BigDecimal hot, BigDecimal cool) {
        List<String> parts = new ArrayList<>(2);
        if (hot != null) {
            parts.add("핫존 " + plain(hot));
        }
        if (cool != null) {
            parts.add("쿨존 " + plain(cool));
        }
        return parts.isEmpty() ? null : String.join(" / ", parts);
    }

    private static String plain(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private static void appendRow(StringBuilder csv, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(values.get(i)));
        }
        csv.append("\r\n");
    }

    /**
     * 쉼표·따옴표·줄바꿈이 있으면 따옴표로 감싼다. 사용자가 적은 글이 =, +, -, @로 시작하면
     * 엑셀이 수식으로 실행하지 않게 앞에 '를 붙인다(CSV 수식 주입 방지).
     */
    static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String safe = "=+-@\t\r".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
