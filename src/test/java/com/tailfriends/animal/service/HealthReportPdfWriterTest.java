package com.tailfriends.animal.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.tailfriends.animal.domain.Sex;
import com.tailfriends.animal.service.HealthReport.FeedSummary;
import com.tailfriends.animal.service.HealthReport.Profile;
import com.tailfriends.animal.service.HealthReport.ShedSummary;
import com.tailfriends.animal.service.HealthReport.VisitEntry;
import com.tailfriends.animal.service.HealthReport.WeightEntry;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class HealthReportPdfWriterTest {

    private static final LocalDate FROM = LocalDate.of(2026, 8, 11);
    private static final LocalDate TO = LocalDate.of(2026, 11, 11);
    private static final Profile PROFILE =
            new Profile("망고", "레오파드 게코", "탠저린", Sex.FEMALE, LocalDate.of(2025, 6, 20), 4);

    private final HealthReportPdfWriter writer = new HealthReportPdfWriter();

    @Test
    void 한글이_들어간_PDF를_만들고_요약과_진료_기록을_담는다() throws IOException {
        HealthReport report = new HealthReport(TO, FROM, TO, PROFILE,
                List.of(new WeightEntry(LocalDate.of(2026, 9, 1), new BigDecimal("48.0")),
                        new WeightEntry(LocalDate.of(2026, 10, 3), new BigDecimal("57.8"))),
                new FeedSummary(10, 3, LocalDate.of(2026, 11, 1), 2),
                new ShedSummary(1, 1, 1, LocalDate.of(2026, 9, 28)),
                List.of(new VisitEntry(LocalDate.of(2026, 10, 2), "제주 특수동물병원", "부분 탈피", "탈피 부전", "온욕 주 2회")));

        String text = text(writer.write(report));

        assertThat(text).contains("수의사용 건강 리포트", "망고", "레오파드 게코", "암컷", "2025-06-20", "4일");
        assertThat(text).contains("처음 48.0g", "마지막 57.8g", "변화 +9.8g");
        assertThat(text).contains("급여 기록 10번", "안 먹음 3번", "최근 연속으로 안 먹은 횟수: 2번");
        assertThat(text).contains("완탈 1번", "부분 탈피 1번", "마지막 탈피일: 2026-09-28");
        assertThat(text).contains("제주 특수동물병원", "탈피 부전", "온욕 주 2회");
    }

    @Test
    void 기록이_없어도_PDF를_만들고_없다고_적는다() throws IOException {
        HealthReport report = new HealthReport(TO, FROM, TO,
                new Profile("초코", "볼 파이톤", null, Sex.UNKNOWN, null, 10),
                List.of(), new FeedSummary(0, 0, null, 0), new ShedSummary(0, 0, 0, null), List.of());

        String text = text(writer.write(report));

        assertThat(text).contains("초코", "모름", "기간 안 체중 기록이 없어요", "기간 안 급여 기록이 없어요",
                "기간 안 탈피 기록이 없어요", "기간 안 진료 기록이 없어요");
    }

    @Test
    void 체중이_한_번이어도_그래프를_그린다() throws IOException {
        HealthReport report = new HealthReport(TO, FROM, TO, PROFILE,
                List.of(new WeightEntry(LocalDate.of(2026, 9, 1), new BigDecimal("48.0"))),
                new FeedSummary(0, 0, null, 0), new ShedSummary(0, 0, 0, null), List.of());

        assertThat(text(writer.write(report))).contains("변화 0.0g", "기록 1개");
    }

    private static String text(byte[] pdf) throws IOException {
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }
}
