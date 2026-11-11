package com.tailfriends.animal.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.tailfriends.animal.domain.Sex;
import com.tailfriends.animal.service.HealthReport.FeedSummary;
import com.tailfriends.animal.service.HealthReport.Profile;
import com.tailfriends.animal.service.HealthReport.ShedSummary;
import com.tailfriends.animal.service.HealthReport.VisitEntry;
import com.tailfriends.animal.service.HealthReport.WeightEntry;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 수의사용 건강 리포트를 A4 PDF로 그린다. 한글은 나눔고딕(OFL, resources/fonts)을 넣어서 쓴다.
 */
@Component
public class HealthReportPdfWriter {

    private static final Color TEXT = new Color(0x22, 0x22, 0x22);
    private static final Color MUTED = new Color(0x6B, 0x6B, 0x6B);
    private static final Color LINE = new Color(0xD9, 0xD9, 0xD9);
    private static final Color ACCENT = new Color(0x2E, 0x7D, 0x5B);
    private static final Color HEADER_BG = new Color(0xF2, 0xF4, 0xF3);
    private static final float CHART_HEIGHT = 170f;

    private final BaseFont regular;
    private final BaseFont bold;

    public HealthReportPdfWriter() {
        this.regular = loadFont("fonts/NanumGothic-Regular.ttf");
        this.bold = loadFont("fonts/NanumGothic-Bold.ttf");
    }

    public byte[] write(HealthReport report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        document.addTitle("수의사용 건강 리포트 - " + report.profile().name());
        document.addCreator("꼬리친구");
        document.open();

        document.add(new Paragraph("수의사용 건강 리포트", font(bold, 18, TEXT)));
        document.add(spaced(new Paragraph("기간 %s ~ %s · 만든 날 %s".formatted(
                report.from(), report.to(), report.generatedOn()), font(regular, 9, MUTED)), 2, 12));

        section(document, "개체 정보");
        document.add(profileTable(report.profile()));

        section(document, "체중");
        addWeights(document, writer, report);

        section(document, "급여");
        addFeed(document, report.feed());

        section(document, "탈피");
        addShed(document, report.shed());

        section(document, "진료 기록");
        addVisits(document, report.visits());

        document.add(spaced(new Paragraph("꼬리친구 앱에 기록한 내용으로 만든 참고 자료예요. 진단과 치료는 수의사와 상담하세요.",
                font(regular, 8, MUTED)), 18, 0));
        document.close();
        return out.toByteArray();
    }

    private PdfPTable profileTable(Profile profile) {
        PdfPTable table = new PdfPTable(new float[] {1, 2, 1, 2});
        table.setWidthPercentage(100);
        labelValue(table, "이름", profile.name());
        labelValue(table, "종", profile.species());
        labelValue(table, "모프", profile.morph());
        labelValue(table, "성별", sexLabel(profile.sex()));
        labelValue(table, "해칭일", profile.hatchDate() == null ? null : profile.hatchDate().toString());
        labelValue(table, "급여 주기", profile.feedIntervalDays() + "일");
        return table;
    }

    private void addWeights(Document document, PdfWriter writer, HealthReport report) {
        List<WeightEntry> weights = report.weights();
        if (weights.isEmpty()) {
            document.add(body("기간 안 체중 기록이 없어요."));
            return;
        }
        WeightEntry first = weights.getFirst();
        WeightEntry last = weights.getLast();
        BigDecimal diff = last.grams().subtract(first.grams());
        String sign = diff.signum() > 0 ? "+" : "";
        document.add(body("처음 %sg (%s) → 마지막 %sg (%s), 변화 %s%sg, 기록 %d개".formatted(
                first.grams().toPlainString(), first.date(), last.grams().toPlainString(), last.date(),
                sign, diff.toPlainString(), weights.size())));
        float width = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
        Image chart = Image.getInstance(weightChart(writer.getDirectContent(), report, width));
        chart.setSpacingBefore(6);
        document.add(chart);
    }

    /**
     * 날짜 축은 리포트 기간 전체, 체중 축은 기록의 최소~최대에 여백을 둔다. 점과 선으로 그린다.
     */
    private PdfTemplate weightChart(PdfContentByte content, HealthReport report, float width) {
        PdfTemplate chart = content.createTemplate(width, CHART_HEIGHT);
        float left = 44;
        float right = width - 8;
        float bottom = 22;
        float top = CHART_HEIGHT - 10;

        List<WeightEntry> weights = report.weights();
        double min = weights.stream().mapToDouble(w -> w.grams().doubleValue()).min().orElse(0);
        double max = weights.stream().mapToDouble(w -> w.grams().doubleValue()).max().orElse(0);
        double padding = Math.max((max - min) * 0.1, 1);
        double low = Math.max(min - padding, 0);
        double high = max + padding;
        long start = report.from().toEpochDay();
        long span = Math.max(report.to().toEpochDay() - start, 1);

        chart.setColorStroke(LINE);
        chart.setLineWidth(0.5f);
        for (int i = 0; i <= 4; i++) {
            float y = bottom + (top - bottom) * i / 4;
            chart.moveTo(left, y);
            chart.lineTo(right, y);
            label(chart, "%.1f".formatted(low + (high - low) * i / 4), left - 4, y - 3, Element.ALIGN_RIGHT);
        }
        chart.stroke();
        label(chart, report.from().toString(), left, 6, Element.ALIGN_LEFT);
        label(chart, report.to().toString(), right, 6, Element.ALIGN_RIGHT);

        chart.setColorStroke(ACCENT);
        chart.setColorFill(ACCENT);
        chart.setLineWidth(1.5f);
        for (int i = 0; i < weights.size(); i++) {
            float x = left + (right - left) * (weights.get(i).date().toEpochDay() - start) / span;
            float y = (float) (bottom + (top - bottom) * (weights.get(i).grams().doubleValue() - low) / (high - low));
            if (i == 0) {
                chart.moveTo(x, y);
            } else {
                chart.lineTo(x, y);
            }
        }
        if (weights.size() > 1) {
            chart.stroke();
        }
        for (WeightEntry weight : weights) {
            float x = left + (right - left) * (weight.date().toEpochDay() - start) / span;
            float y = (float) (bottom + (top - bottom) * (weight.grams().doubleValue() - low) / (high - low));
            chart.circle(x, y, 2.2f);
            chart.fill();
        }
        return chart;
    }

    private void label(PdfTemplate chart, String text, float x, float y, int align) {
        chart.beginText();
        chart.setFontAndSize(regular, 7);
        chart.setColorFill(MUTED);
        chart.showTextAligned(align, text, x, y, 0);
        chart.endText();
    }

    private void addFeed(Document document, FeedSummary feed) {
        if (feed.total() == 0) {
            document.add(body("기간 안 급여 기록이 없어요."));
            return;
        }
        document.add(body("급여 기록 %d번 (먹음 %d번, 안 먹음 %d번)".formatted(feed.total(), feed.eaten(), feed.refused())));
        document.add(body("마지막으로 먹은 날: " + (feed.lastEatenDate() == null ? "기간 안에 없음" : feed.lastEatenDate())));
        if (feed.refusalStreak() > 0) {
            document.add(body("최근 연속으로 안 먹은 횟수: %d번".formatted(feed.refusalStreak())));
        }
    }

    private void addShed(Document document, ShedSummary shed) {
        if (shed.sheds() == 0 && shed.blue() == 0) {
            document.add(body("기간 안 탈피 기록이 없어요."));
            return;
        }
        document.add(body("탈피 %d번 (완탈 %d번, 부분 탈피 %d번), 블루 단계 기록 %d번".formatted(
                shed.sheds(), shed.complete(), shed.partial(), shed.blue())));
        if (shed.lastShedDate() != null) {
            document.add(body("마지막 탈피일: " + shed.lastShedDate()));
        }
    }

    private void addVisits(Document document, List<VisitEntry> visits) {
        if (visits.isEmpty()) {
            document.add(body("기간 안 진료 기록이 없어요."));
            return;
        }
        PdfPTable table = new PdfPTable(new float[] {1.1f, 1.4f, 2, 2, 2});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String header : List.of("방문일", "병원", "증상", "진단", "처방")) {
            PdfPCell cell = cell(header, font(bold, 9, TEXT));
            cell.setBackgroundColor(HEADER_BG);
            table.addCell(cell);
        }
        for (VisitEntry visit : visits) {
            table.addCell(cell(visit.visitDate().toString(), font(regular, 9, TEXT)));
            table.addCell(cell(visit.hospital(), font(regular, 9, TEXT)));
            table.addCell(cell(visit.symptom(), font(regular, 9, TEXT)));
            table.addCell(cell(visit.diagnosis(), font(regular, 9, TEXT)));
            table.addCell(cell(visit.prescription(), font(regular, 9, TEXT)));
        }
        document.add(table);
    }

    private void section(Document document, String title) {
        document.add(spaced(new Paragraph(title, font(bold, 12, ACCENT)), 14, 6));
    }

    private Paragraph body(String text) {
        Paragraph paragraph = new Paragraph(text, font(regular, 10, TEXT));
        paragraph.setSpacingAfter(2);
        return paragraph;
    }

    private void labelValue(PdfPTable table, String label, String value) {
        PdfPCell labelCell = cell(label, font(bold, 9, MUTED));
        labelCell.setBackgroundColor(HEADER_BG);
        table.addCell(labelCell);
        table.addCell(cell(value, font(regular, 10, TEXT)));
    }

    private static PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null || text.isBlank() ? "-" : text, font));
        cell.setBorderColor(LINE);
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(5);
        return cell;
    }

    private static Paragraph spaced(Paragraph paragraph, float before, float after) {
        paragraph.setSpacingBefore(before);
        paragraph.setSpacingAfter(after);
        return paragraph;
    }

    private static Font font(BaseFont base, float size, Color color) {
        return new Font(base, size, Font.NORMAL, color);
    }

    private static String sexLabel(Sex sex) {
        if (sex == null) {
            return null;
        }
        return switch (sex) {
            case MALE -> "수컷";
            case FEMALE -> "암컷";
            case UNKNOWN -> "모름";
        };
    }

    private static BaseFont loadFont(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
        } catch (IOException e) {
            throw new UncheckedIOException("PDF 폰트를 읽지 못했어요: " + path, e);
        }
    }
}
