package com.sphas.project03.utils;

import com.sphas.project03.controller.dto.WeeklyReportDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import java.io.InputStream; // 读取resources字体
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject; // 画图片
import java.time.LocalDateTime; // 页脚时间
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
                PdfCtx ctx = new PdfCtx(doc, font, dto);
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

                // 这里的字段你按你的dto改（下面我教你对齐）
                drawTwoColRow(ctx, x, tableW, "记录天数", safe(dto.getDays()) + " 天", col1W, col2W, fs, lineH, false);
                drawTwoColRow(ctx, x, tableW, "平均体重(kg)", safe(dto.getAvgWeight()), col1W, col2W, fs, lineH, true);
                drawTwoColRow(ctx, x, tableW, "平均步数", safe(dto.getAvgSteps()), col1W, col2W, fs, lineH, false);
                drawTwoColRow(ctx, x, tableW, "平均睡眠(h)", safe(dto.getAvgSleepHours()), col1W, col2W, fs, lineH, true);

                ctx.y -= 16;

                // ====== 总结（自动换行 + 自动分页）======
                ctx.ensureSpace(20);
                drawText(ctx.cs, font, 12, "总结", x, ctx.y);
                ctx.y -= 18;

                drawParagraph(ctx, "• " + safe(dto.getSummary()), 11, x, tableW, 16f);
                ctx.y -= 12;

                // ====== 建议（很多条会自动分页）======
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




    // 选一个可用的系统字体
//    private static File pickFontFile() {
//        // ✅ 优先用 TTF（最稳）
//        File yahei = new File("C:\\Windows\\Fonts\\msyh.ttf");   // 微软雅黑 ttf
//        if (yahei.exists()) return yahei;
//
//        File simhei = new File("C:\\Windows\\Fonts\\simhei.ttf"); // 黑体 ttf
//        if (simhei.exists()) return simhei;
//
//        // ⚠️ 最后才用 TTC（有些环境会解析失败）
//        File yaheiTtc = new File("C:\\Windows\\Fonts\\msyh.ttc");
//        if (yaheiTtc.exists()) return yaheiTtc;
//
//        File simsunTtc = new File("C:\\Windows\\Fonts\\simsun.ttc");
//        if (simsunTtc.exists()) return simsunTtc;
//
//        throw new RuntimeException("找不到可用中文字体，请检查 C:\\Windows\\Fonts");
//    }
    private static void drawText(PDPageContentStream cs, PDType0Font font, int fontSize,
                                 String text, float x, float y) throws Exception {
        cs.beginText();                 // 开始写字
        cs.setFont(font, fontSize);     // 设置字体
        cs.newLineAtOffset(x, y);       // 坐标
        cs.showText(text == null ? "" : text);
        cs.endText();                   // 结束写字
    }

    private static byte[] readBytes(InputStream is) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096]; // 缓冲区
        int len;
        while ((len = is.read(buf)) != -1) {
            bos.write(buf, 0, len);
        }
        return bos.toByteArray();
    }

    private static void drawHeader(PDDocument doc, PDPageContentStream cs, PDType0Font font,
                                   WeeklyReportDTO dto) throws Exception {

        // A4固定尺寸：宽595，高842
        float pageW = 595f;
        float pageH = 842f;

        // 1) 顶部色条
        cs.setNonStrokingColor(35, 78, 120);
        cs.addRect(0, pageH - 57, pageW, 57);
        cs.fill();

        // 2) Logo（可选，有就画，没有就跳过）
        try (InputStream logoIs = PdfUtil.class.getResourceAsStream("/pdf/logo.png")) {
            if (logoIs != null) {
                byte[] bytes = readBytes(logoIs); // Java8读流
                PDImageXObject img = PDImageXObject.createFromByteArray(doc, bytes, "logo");
                cs.drawImage(img, 40, pageH - 52, 40, 40); // x,y,w,h
            }
        } catch (Exception ignore) {
            // logo失败不影响生成
        }

        // 3) 标题（白字）
        cs.setNonStrokingColor(255, 255, 255);
        drawText(cs, font, 18, "SPHAS 健康周报", 90, pageH - 30);
        drawText(cs, font, 11,
                "日期范围：" + safe(dto.getFrom()) + " ~ " + safe(dto.getTo()),
                90, pageH - 48);

        // 恢复黑色
        cs.setNonStrokingColor(0, 0, 0);
    }

    private static float drawTable(PDPageContentStream cs, PDType0Font font,
                                   float startX, float startY, float tableW,
                                   String[][] rows) throws Exception {

        float rowH = 24f;
        float col1W = tableW * 0.35f;
        float col2W = tableW * 0.65f;

        // 表头（第0行当表头）
        cs.setNonStrokingColor(230, 238, 247);     // 浅蓝灰
        cs.addRect(startX, startY - rowH, tableW, rowH);
        cs.fill();
        cs.setNonStrokingColor(0, 0, 0);

        // 外框
        cs.setLineWidth(0.7f);
        cs.addRect(startX, startY - rowH * rows.length, tableW, rowH * rows.length);
        cs.stroke();

        // 竖线
        cs.moveTo(startX + col1W, startY);
        cs.lineTo(startX + col1W, startY - rowH * rows.length);
        cs.stroke();

        float y = startY;

        for (int i = 0; i < rows.length; i++) {
            // 斑马纹（表头不算，从第1行开始隔行浅灰）
            if (i >= 1 && i % 2 == 0) {
                cs.setNonStrokingColor(245, 245, 245);
                cs.addRect(startX, y - rowH, tableW, rowH);
                cs.fill();
                cs.setNonStrokingColor(0, 0, 0);
            }

            // 横线
            cs.moveTo(startX, y - rowH);
            cs.lineTo(startX + tableW, y - rowH);
            cs.stroke();

            // 写字
            String c1 = rows[i][0];
            String c2 = rows[i][1];
            drawText(cs, font, 11, c1, startX + 10, y - 16);
            drawText(cs, font, 11, c2, startX + col1W + 10, y - 16);

            y -= rowH;
        }

        return y; // 返回表格结束后的 y，方便继续画下面内容
    }
    private static void drawFooter(PDPageContentStream cs, PDType0Font font, int pageNo) throws Exception {
        float pageW = 595f;

        String t = "生成时间：" + LocalDateTime.now().toString().replace("T", " ").substring(0, 16);
        drawText(cs, font, 9, t, 50, 30);                 // 左下角时间
        drawText(cs, font, 9, "Page " + pageNo, pageW - 110, 30); // 右下角页码
    }

    private static class PdfCtx {
        PDDocument doc;
        PDType0Font font;
        WeeklyReportDTO dto;

        PDPage page;
        PDPageContentStream cs;

        int pageNo = 0;
        float y; // 当前写到的y（从上往下）

        PdfCtx(PDDocument doc, PDType0Font font, WeeklyReportDTO dto) {
            this.doc = doc;
            this.font = font;
            this.dto = dto;
        }

        void newPage() throws Exception {
            // 关掉上一页，并画页脚
            if (cs != null) {
                drawFooter(cs, font, pageNo);
                cs.close();
            }

            pageNo++;
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);

            // 每页重复页眉
            drawHeader(doc, cs, font, dto);

            // 页眉下面开始写
            y = PAGE_H - MARGIN - HEADER_H;
        }

        void ensureSpace(float need) throws Exception {
            float minY = MARGIN + FOOTER_H; // 页脚上方留空
            if (y - need < minY) {
                newPage(); // 不够就新页
            }
        }

        void finish() throws Exception {
            // 最后一页：补页脚并关闭流
            if (cs != null) {
                drawFooter(cs, font, pageNo); // 画页脚
                cs.close();                   // 关闭流
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

            // 超宽就回退一个字符换行
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
            // getStringWidth单位是1/1000 text space
            return font.getStringWidth(text) / 1000f * fontSize;
        } catch (Exception e) {
            return text.length() * fontSize; // 兜底
        }
    }
    private static void drawParagraph(PdfCtx ctx, String text, int fontSize, float x, float maxWidth, float lineH)
            throws Exception {

        List<String> lines = wrapText(ctx.font, fontSize, text, maxWidth);

        // 需要的高度
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

        // 右侧内容换行
        List<String> rightLines = wrapText(ctx.font, fontSize, right, col2W - 20);
        int lines = Math.max(1, rightLines.size());
        float rowH = Math.max(24f, lines * lineH + 10);

        ctx.ensureSpace(rowH + 2);

        float topY = ctx.y;
        float bottomY = topY - rowH;

        // 斑马纹（可选）
        if (zebra) {
            ctx.cs.setNonStrokingColor(245, 245, 245);
            ctx.cs.addRect(x, bottomY, tableW, rowH);
            ctx.cs.fill();
            ctx.cs.setNonStrokingColor(0, 0, 0);
        }

        // 边框
        ctx.cs.setLineWidth(0.7f);
        ctx.cs.addRect(x, bottomY, tableW, rowH);
        ctx.cs.stroke();

        // 竖线
        ctx.cs.moveTo(x + col1W, bottomY);
        ctx.cs.lineTo(x + col1W, topY);
        ctx.cs.stroke();

        // 左列
        drawText(ctx.cs, ctx.font, fontSize, left, x + 10, topY - 16);

        // 右列多行
        float ty = topY - 16;
        if (rightLines.isEmpty()) rightLines.add("-");
        for (String line : rightLines) {
            drawText(ctx.cs, ctx.font, fontSize, line, x + col1W + 10, ty);
            ty -= lineH;
        }

        ctx.y -= rowH; // 移动游标
    }

    private static String safe(Object o) {
        return o == null ? "-" : String.valueOf(o);
    }
}

