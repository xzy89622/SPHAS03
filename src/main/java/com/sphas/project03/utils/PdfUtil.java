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

/**
 * PDF工具：生成周报PDF（支持中文）
 */
public class PdfUtil {

    public static byte[] buildWeeklyReport(WeeklyReportDTO dto) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            // ✅ 1) 加载中文字体（Windows 常见字体路径）
            // 优先微软雅黑，其次宋体（你电脑大概率都有）
            // 1) 从resources加载字体（不依赖电脑字体）
            InputStream is = PdfUtil.class.getResourceAsStream(
                    "/fonts/AlibabaPuHuiTi-2-55-Regular.ttf"
            );
            if (is == null) {
                throw new RuntimeException("字体文件未找到：/fonts/AlibabaPuHuiTi-2-55-Regular.ttf");
            }
            PDType0Font font = PDType0Font.load(doc, is);


            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                cs.beginText();                 // ✅ beginText 一定配对 endText
                cs.newLineAtOffset(50, 780);

                // 标题
                cs.setFont(font, 16);
                cs.showText("SPHAS 健康周报");
                cs.newLineAtOffset(0, -24);

                // 正文
                cs.setFont(font, 12);
                cs.showText("日期范围：" + safe(dto.getFrom()) + " ~ " + safe(dto.getTo()));
                cs.newLineAtOffset(0, -18);

                cs.showText("记录天数：" + safe(dto.getDays()));
                cs.newLineAtOffset(0, -18);

                cs.showText("平均体重(kg)：" + safe(dto.getAvgWeight()));
                cs.newLineAtOffset(0, -18);

                cs.showText("平均步数：" + safe(dto.getAvgSteps()));
                cs.newLineAtOffset(0, -18);

                cs.showText("平均睡眠(h)：" + safe(dto.getAvgSleepHours()));
                cs.newLineAtOffset(0, -18);

                cs.showText("体重趋势：" + safe(dto.getWeightTrend()));
                cs.newLineAtOffset(0, -18);

                cs.showText("血压风险：" + (Boolean.TRUE.equals(dto.getBpRisk()) ? "有" : "无"));
                cs.newLineAtOffset(0, -22);

                cs.showText("总结：" + safe(dto.getSummary()));
                cs.newLineAtOffset(0, -22);

                cs.showText("建议：");
                cs.newLineAtOffset(0, -18);

                if (dto.getSuggestions() != null) {
                    for (String s : dto.getSuggestions()) {
                        cs.showText(" - " + s);
                        cs.newLineAtOffset(0, -16);
                    }
                }

                cs.endText();                   // ✅ endText 必须调用
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

    private static String safe(Object o) {
        return o == null ? "-" : String.valueOf(o);
    }
}

