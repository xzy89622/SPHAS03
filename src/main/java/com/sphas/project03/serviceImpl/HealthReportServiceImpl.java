package com.sphas.project03.serviceImpl;

import com.sphas.project03.controller.dto.WeeklyReportDTO;
import com.sphas.project03.entity.HealthRecord;
import com.sphas.project03.service.HealthRecordService;
import com.sphas.project03.service.HealthReportService;
import com.sphas.project03.utils.PdfUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 周报业务：统计最近7天
 */
@Service
public class HealthReportServiceImpl implements HealthReportService {

    private final HealthRecordService healthRecordService;

    public HealthReportServiceImpl(HealthRecordService healthRecordService) {
        this.healthRecordService = healthRecordService;
    }

    @Override
    public WeeklyReportDTO weekly(Long userId) {

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(6);

        List<HealthRecord> list = healthRecordService.listByDateRange(
                userId, from.toString(), to.toString()
        );

        WeeklyReportDTO dto = new WeeklyReportDTO();
        dto.setFrom(from.toString());
        dto.setTo(to.toString());
        dto.setDays(list.size());

        // 统计平均值
        dto.setAvgWeight(avgWeight(list));
        dto.setAvgSteps(avgSteps(list));
        dto.setAvgSleepHours(avgSleep(list));

        // 趋势判断（用第一天 vs 最后一天）
        dto.setWeightTrend(weightTrend(list));

        // 血压风险：任意一天偏高就算风险
        dto.setBpRisk(hasBpRisk(list));

        // 建议与总结
        dto.setSuggestions(buildSuggestions(dto));
        dto.setSummary(buildSummary(dto));

        return dto;
    }

    @Override
    public byte[] weeklyPdf(Long userId) {
        WeeklyReportDTO dto = weekly(userId);
        return PdfUtil.buildWeeklyReport(dto); // 生成PDF字节
    }

    private Double avgWeight(List<HealthRecord> list) {
        double sum = 0;
        int cnt = 0;
        for (HealthRecord r : list) {
            if (r.getWeightKg() != null) {
                sum += r.getWeightKg();
                cnt++;
            }
        }
        return cnt == 0 ? null : round(sum / cnt);
    }

    private Integer avgSteps(List<HealthRecord> list) {
        long sum = 0;
        int cnt = 0;
        for (HealthRecord r : list) {
            if (r.getSteps() != null) {
                sum += r.getSteps();
                cnt++;
            }
        }
        return cnt == 0 ? null : (int) (sum / cnt);
    }

    private Double avgSleep(List<HealthRecord> list) {
        double sum = 0;
        int cnt = 0;
        for (HealthRecord r : list) {
            if (r.getSleepHours() != null) {
                sum += r.getSleepHours();
                cnt++;
            }
        }
        return cnt == 0 ? null : round(sum / cnt);
    }

    private String weightTrend(List<HealthRecord> list) {
        if (list.size() < 2) return "数据不足";

        HealthRecord first = list.get(0);
        HealthRecord last = list.get(list.size() - 1);
        if (first.getWeightKg() == null || last.getWeightKg() == null) return "数据不足";

        double diff = last.getWeightKg() - first.getWeightKg();
        if (Math.abs(diff) < 0.5) return "稳定";
        return diff > 0 ? "上升" : "下降";
    }

    private boolean hasBpRisk(List<HealthRecord> list) {
        for (HealthRecord r : list) {
            if (r.getSystolic() != null && r.getDiastolic() != null) {
                if (r.getSystolic() >= 130 || r.getDiastolic() >= 85) {
                    return true; // 简单判定偏高
                }
            }
        }
        return false;
    }

    private List<String> buildSuggestions(WeeklyReportDTO dto) {
        List<String> tips = new ArrayList<>();

        if (dto.getAvgSteps() != null && dto.getAvgSteps() < 8000) {
            tips.add("本周平均步数偏低，建议每天至少 8000 步");
        }
        if (dto.getAvgSleepHours() != null && dto.getAvgSleepHours() < 7) {
            tips.add("本周睡眠不足，建议保证 7 小时以上");
        }
        if (Boolean.TRUE.equals(dto.getBpRisk())) {
            tips.add("存在血压偏高情况，建议清淡饮食、减少熬夜并关注复测");
        }
        if ("上升".equals(dto.getWeightTrend())) {
            tips.add("体重有上升趋势，建议适当控制饮食并增加运动");
        }
        if (tips.isEmpty()) {
            tips.add("整体状态良好，继续保持当前习惯");
        }
        return tips;
    }

    private String buildSummary(WeeklyReportDTO dto) {
        if (dto.getDays() == null || dto.getDays() == 0) {
            return "本周暂无记录，建议坚持每日记录";
        }
        if (Boolean.TRUE.equals(dto.getBpRisk())) return "本周血压有偏高风险";
        if ("上升".equals(dto.getWeightTrend())) return "本周体重略有上升";
        return "本周整体状态良好";
    }

    private double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
