// （文件较长，保持你项目里完整可替换的版本）
// 你把下面整段从 package 到最后一行全部替换掉你原来的 PdfUtil.java

package com.sphas.project03.utils;

import com.sphas.project03.controller.dto.WeeklyReportDTO;
import com.sphas.project03.controller.dto.MonthlyReportDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF工具：生成周报PDF（支持中文）
 */
public class PdfUtil {
    private static final float PAGE_W = 595f;     // A4宽
    private static final float PAGE_H = 842f;     // A4高
    private static final float MARGIN = 50f;      // 左右边距
    private static final float HEADER_H = 70f;    // 页眉占用高度
    private static final float FOOTER_H = 45f;    // 页脚占用高度

    public static byte[] buildWeeklyReport(WeeklyReportDTO dto) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 1) 加载字体（resources）
            try (InputStream is = PdfUtil.class.getResourceAsStream("/fonts/AlibabaPuHuiTi-2-55-Regular.ttf")) {
                if (is == null) throw new RuntimeException("字体文件未找到: /fonts/AlibabaPuHuiTi-2-55-Regular.ttf");
                PDType0Font font = PDType0Font.load(doc, is);

                // 2) PdfCtx：负责自动分页 + 每页重复页眉页脚
                PdfCtx ctx = new PdfCtx(doc, font, "SPHAS 健康周报", dto.getFrom(), dto.getTo());
                ctx.newPage(); // 开第一页（里面会画页眉）

                float x = MARGIN;
                float tableW = PAGE_W - 2 * MARGIN;

                // ====== 标题：本周核心数据 ======
                ctx.ensureSpace(30);
                drawText(ctx.cs, font, 12, "本周核心数据", x, ctx.y);
                ctx.y -= 18;

                // ====== 两列表格（支持右列自动换行 + 自动分页）======
                float col1W = tableW * 0.30f;
                float col2W = tableW * 0.70f;

                // 表头（固定一行）
                ctx.ensureSpace(28);
                ctx.cs.setNonStrokingColor(230, 238, 247); // 表头底色
                ctx.cs.addRect(x, ctx.y - 24, tableW, 24);
                ctx.cs.fill();
                ctx.cs.setNonStrokingColor(0, 0, 0);

                ctx.cs.setLineWidth(0.7f);
                ctx.cs.addRect(x, ctx.y - 24, tableW, 24);
                ctx.cs.stroke();
                ctx.cs.moveTo(x + col1W, ctx.y - 24);
                ctx.cs.lineTo(x + col1W, ctx.y);
                ctx.cs.stroke();

                drawText(ctx.cs, font, 11, "指标", x + 10, ctx.y - 16);
                drawText(ctx.cs, font, 11, "值", x + col1W + 10, ctx.y - 16);
                ctx.y -= 24;

                int fs = 11;
                float lineH = 16f;

                drawTwoColRow(ctx, x, tableW, "记录天数", safe(dto.getDays()) + " 天", col1W, col2W, fs, lineH, false);
                drawTwoColRow(ctx, x, tableW, "平均体重(kg)", safe(dto.getAvgWeight()), col1W, col2W, fs, lineH, true);
                drawTwoColRow(ctx, x, tableW, "平均步数", safe(dto.getAvgSteps()), col1W, col2W, fs, lineH, false);
                drawTwoColRow(ctx, x, tableW, "平均睡眠(h)", safe(dto.getAvgSleepHours()), col1W, col2W, fs, lineH, true);

                ctx.y -= 16;

                // ====== 总结 ======
                ctx.ensureSpace(20);
                drawText(ctx.cs, font, 12, "总结", x, ctx.y);
                ctx.y -= 18;

                drawParagraph(ctx, "• " + safe(dto.getSummary()), 11, x, tableW, 16f);
                ctx.y -= 12;

                // ====== 建议 ======
                ctx.ensureSpace(20);
                drawText(ctx.cs, font, 12, "建议", x, ctx.y);
                ctx.y -= 18;

                if (dto.getSuggestions() != null && !dto.getSuggestions().isEmpty()) {
                    int i = 1;
                    for (String s : dto.getSuggestions()) {
                        drawParagraph(ctx, i + ". " + s, 11, x, tableW, 16f);
                        ctx.y -= 6;
                        i++;
                    }
                } else {
                    drawParagraph(ctx, "暂无建议", 11, x, tableW, 16f);
                }

                // 3) 最后一页收尾（补页脚+关闭流）
                ctx.finish();

                // 4) 输出
                doc.save(out);
                return out.toByteArray();
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("PDF生成失败");
        }
    }

    /** 月报PDF（最近30天） */
    public static byte[] buildMonthlyReport(MonthlyReportDTO dto) {
        // 说明：版式复用周报模板，只是标题改成“月报”
        WeeklyReportDTO temp = new WeeklyReportDTO();
        temp.setFrom(dto.getFrom());
        temp.setTo(dto.getTo());
        temp.setDays(dto.getDays());
        temp.setAvgWeight(dto.getAvgWeight());
        temp.setAvgSteps(dto.getAvgSteps());
        temp.setAvgSleepHours(dto.getAvgSleepHours());
        temp.setWeightTrend(dto.getWeightTrend());
        temp.setBpRisk(dto.getBpRisk());
        temp.setSuggestions(dto.getSuggestions());
        temp.setSummary(dto.getSummary());

        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            try (InputStream is = PdfUtil.class.getResourceAsStream("/fonts/AlibabaPuHuiTi-2-55-Regular.ttf")) {
                if (is == null) throw new RuntimeException("字体文件未找到: /fonts/AlibabaPuHuiTi-2-55-Regular.ttf");
                PDType0Font font = PDType0Font.load(doc, is);

                PdfCtx ctx = new PdfCtx(doc, font, "SPHAS 健康月报", dto.getFrom(), dto.getTo());
                ctx.newPage();

                float x = MARGIN;
                float tableW = PAGE_W - 2 * MARGIN;

                ctx.ensureSpace(30);
                drawText(ctx.cs, font, 12, "本月核心数据", x, ctx.y);
                ctx.y -= 18;

                float col1W = tableW * 0.30f;
                float col2W = tableW * 0.70f;

                ctx.ensureSpace(28);
                ctx.cs.setNonStrokingColor(230, 238, 247);
                ctx.cs.addRect(x, ctx.y - 24, tableW, 24);
                ctx.cs.fill();
                ctx.cs.setNonStrokingColor(0, 0, 0);

                ctx.cs.setLineWidth(0.7f);
                ctx.cs.addRect(x, ctx.y - 24, tableW, 24);
                ctx.cs.stroke();
                ctx.cs.moveTo(x + col1W, ctx.y - 24);
                ctx.cs.lineTo(x + col1W, ctx.y);
                ctx.cs.stroke();

                drawText(ctx.cs, font, 11, "指标", x + 10, ctx.y - 16);
                drawText(ctx.cs, font, 11, "值", x + col1W + 10, ctx.y - 16);
                ctx.y -= 24;

                int fs = 11;
                float lineH = 16f;

                drawTwoColRow(ctx, x, tableW, "记录天数", safe(temp.getDays()) + " 天", col1W, col2W, fs, lineH, false);
                drawTwoColRow(ctx, x, tableW, "平均体重(kg)", safe(temp.getAvgWeight()), col1W, col2W, fs, lineH, true);
                drawTwoColRow(ctx, x, tableW, "平均步数", safe(temp.getAvgSteps()), col1W, col2W, fs, lineH, false);
                drawTwoColRow(ctx, x, tableW, "平均睡眠(h)", safe(temp.getAvgSleepHours()), col1W, col2W, fs, lineH, true);

                ctx.y -= 16;

                ctx.ensureSpace(20);
                drawText(ctx.cs, font, 12, "总结", x, ctx.y);
                ctx.y -= 18;
                drawParagraph(ctx, "• " + safe(temp.getSummary()), 11, x, tableW, 16f);
                ctx.y -= 12;

                ctx.ensureSpace(20);
                drawText(ctx.cs, font, 12, "建议", x, ctx.y);
                ctx.y -= 18;

                if (temp.getSuggestions() != null && !temp.getSuggestions().isEmpty()) {
                    int i = 1;
                    for (String s : temp.getSuggestions()) {
                        drawParagraph(ctx, i + ". " + s, 11, x, tableW, 16f);
                        ctx.y -= 6;
                        i++;
                    }
                } else {
                    drawParagraph(ctx, "暂无建议", 11, x, tableW, 16f);
                }

                ctx.finish();
                doc.save(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("PDF生成失败");
        }
    }

    private static void drawText(PDPageContentStream cs, PDType0Font font, int fontSize,
                                 String text, float x, float y) throws Exception {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text == null ? "" : text);
        cs.endText();
    }

    private static byte[] readBytes(InputStream is) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) != -1) {
            bos.write(buf, 0, len);
        }
        return bos.toByteArray();
    }

    private static void drawHeader(PDDocument doc, PDPageContentStream cs, PDType0Font font,
                                   String title, String from, String to) throws Exception {

        float pageW = 595f;
        float pageH = 842f;

        cs.setNonStrokingColor(35, 78, 120);
        cs.addRect(0, pageH - 57, pageW, 57);
        cs.fill();

        try (InputStream logoIs = PdfUtil.class.getResourceAsStream("/pdf/logo.png")) {
            if (logoIs != null) {
                byte[] bytes = readBytes(logoIs);
                PDImageXObject img = PDImageXObject.createFromByteArray(doc, bytes, "logo");
                cs.drawImage(img, 40, pageH - 52, 40, 40);
            }
        } catch (Exception ignore) {}

        cs.setNonStrokingColor(255, 255, 255);
        drawText(cs, font, 18, title, 90, pageH - 30);
        drawText(cs, font, 11, "日期范围：" + safe(from) + " ~ " + safe(to), 90, pageH - 48);

        cs.setNonStrokingColor(0, 0, 0);
    }

    private static void drawFooter(PDPageContentStream cs, PDType0Font font, int pageNo) throws Exception {
        float pageW = 595f;

        String t = "生成时间：" + LocalDateTime.now().toString().replace("T", " ").substring(0, 16);
        drawText(cs, font, 9, t, 50, 30);
        drawText(cs, font, 9, "Page " + pageNo, pageW - 110, 30);
    }

    private static class PdfCtx {
        PDDocument doc;
        PDType0Font font;
        String title;
        String from;
        String to;

        PDPage page;
        PDPageContentStream cs;

        int pageNo = 0;
        float y;

        PdfCtx(PDDocument doc, PDType0Font font, String title, String from, String to) {
            this.doc = doc;
            this.font = font;
            this.title = title;
            this.from = from;
            this.to = to;
        }

        void newPage() throws Exception {
            if (cs != null) {
                drawFooter(cs, font, pageNo);
                cs.close();
            }

            pageNo++;
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);

            drawHeader(doc, cs, font, title, from, to);

            y = PAGE_H - MARGIN - HEADER_H;
        }

        void ensureSpace(float need) throws Exception {
            float minY = MARGIN + FOOTER_H;
            if (y - need < minY) {
                newPage();
            }
        }

        void finish() throws Exception {
            if (cs != null) {
                drawFooter(cs, font, pageNo);
                cs.close();
                cs = null;
            }
        }
    }

    private static List<String> wrapText(PDType0Font font, int fontSize, String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null) return lines;

        String t = text.trim();
        if (t.isEmpty()) return lines;

        StringBuilder line = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            line.append(ch);

            if (textWidth(font, fontSize, line.toString()) > maxWidth) {
                line.deleteCharAt(line.length() - 1);
                lines.add(line.toString());
                line = new StringBuilder();
                line.append(ch);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private static float textWidth(PDType0Font font, int fontSize, String text) {
        try {
            return font.getStringWidth(text) / 1000f * fontSize;
        } catch (Exception e) {
            return text.length() * fontSize;
        }
    }

    private static void drawParagraph(PdfCtx ctx, String text, int fontSize, float x, float maxWidth, float lineH)
            throws Exception {

        List<String> lines = wrapText(ctx.font, fontSize, text, maxWidth);

        float needH = lines.size() * lineH;
        ctx.ensureSpace(needH + 6);

        for (String line : lines) {
            drawText(ctx.cs, ctx.font, fontSize, line, x, ctx.y);
            ctx.y -= lineH;
        }
    }

    private static void drawTwoColRow(PdfCtx ctx, float x, float tableW,
                                      String left, String right,
                                      float col1W, float col2W,
                                      int fontSize, float lineH,
                                      boolean zebra) throws Exception {

        List<String> rightLines = wrapText(ctx.font, fontSize, right, col2W - 20);
        int lines = Math.max(1, rightLines.size());
        float rowH = Math.max(24f, lines * lineH + 10);

        ctx.ensureSpace(rowH + 2);

        float topY = ctx.y;

        if (zebra) {
            ctx.cs.setNonStrokingColor(245, 245, 245);
            ctx.cs.addRect(x, topY - rowH, tableW, rowH);
            ctx.cs.fill();
            ctx.cs.setNonStrokingColor(0, 0, 0);
        }

        ctx.cs.setLineWidth(0.6f);
        ctx.cs.addRect(x, topY - rowH, tableW, rowH);
        ctx.cs.stroke();
        ctx.cs.moveTo(x + col1W, topY - rowH);
        ctx.cs.lineTo(x + col1W, topY);
        ctx.cs.stroke();

        drawText(ctx.cs, ctx.font, fontSize, left, x + 10, topY - 16);

        float ty = topY - 16;
        if (rightLines.isEmpty()) rightLines.add("-");
        for (String line : rightLines) {
            drawText(ctx.cs, ctx.font, fontSize, line, x + col1W + 10, ty);
            ty -= lineH;
        }

        ctx.y -= rowH;
    }

    private static String safe(Object o) {
        return o == null ? "-" : String.valueOf(o);
    }
}