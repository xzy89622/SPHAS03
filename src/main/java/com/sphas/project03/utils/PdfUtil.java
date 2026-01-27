package com.sphas.project03.utils;

import com.sphas.project03.controller.dto.WeeklyReportDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import java.io.InputStream; // 读取resources字体
import java.io.ByteArrayOutputStream;
import java.io.File;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject; // 画图片
import java.time.LocalDateTime; // 页脚时间
import java.io.InputStream;              // 读资源
import java.io.ByteArrayOutputStream;    // 转 byte[]

/**
 * PDF工具：生成周报PDF（支持中文）
 */
public class PdfUtil {

    public static byte[] buildWeeklyReport(WeeklyReportDTO dto) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            // 1) 从resources加载字体（你已有）
            InputStream is = PdfUtil.class.getResourceAsStream("/fonts/AlibabaPuHuiTi-2-55-Regular.ttf");
            if (is == null) throw new RuntimeException("字体文件未找到");
            PDType0Font font = PDType0Font.load(doc, is);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // 页眉
                drawHeader(doc, cs, font, dto);

                float x = 50f;
                float y = 760f;  // header下面开始画
                float w = 595f - 100f;

                // 小标题
                drawText(cs, font, 12, "本周核心数据", x, y);
                y -= 14;

                // 表格数据（第一行当表头）
                String[][] rows = new String[][]{
                        {"指标", "值"},
                        {"记录天数", safe(dto.getDays()) + " 天"},
                        {"平均体重(kg)", safe(dto.getAvgWeight())},
                        {"平均步数", safe(dto.getAvgSteps())},
                        {"平均睡眠(h)", safe(dto.getAvgSleepHours())},
                        {"体重趋势", safe(dto.getWeightTrend())},
                        {"血压风险", Boolean.TRUE.equals(dto.getBpRisk()) ? "有" : "无"},
                        {"总结", safe(dto.getSummary())}
                };

                y = drawTable(cs, font, x, y, w, rows);
                y -= 18;

                // 建议列表（简单写法：一行一条，后面我们再升级自动换行/分页）
                drawText(cs, font, 12, "建议", x, y);
                y -= 18;

                if (dto.getSuggestions() != null) {
                    for (String s : dto.getSuggestions()) {
                        drawText(cs, font, 11, "• " + s, x + 10, y);
                        y -= 16;
                    }
                }

                // 页脚
                drawFooter(cs, font, 1);
            }

            doc.save(out);
            return out.toByteArray();

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

        // 顶部色条
        cs.setNonStrokingColor(35, 78, 120);     // 深蓝（你也可以换）
        cs.addRect(0, 785, 595, 57);             // A4宽约595
        cs.fill();

        // Logo（如果存在就画）
        try (InputStream logoIs = PdfUtil.class.getResourceAsStream("/pdf/logo.png")) {
            if (logoIs != null) {
                byte[] logoBytes = readBytes(logoIs); // Java8读流
                PDImageXObject img = PDImageXObject.createFromByteArray(doc, logoBytes, "logo");
                cs.drawImage(img, 40, 795, 40, 40);   // x,y,w,h
            }
        } catch (Exception ignore) {
            // logo加载失败不影响主流程
        }


        // 标题
        cs.setNonStrokingColor(255, 255, 255);  // 白字
        drawText(cs, font, 18, "SPHAS 健康周报", 90, 812);

        // 副标题（日期）
        drawText(cs, font, 11,
                "日期范围：" + safe(dto.getFrom()) + " ~ " + safe(dto.getTo()),
                90, 795);

        // 恢复默认颜色（黑色）
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
        String t = "生成时间：" + LocalDateTime.now().toString().replace("T", " ").substring(0, 16);
        drawText(cs, font, 9, t, 50, 30);                 // 左下角时间
        drawText(cs, font, 9, "Page " + pageNo, 520, 30); // 右下角页码
    }

    private static String safe(Object o) {
        return o == null ? "-" : String.valueOf(o);
    }
}

