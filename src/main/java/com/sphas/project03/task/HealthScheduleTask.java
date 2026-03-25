package com.sphas.project03.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sphas.project03.common.HealthConstants;
import com.sphas.project03.controller.dto.MonthlyReportDTO;
import com.sphas.project03.controller.dto.WeeklyReportDTO;
import com.sphas.project03.entity.HealthRecord;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.entity.WeeklyReportRecord;
import com.sphas.project03.mapper.HealthRecordMapper;
import com.sphas.project03.mapper.SysUserMapper;
import com.sphas.project03.mapper.WeeklyReportMapper;
import com.sphas.project03.service.HealthReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 健康报告定时任务
 * 这版做三件事：
 * 1. 自动生成系统汇总周报 / 月报
 * 2. 自动生成每个普通用户自己的周报 / 月报历史
 * 3. 和手动查看报告保持同一套历史落库结构
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthScheduleTask {

    private final SysUserMapper sysUserMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final WeeklyReportMapper weeklyReportMapper;
    private final HealthReportService healthReportService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 每周一早上 7 点生成上一周周报
     */
    @Scheduled(cron = "0 0 7 * * MON")
    public void generateWeeklyReportTask() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.minusDays(1);
        LocalDate startDate = endDate.with(DayOfWeek.MONDAY);

        saveSystemWeeklyReport(startDate, endDate);
        saveAllUserWeeklyReports(startDate, endDate);

        log.info("周报自动生成完成：{} ~ {}", startDate, endDate);
    }

    /**
     * 每月 1 号早上 7 点半生成上个月月报
     */
    @Scheduled(cron = "0 30 7 1 * ?")
    public void generateMonthlyReportTask() {
        LocalDate firstDayOfThisMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = firstDayOfThisMonth.minusDays(1);
        LocalDate startDate = endDate.withDayOfMonth(1);

        saveSystemMonthlyReport(startDate, endDate);
        saveAllUserMonthlyReports(startDate, endDate);

        log.info("月报自动生成完成：{} ~ {}", startDate, endDate);
    }

    /**
     * 系统周报
     */
    private void saveSystemWeeklyReport(LocalDate startDate, LocalDate endDate) {
        List<HealthRecord> list = healthRecordMapper.selectList(
                new LambdaQueryWrapper<HealthRecord>()
                        .ge(HealthRecord::getRecordDate, startDate)
                        .le(HealthRecord::getRecordDate, endDate)
                        .orderByAsc(HealthRecord::getRecordDate)
        );

        Map<String, Object> metrics = buildSystemMetrics("WEEK", list, startDate, endDate);
        Map<String, Object> detail = buildSystemDetail("WEEK", metrics);

        saveReportRecord(
                0L,
                "WEEK",
                startDate,
                endDate,
                "[WEEK] " + startDate + " ~ " + endDate + " 周健康总结报告",
                String.valueOf(detail.getOrDefault("summary", "")),
                metrics,
                detail
        );
    }

    /**
     * 系统月报
     */
    private void saveSystemMonthlyReport(LocalDate startDate, LocalDate endDate) {
        List<HealthRecord> list = healthRecordMapper.selectList(
                new LambdaQueryWrapper<HealthRecord>()
                        .ge(HealthRecord::getRecordDate, startDate)
                        .le(HealthRecord::getRecordDate, endDate)
                        .orderByAsc(HealthRecord::getRecordDate)
        );

        Map<String, Object> metrics = buildSystemMetrics("MONTH", list, startDate, endDate);
        Map<String, Object> detail = buildSystemDetail("MONTH", metrics);

        saveReportRecord(
                0L,
                "MONTH",
                startDate,
                endDate,
                "[MONTH] " + startDate + " ~ " + endDate + " 月健康总结报告",
                String.valueOf(detail.getOrDefault("summary", "")),
                metrics,
                detail
        );
    }

    /**
     * 所有用户周报
     */
    private void saveAllUserWeeklyReports(LocalDate startDate, LocalDate endDate) {
        List<SysUser> userList = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, "USER")
                        .eq(SysUser::getStatus, 1)
        );

        for (SysUser user : userList) {
            try {
                WeeklyReportDTO dto = healthReportService.weekly(user.getId());
                if (dto == null || dto.getFrom() == null || dto.getTo() == null) {
                    continue;
                }

                Map<String, Object> metrics = buildWeeklyMetrics(dto);
                Map<String, Object> detail = buildWeeklyDetail(dto);

                saveReportRecord(
                        user.getId(),
                        "WEEK",
                        LocalDate.parse(dto.getFrom()),
                        LocalDate.parse(dto.getTo()),
                        "[WEEK] " + dto.getFrom() + " ~ " + dto.getTo() + " 我的周健康报告",
                        dto.getSummary(),
                        metrics,
                        detail
                );
            } catch (Exception e) {
                log.error("生成用户周报失败，userId={}", user.getId(), e);
            }
        }
    }

    /**
     * 所有用户月报
     */
    private void saveAllUserMonthlyReports(LocalDate startDate, LocalDate endDate) {
        List<SysUser> userList = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, "USER")
                        .eq(SysUser::getStatus, 1)
        );

        for (SysUser user : userList) {
            try {
                MonthlyReportDTO dto = healthReportService.monthly(user.getId());
                if (dto == null || dto.getFrom() == null || dto.getTo() == null) {
                    continue;
                }

                Map<String, Object> metrics = buildMonthlyMetrics(dto);
                Map<String, Object> detail = buildMonthlyDetail(dto);

                saveReportRecord(
                        user.getId(),
                        "MONTH",
                        LocalDate.parse(dto.getFrom()),
                        LocalDate.parse(dto.getTo()),
                        "[MONTH] " + dto.getFrom() + " ~ " + dto.getTo() + " 我的月健康报告",
                        dto.getSummary(),
                        metrics,
                        detail
                );
            } catch (Exception e) {
                log.error("生成用户月报失败，userId={}", user.getId(), e);
            }
        }
    }

    /**
     * 统一保存历史记录
     */
    private void saveReportRecord(Long userId,
                                  String reportType,
                                  LocalDate startDate,
                                  LocalDate endDate,
                                  String title,
                                  String summary,
                                  Map<String, Object> metrics,
                                  Map<String, Object> detail) {
        WeeklyReportRecord db = weeklyReportMapper.selectOne(
                new LambdaQueryWrapper<WeeklyReportRecord>()
                        .eq(WeeklyReportRecord::getUserId, userId)
                        .eq(WeeklyReportRecord::getReportType, reportType)
                        .eq(WeeklyReportRecord::getWeekStart, startDate)
                        .eq(WeeklyReportRecord::getWeekEnd, endDate)
                        .last("limit 1")
        );

        LocalDateTime now = LocalDateTime.now();
        if (db == null) {
            db = new WeeklyReportRecord();
            db.setCreatedAt(now);
        }

        db.setUserId(userId);
        db.setReportType(reportType);
        db.setWeekStart(startDate);
        db.setWeekEnd(endDate);
        db.setTitle(title);
        db.setSummary(summary);
        db.setMetricsJson(writeJson(metrics));
        db.setTableJson(writeJson(detail));
        db.setUpdatedAt(now);

        if (db.getId() == null) {
            weeklyReportMapper.insert(db);
        } else {
            weeklyReportMapper.updateById(db);
        }
    }

    /**
     * 系统汇总指标
     */
    private Map<String, Object> buildSystemMetrics(String reportType,
                                                   List<HealthRecord> list,
                                                   LocalDate startDate,
                                                   LocalDate endDate) {
        Map<String, Object> metrics = new HashMap<>();

        Set<Long> userIds = new HashSet<>();
        for (HealthRecord item : list) {
            if (item.getUserId() != null) {
                userIds.add(item.getUserId());
            }
        }

        metrics.put("reportType", reportType);
        metrics.put("scope", "SYSTEM");
        metrics.put("from", startDate.toString());
        metrics.put("to", endDate.toString());
        metrics.put("recordCount", list.size());
        metrics.put("userCount", userIds.size());
        metrics.put("avgWeight", avgWeight(list));
        metrics.put("avgSteps", avgSteps(list));
        metrics.put("avgSleepHours", avgSleepHours(list));
        metrics.put("weightTrend", weightTrend(list));
        metrics.put("bpRiskCount", bpRiskCount(list));

        return metrics;
    }

    /**
     * 系统汇总详情
     */
    private Map<String, Object> buildSystemDetail(String reportType, Map<String, Object> metrics) {
        Map<String, Object> detail = new HashMap<>();
        List<String> riskTips = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        Integer recordCount = toInt(metrics.get("recordCount"));
        Integer userCount = toInt(metrics.get("userCount"));
        Double avgWeight = toDouble(metrics.get("avgWeight"));
        Integer avgSteps = toInt(metrics.get("avgSteps"));
        Double avgSleepHours = toDouble(metrics.get("avgSleepHours"));
        String weightTrend = String.valueOf(metrics.getOrDefault("weightTrend", "数据不足"));
        Integer bpRiskCount = toInt(metrics.get("bpRiskCount"));

        if (recordCount == 0) {
            riskTips.add("当前周期暂无健康记录，系统报告参考性较低。");
            suggestions.add("建议先提醒用户坚持录入健康数据。");
        }

        if (bpRiskCount > 0) {
            riskTips.add("本周期存在 " + bpRiskCount + " 条血压偏高记录，需要重点关注。");
            suggestions.add("建议加强异常用户复测提醒与饮食干预。");
        }

        int stepLine = "WEEK".equals(reportType) ? 8000 : 7000;
        if (avgSteps > 0 && avgSteps < stepLine) {
            riskTips.add("整体活动量偏低，平均步数未达到推荐水平。");
            suggestions.add("建议把日均步数逐步提升到 " + stepLine + " 步以上。");
        }

        if (avgSleepHours != null && avgSleepHours > 0 && avgSleepHours < 7) {
            riskTips.add("整体睡眠时长偏短，存在作息不规律风险。");
            suggestions.add("建议尽量在 23 点前入睡，保证 7 小时以上睡眠。");
        }

        if ("上升".equals(weightTrend)) {
            riskTips.add("体重呈上升趋势，需要关注饮食控制与运动执行。");
            suggestions.add("建议结合饮食计划和运动计划持续干预。");
        }

        if (avgWeight != null && avgWeight > 85) {
            riskTips.add("样本用户平均体重偏高，肥胖相关风险需要持续关注。");
            suggestions.add("建议增加有氧运动和饮食热量控制。");
        }

        if (riskTips.isEmpty()) {
            riskTips.add("本周期整体健康趋势平稳，暂未发现明显集中风险。");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("建议继续保持规律记录，按周期观察趋势变化。");
        }

        String summary = "本周期共统计 " + recordCount + " 条记录，覆盖 "
                + userCount + " 位用户，平均体重 " + formatDouble(avgWeight)
                + "kg，平均步数 " + avgSteps
                + " 步，平均睡眠 " + formatDouble(avgSleepHours)
                + " 小时，体重趋势为 " + weightTrend + "。";

        detail.put("summary", summary);
        detail.put("riskTips", riskTips);
        detail.put("suggestions", suggestions);

        return detail;
    }

    private Map<String, Object> buildWeeklyMetrics(WeeklyReportDTO dto) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("reportType", "WEEK");
        metrics.put("scope", "USER");
        metrics.put("from", dto.getFrom());
        metrics.put("to", dto.getTo());
        metrics.put("recordCount", dto.getDays());
        metrics.put("avgWeight", dto.getAvgWeight());
        metrics.put("avgSteps", dto.getAvgSteps());
        metrics.put("avgSleepHours", dto.getAvgSleepHours());
        metrics.put("weightTrend", dto.getWeightTrend());
        metrics.put("bpRiskCount", Boolean.TRUE.equals(dto.getBpRisk()) ? 1 : 0);
        metrics.put("riskLevel", calcRiskLevel(dto.getBpRisk(), dto.getAvgSteps(), dto.getAvgSleepHours(), dto.getWeightTrend()));
        return metrics;
    }

    private Map<String, Object> buildMonthlyMetrics(MonthlyReportDTO dto) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("reportType", "MONTH");
        metrics.put("scope", "USER");
        metrics.put("from", dto.getFrom());
        metrics.put("to", dto.getTo());
        metrics.put("recordCount", dto.getDays());
        metrics.put("avgWeight", dto.getAvgWeight());
        metrics.put("avgSteps", dto.getAvgSteps());
        metrics.put("avgSleepHours", dto.getAvgSleepHours());
        metrics.put("weightTrend", dto.getWeightTrend());
        metrics.put("bpRiskCount", Boolean.TRUE.equals(dto.getBpRisk()) ? 1 : 0);
        metrics.put("riskLevel", calcRiskLevel(dto.getBpRisk(), dto.getAvgSteps(), dto.getAvgSleepHours(), dto.getWeightTrend()));
        return metrics;
    }

    private Map<String, Object> buildWeeklyDetail(WeeklyReportDTO dto) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("summary", dto.getSummary());
        detail.put("riskTips", buildRiskTips(dto.getBpRisk(), dto.getWeightTrend(), dto.getAvgSleepHours(), dto.getAvgSteps()));
        detail.put("suggestions", dto.getSuggestions() == null ? new ArrayList<>() : dto.getSuggestions());
        return detail;
    }

    private Map<String, Object> buildMonthlyDetail(MonthlyReportDTO dto) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("summary", dto.getSummary());
        detail.put("riskTips", buildRiskTips(dto.getBpRisk(), dto.getWeightTrend(), dto.getAvgSleepHours(), dto.getAvgSteps()));
        detail.put("suggestions", dto.getSuggestions() == null ? new ArrayList<>() : dto.getSuggestions());
        return detail;
    }

    private List<String> buildRiskTips(Boolean bpRisk, String weightTrend, Double avgSleepHours, Integer avgSteps) {
        ArrayList<String> list = new ArrayList<>();

        if (Boolean.TRUE.equals(bpRisk)) {
            list.add("存在血压偏高风险，需要重点关注。");
        }
        if ("上升".equals(weightTrend)) {
            list.add("体重呈上升趋势，需要关注饮食控制与运动执行。");
        }
        if (avgSleepHours != null && avgSleepHours < 7) {
            list.add("睡眠时长偏短，存在作息不规律风险。");
        }
        if (avgSteps != null && avgSteps > 0 && avgSteps < 7000) {
            list.add("活动量偏低，平均步数未达到推荐水平。");
        }

        if (list.isEmpty()) {
            list.add("本周期整体健康趋势平稳，暂未发现明显风险。");
        }

        return list;
    }

    private String calcRiskLevel(Boolean bpRisk, Integer avgSteps, Double avgSleepHours, String weightTrend) {
        int score = 0;

        if (Boolean.TRUE.equals(bpRisk)) {
            score += 2;
        }
        if (avgSteps != null && avgSteps > 0) {
            if (avgSteps < 4000) {
                score += 2;
            } else if (avgSteps < 7000) {
                score += 1;
            }
        }
        if (avgSleepHours != null) {
            if (avgSleepHours < 6) {
                score += 2;
            } else if (avgSleepHours < 7) {
                score += 1;
            }
        }
        if ("上升".equals(weightTrend)) {
            score += 1;
        }

        if (score >= 4) {
            return "HIGH";
        }
        if (score >= 2) {
            return "MID";
        }
        return "LOW";
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
        return cnt == 0 ? null : round2(sum / cnt);
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
        return cnt == 0 ? 0 : (int) Math.round(sum * 1.0 / cnt);
    }

    private Double avgSleepHours(List<HealthRecord> list) {
        double sum = 0;
        int cnt = 0;
        for (HealthRecord r : list) {
            if (r.getSleepHours() != null) {
                sum += r.getSleepHours();
                cnt++;
            }
        }
        return cnt == 0 ? null : round2(sum / cnt);
    }

    private String weightTrend(List<HealthRecord> list) {
        List<HealthRecord> weightList = new ArrayList<>();
        for (HealthRecord item : list) {
            if (item.getWeightKg() != null) {
                weightList.add(item);
            }
        }

        if (weightList.size() < 2) {
            return "数据不足";
        }

        Double first = weightList.get(0).getWeightKg();
        Double last = weightList.get(weightList.size() - 1).getWeightKg();
        double diff = last - first;

        if (Math.abs(diff) < 0.5) {
            return "稳定";
        }
        return diff > 0 ? "上升" : "下降";
    }

    private Integer bpRiskCount(List<HealthRecord> list) {
        int cnt = 0;
        for (HealthRecord r : list) {
            if (r.getSystolic() != null && r.getDiastolic() != null) {
                if (r.getSystolic() >= HealthConstants.BP_SYS_MID
                        || r.getDiastolic() >= HealthConstants.BP_DIA_MID) {
                    cnt++;
                }
            }
        }
        return cnt;
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON 序列化失败", e);
            return "{}";
        }
    }

    private Integer toInt(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(value)));
        } catch (Exception e) {
            return 0;
        }
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return round2(Double.parseDouble(String.valueOf(value)));
        } catch (Exception e) {
            return null;
        }
    }

    private String formatDouble(Double value) {
        if (value == null) {
            return "0";
        }
        return String.valueOf(round2(value));
    }

    private Double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}