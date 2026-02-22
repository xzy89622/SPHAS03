package com.sphas.project03.serviceImpl;

import com.sphas.project03.controller.dto.WeeklyReportDTO;
import com.sphas.project03.controller.dto.MonthlyReportDTO;
import com.sphas.project03.entity.HealthRecord;
import com.sphas.project03.service.HealthRecordService;
import com.sphas.project03.service.HealthReportService;
import com.sphas.project03.utils.PdfUtil;
import org.springframework.stereotype.Service;
import com.sphas.project03.common.HealthConstants;

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
        // 最近7天（含今天）
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(6);
        List<HealthRecord> list = healthRecordService.listByDateRange(userId, from.toString(), to.toString());

        WeeklyReportDTO dto = new WeeklyReportDTO();
        fillCommon(dto, list, from, to);
        return dto;
    }

    @Override
    public byte[] weeklyPdf(Long userId) {
        WeeklyReportDTO dto = weekly(userId);
        return PdfUtil.buildWeeklyReport(dto); // 生成PDF字节
    }

    @Override
    public MonthlyReportDTO monthly(Long userId) {
        // 最近30天（含今天）
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(29);
        List<HealthRecord> list = healthRecordService.listByDateRange(userId, from.toString(), to.toString());

        MonthlyReportDTO dto = new MonthlyReportDTO();
        fillCommon(dto, list, from, to);
        return dto;
    }

    @Override
    public byte[] monthlyPdf(Long userId) {
        MonthlyReportDTO dto = monthly(userId);
        return PdfUtil.buildMonthlyReport(dto);
    }

    /**
     * 公共填充逻辑：周报/月报共用
     * 说明：为了减少重复代码，字段保持一致。
     */
    private void fillCommon(Object dto, List<HealthRecord> list, LocalDate from, LocalDate to) {

        // 统计平均值
        Double avgWeight = avgWeight(list);
        Integer avgSteps = avgSteps(list);
        Double avgSleep = avgSleep(list);
        String trend = weightTrend(list);
        Boolean bpRisk = hasBpRisk(list);

        if (dto instanceof WeeklyReportDTO) {
            WeeklyReportDTO d = (WeeklyReportDTO) dto;
            d.setFrom(from.toString());
            d.setTo(to.toString());
            d.setDays(list.size());
            d.setAvgWeight(avgWeight);
            d.setAvgSteps(avgSteps);
            d.setAvgSleepHours(avgSleep);
            d.setWeightTrend(trend);
            d.setBpRisk(bpRisk);
            d.setSuggestions(buildSuggestions(d));
            d.setSummary(buildSummary(d));
            return;
        }

        if (dto instanceof MonthlyReportDTO) {
            MonthlyReportDTO d = (MonthlyReportDTO) dto;
            d.setFrom(from.toString());
            d.setTo(to.toString());
            d.setDays(list.size());
            d.setAvgWeight(avgWeight);
            d.setAvgSteps(avgSteps);
            d.setAvgSleepHours(avgSleep);
            d.setWeightTrend(trend);
            d.setBpRisk(bpRisk);
            d.setSuggestions(buildSuggestionsForMonth(d));
            d.setSummary(buildSummaryForMonth(d));
        }
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
        if (list == null || list.size() < 2) return "数据不足";

        // 假设 list 是按时间倒序排列（最新在0），若你的查询排序不同要调一下
        HealthRecord latest = list.get(0);
        HealthRecord oldest = list.get(list.size() - 1);

        if (latest.getWeightKg() == null || oldest.getWeightKg() == null) return "数据不足";

        double diff = latest.getWeightKg() - oldest.getWeightKg();

        // 0.5kg 缓冲，避免误判
        if (Math.abs(diff) < 0.5) {
            return "稳定";
        }
        return diff > 0 ? "上升" : "下降";
    }

    private boolean hasBpRisk(List<HealthRecord> list) {
        if (list == null || list.isEmpty()) return false;

        for (HealthRecord r : list) {
            if (r.getSystolic() != null && r.getDiastolic() != null) {
                // 用统一阈值判断
                if (r.getSystolic() >= HealthConstants.BP_SYS_MID ||
                        r.getDiastolic() >= HealthConstants.BP_DIA_MID) {
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

    // 月报建议：阈值稍微放宽一点（30天平均）
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

    private String buildSummaryForMonth(MonthlyReportDTO dto) {
        if (dto.getDays() == null || dto.getDays() == 0) {
            return "本月暂无记录，建议坚持每日记录";
        }
        if (Boolean.TRUE.equals(dto.getBpRisk())) return "本月血压有偏高风险";
        if ("上升".equals(dto.getWeightTrend())) return "本月体重略有上升";
        return "本月整体状态良好";
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