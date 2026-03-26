package com.sphas.project03.serviceImpl;

import com.sphas.project03.common.HealthConstants;
import com.sphas.project03.controller.dto.MonthlyReportDTO;
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
 * 健康报告业务
 * 这里统一生成当前周报和当前月报
 */
@Service
public class HealthReportServiceImpl implements HealthReportService {

    private final HealthRecordService healthRecordService;

    public HealthReportServiceImpl(HealthRecordService healthRecordService) {
        this.healthRecordService = healthRecordService;
    }

    @Override
    public WeeklyReportDTO weekly(Long userId) {
        // 最近7天（含今天）
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(6);
        List<HealthRecord> list = healthRecordService.listByDateRange(userId, from.toString(), to.toString());

        WeeklyReportDTO dto = new WeeklyReportDTO();
        fillWeekly(dto, list, from, to);
        return dto;
    }

    @Override
    public byte[] weeklyPdf(Long userId) {
        WeeklyReportDTO dto = weekly(userId);
        return PdfUtil.buildWeeklyReport(dto);
    }

    @Override
    public MonthlyReportDTO monthly(Long userId) {
        // 最近30天（含今天）
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(29);
        List<HealthRecord> list = healthRecordService.listByDateRange(userId, from.toString(), to.toString());

        MonthlyReportDTO dto = new MonthlyReportDTO();
        fillMonthly(dto, list, from, to);
        return dto;
    }

    @Override
    public byte[] monthlyPdf(Long userId) {
        MonthlyReportDTO dto = monthly(userId);
        return PdfUtil.buildMonthlyReport(dto);
    }

    /**
     * 填充周报
     */
    private void fillWeekly(WeeklyReportDTO dto, List<HealthRecord> list, LocalDate from, LocalDate to) {
        dto.setFrom(from.toString());
        dto.setTo(to.toString());
        dto.setDays(list == null ? 0 : list.size());
        dto.setAvgWeight(avgWeight(list));
        dto.setAvgSteps(avgSteps(list));
        dto.setAvgSleepHours(avgSleep(list));
        dto.setWeightTrend(weightTrend(list));
        dto.setBpRisk(hasBpRisk(list));
        dto.setSuggestions(buildSuggestions(dto));
        dto.setSummary(buildSummary(dto));
    }

    /**
     * 填充月报
     */
    private void fillMonthly(MonthlyReportDTO dto, List<HealthRecord> list, LocalDate from, LocalDate to) {
        dto.setFrom(from.toString());
        dto.setTo(to.toString());
        dto.setDays(list == null ? 0 : list.size());
        dto.setAvgWeight(avgWeight(list));
        dto.setAvgSteps(avgSteps(list));
        dto.setAvgSleepHours(avgSleep(list));
        dto.setWeightTrend(weightTrend(list));
        dto.setBpRisk(hasBpRisk(list));
        dto.setSuggestions(buildSuggestionsForMonth(dto));
        dto.setSummary(buildSummaryForMonth(dto));
    }

    private Double avgWeight(List<HealthRecord> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        double sum = 0;
        int cnt = 0;
        for (HealthRecord r : list) {
            if (r != null && r.getWeightKg() != null) {
                sum += r.getWeightKg();
                cnt++;
            }
        }
        return cnt == 0 ? null : round(sum / cnt);
    }

    private Integer avgSteps(List<HealthRecord> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        long sum = 0;
        int cnt = 0;
        for (HealthRecord r : list) {
            if (r != null && r.getSteps() != null) {
                sum += r.getSteps();
                cnt++;
            }
        }
        return cnt == 0 ? null : (int) (sum / cnt);
    }

    private Double avgSleep(List<HealthRecord> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        double sum = 0;
        int cnt = 0;
        for (HealthRecord r : list) {
            if (r != null && r.getSleepHours() != null) {
                sum += r.getSleepHours();
                cnt++;
            }
        }
        return cnt == 0 ? null : round(sum / cnt);
    }

    /**
     * 体重趋势
     * 注意：
     * listByDateRange 现在是按 record_date 升序查的
     * 所以第一个是最早，最后一个才是最新
     */
    private String weightTrend(List<HealthRecord> list) {
        if (list == null || list.size() < 2) {
            return "数据不足";
        }

        HealthRecord earliest = firstRecordWithWeight(list);
        HealthRecord latest = lastRecordWithWeight(list);

        if (earliest == null || latest == null) {
            return "数据不足";
        }

        double diff = latest.getWeightKg() - earliest.getWeightKg();

        // 留一点缓冲，避免小波动被误判
        if (Math.abs(diff) < 0.5) {
            return "稳定";
        }
        return diff > 0 ? "上升" : "下降";
    }

    private HealthRecord firstRecordWithWeight(List<HealthRecord> list) {
        for (HealthRecord r : list) {
            if (r != null && r.getWeightKg() != null) {
                return r;
            }
        }
        return null;
    }

    private HealthRecord lastRecordWithWeight(List<HealthRecord> list) {
        for (int i = list.size() - 1; i >= 0; i--) {
            HealthRecord r = list.get(i);
            if (r != null && r.getWeightKg() != null) {
                return r;
            }
        }
        return null;
    }

    private boolean hasBpRisk(List<HealthRecord> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }

        for (HealthRecord r : list) {
            if (r != null && r.getSystolic() != null && r.getDiastolic() != null) {
                if (r.getSystolic() >= HealthConstants.BP_SYS_MID
                        || r.getDiastolic() >= HealthConstants.BP_DIA_MID) {
                    return true;
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

    private List<String> buildSuggestionsForMonth(MonthlyReportDTO dto) {
        List<String> tips = new ArrayList<>();

        if (dto.getAvgSteps() != null && dto.getAvgSteps() < 7000) {
            tips.add("本月平均步数偏低，建议逐步提升到 7000-9000 步/天");
        }
        if (dto.getAvgSleepHours() != null && dto.getAvgSleepHours() < 7) {
            tips.add("本月睡眠不足，建议保证 7 小时以上，并尽量固定作息");
        }
        if (Boolean.TRUE.equals(dto.getBpRisk())) {
            tips.add("本月存在血压偏高情况，建议清淡饮食、减少熬夜并关注复测");
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
        if (Boolean.TRUE.equals(dto.getBpRisk())) {
            return "本周血压有偏高风险";
        }
        if ("上升".equals(dto.getWeightTrend())) {
            return "本周体重略有上升";
        }
        if ("下降".equals(dto.getWeightTrend())) {
            return "本周体重整体有下降趋势";
        }
        return "本周整体状态良好";
    }

    private String buildSummaryForMonth(MonthlyReportDTO dto) {
        if (dto.getDays() == null || dto.getDays() == 0) {
            return "本月暂无记录，建议坚持每日记录";
        }
        if (Boolean.TRUE.equals(dto.getBpRisk())) {
            return "本月血压有偏高风险";
        }
        if ("上升".equals(dto.getWeightTrend())) {
            return "本月体重略有上升";
        }
        if ("下降".equals(dto.getWeightTrend())) {
            return "本月体重整体有下降趋势";
        }
        return "本月整体状态良好";
    }

    private double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}