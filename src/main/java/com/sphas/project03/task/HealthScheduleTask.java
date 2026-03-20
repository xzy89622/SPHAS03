package com.sphas.project03.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sphas.project03.common.HealthConstants;
import com.sphas.project03.entity.HealthRecord;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.entity.WeeklyReportRecord;
import com.sphas.project03.mapper.HealthRecordMapper;
import com.sphas.project03.mapper.SysUserMapper;
import com.sphas.project03.mapper.WeeklyReportMapper;
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
 * 这版做两件事：
 * 1. 自动生成系统汇总周报 / 月报
 * 2. 自动生成每个普通用户自己的周报 / 月报历史
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthScheduleTask {

    private final SysUserMapper sysUserMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final WeeklyReportMapper weeklyReportMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 每周一早上 7 点生成上一周周报
     */
    @Scheduled(cron = "0 0 7 * * MON")
    public void generateWeeklyReportTask() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.minusDays(1);
        LocalDate startDate = endDate.with(DayOfWeek.MONDAY);

        generateAllReports("WEEK", startDate, endDate);
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

        generateAllReports("MONTH", startDate, endDate);
        log.info("月报自动生成完成：{} ~ {}", startDate, endDate);
    }

    /**
     * 生成整套报告
     */
    private void generateAllReports(String reportType, LocalDate startDate, LocalDate endDate) {
        saveSystemReport(reportType, startDate, endDate);
        saveAllUserReports(reportType, startDate, endDate);
    }

    /**
     * 保存系统汇总报告
     */
    private void saveSystemReport(String reportType, LocalDate startDate, LocalDate endDate) {
        List<HealthRecord> recordList = healthRecordMapper.selectList(
                new LambdaQueryWrapper<HealthRecord>()
                        .ge(HealthRecord::getRecordDate, startDate)
                        .le(HealthRecord::getRecordDate, endDate)
                        .orderByAsc(HealthRecord::getRecordDate)
        );

        Map<String, Object> metrics = buildSystemMetrics(reportType, recordList, startDate, endDate);
        Map<String, Object> detail = buildSystemDetail(reportType, metrics);

        WeeklyReportRecord db = weeklyReportMapper.selectOne(
                new LambdaQueryWrapper<WeeklyReportRecord>()
                        .eq(WeeklyReportRecord::getUserId, 0L)
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

        db.setUserId(0L);
        db.setReportType(reportType);
        db.setWeekStart(startDate);
        db.setWeekEnd(endDate);
        db.setTitle(buildSystemTitle(reportType, startDate, endDate));
        db.setSummary(String.valueOf(detail.getOrDefault("summary", "")));
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
     * 给所有普通用户生成个人报告
     */
    private void saveAllUserReports(String reportType, LocalDate startDate, LocalDate endDate) {
        List<SysUser> userList = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, "USER")
                        .eq(SysUser::getStatus, 1)
        );

        for (SysUser user : userList) {
            try {
                saveUserReport(user, reportType, startDate, endDate);
            } catch (Exception e) {
                log.error("生成用户个人报告失败，userId={}", user.getId(), e);
            }
        }
    }

    /**
     * 保存单个用户个人报告
     */
    private void saveUserReport(SysUser user, String reportType, LocalDate startDate, LocalDate endDate) {
        List<HealthRecord> list = healthRecordMapper.selectList(
                new LambdaQueryWrapper<HealthRecord>()
                        .eq(HealthRecord::getUserId, user.getId())
                        .ge(HealthRecord::getRecordDate, startDate)
                        .le(HealthRecord::getRecordDate, endDate)
                        .orderByAsc(HealthRecord::getRecordDate)
        );

        if (list.isEmpty()) {
            return;
        }

        Map<String, Object> metrics = buildUserMetrics(reportType, user, list, startDate, endDate);
        Map<String, Object> detail = buildUserDetail(reportType, metrics);

        WeeklyReportRecord db = weeklyReportMapper.selectOne(
                new LambdaQueryWrapper<WeeklyReportRecord>()
                        .eq(WeeklyReportRecord::getUserId, user.getId())
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

        db.setUserId(user.getId());
        db.setReportType(reportType);
        db.setWeekStart(startDate);
        db.setWeekEnd(endDate);
        db.setTitle(buildUserTitle(reportType, startDate, endDate));
        db.setSummary(String.valueOf(detail.getOrDefault("summary", "")));
        db.setMetricsJson(writeJson(metrics));
        db.setTableJson(writeJson(detail));
        db.setUpdatedAt(now);

        if (db.getId() == null) {
            weeklyReportMapper.insert(db);
        } else {
            weeklyReportMapper.updateById(db);
        }
    }

    private String buildSystemTitle(String reportType, LocalDate startDate, LocalDate endDate) {
        if ("MONTH".equals(reportType)) {
            return "[MONTH] " + startDate + " ~ " + endDate + " 月健康总结报告";
        }
        return "[WEEK] " + startDate + " ~ " + endDate + " 周健康总结报告";
    }

    private String buildUserTitle(String reportType, LocalDate startDate, LocalDate endDate) {
        if ("MONTH".equals(reportType)) {
            return "[MONTH] " + startDate + " ~ " + endDate + " 我的月健康报告";
        }
        return "[WEEK] " + startDate + " ~ " + endDate + " 我的周健康报告";
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
     * 个人报告指标
     */
    private Map<String, Object> buildUserMetrics(String reportType,
                                                 SysUser user,
                                                 List<HealthRecord> list,
                                                 LocalDate startDate,
                                                 LocalDate endDate) {
        Map<String, Object> metrics = new HashMap<>();

        Double avgWeight = avgWeight(list);
        Double avgSleep = avgSleepHours(list);
        Integer avgSteps = avgSteps(list);
        Integer bpRiskCount = bpRiskCount(list);
        String weightTrend = weightTrend(list);
        Double latestBmi = latestBmi(list);

        metrics.put("reportType", reportType);
        metrics.put("scope", "USER");
        metrics.put("userId", user.getId());
        metrics.put("nickname", user.getNickname());
        metrics.put("from", startDate.toString());
        metrics.put("to", endDate.toString());
        metrics.put("recordCount", list.size());
        metrics.put("avgWeight", avgWeight);
        metrics.put("avgSteps", avgSteps);
        metrics.put("avgSleepHours", avgSleep);
        metrics.put("weightTrend", weightTrend);
        metrics.put("bpRiskCount", bpRiskCount);
        metrics.put("latestBmi", latestBmi);
        metrics.put("riskLevel", userRiskLevel(latestBmi, avgSleep, avgSteps, bpRiskCount));

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

    /**
     * 个人报告详情
     */
    private Map<String, Object> buildUserDetail(String reportType, Map<String, Object> metrics) {
        Map<String, Object> detail = new HashMap<>();
        List<String> riskTips = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        Integer recordCount = toInt(metrics.get("recordCount"));
        Double avgWeight = toDouble(metrics.get("avgWeight"));
        Integer avgSteps = toInt(metrics.get("avgSteps"));
        Double avgSleepHours = toDouble(metrics.get("avgSleepHours"));
        String weightTrend = String.valueOf(metrics.getOrDefault("weightTrend", "数据不足"));
        Integer bpRiskCount = toInt(metrics.get("bpRiskCount"));
        Double latestBmi = toDouble(metrics.get("latestBmi"));
        String riskLevel = String.valueOf(metrics.getOrDefault("riskLevel", "LOW"));

        if (recordCount == 0) {
            riskTips.add("当前周期暂无健康记录，报告参考性较低。");
            suggestions.add("建议坚持每日记录健康数据。");
        }

        if (latestBmi != null) {
            if (latestBmi >= 24) {
                riskTips.add("当前 BMI 偏高，需要关注体重控制。");
                suggestions.add("建议减少高糖高脂食物摄入，保持规律运动。");
            } else if (latestBmi > 0 && latestBmi < 18.5) {
                riskTips.add("当前 BMI 偏低，需要注意营养摄入。");
                suggestions.add("建议适当增加优质蛋白和主食摄入。");
            }
        }

        if (bpRiskCount > 0) {
            riskTips.add("本周期出现血压偏高记录，需要持续复测。");
            suggestions.add("建议清淡饮食，减少熬夜，必要时及时就医。");
        }

        int stepLine = "WEEK".equals(reportType) ? 8000 : 7000;
        if (avgSteps > 0 && avgSteps < stepLine) {
            riskTips.add("运动量偏少，平均步数未达到推荐水平。");
            suggestions.add("建议增加日常步行和轻运动，逐步把步数提升到 " + stepLine + " 步以上。");
        }

        if (avgSleepHours != null && avgSleepHours > 0 && avgSleepHours < 7) {
            riskTips.add("睡眠时长偏短，存在作息不规律风险。");
            suggestions.add("建议尽量在 23 点前入睡，保证 7 小时以上睡眠。");
        }

        if ("上升".equals(weightTrend)) {
            riskTips.add("体重呈上升趋势，需要关注近期饮食和运动执行情况。");
            suggestions.add("建议配合系统推荐计划持续干预。");
        } else if ("下降".equals(weightTrend)) {
            suggestions.add("体重整体呈下降趋势，可以继续保持当前健康管理节奏。");
        }

        if (riskTips.isEmpty()) {
            riskTips.add("本周期整体状态较平稳，没有明显异常波动。");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("建议继续保持当前生活习惯，定期记录健康数据。");
        }

        String summary = "本周期共记录 " + recordCount + " 次，平均体重 "
                + formatDouble(avgWeight) + "kg，平均步数 "
                + avgSteps + " 步，平均睡眠 "
                + formatDouble(avgSleepHours) + " 小时，体重趋势为 "
                + weightTrend + "，综合风险等级为 " + riskLevel + "。";

        detail.put("summary", summary);
        detail.put("riskTips", riskTips);
        detail.put("suggestions", suggestions);

        return detail;
    }

    private Double latestBmi(List<HealthRecord> list) {
        for (int i = list.size() - 1; i >= 0; i--) {
            HealthRecord item = list.get(i);
            if (item.getHeightCm() != null && item.getHeightCm() > 0
                    && item.getWeightKg() != null && item.getWeightKg() > 0) {
                double heightMeter = item.getHeightCm() / 100.0;
                return round2(item.getWeightKg() / (heightMeter * heightMeter));
            }
        }
        return null;
    }

    private String userRiskLevel(Double bmi, Double avgSleepHours, Integer avgSteps, Integer bpRiskCount) {
        int score = 0;

        if (bmi != null) {
            if (bmi >= 28 || bmi < 18.5) {
                score += 2;
            } else if (bmi >= 24) {
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

        if (avgSteps != null) {
            if (avgSteps < 4000) {
                score += 2;
            } else if (avgSteps < 6000) {
                score += 1;
            }
        }

        if (bpRiskCount != null && bpRiskCount > 0) {
            score += 2;
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

    private Integer toInt(Object v) {
        if (v == null) {
            return 0;
        }
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(v)));
        } catch (Exception e) {
            return 0;
        }
    }

    private Double toDouble(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return round2(Double.parseDouble(String.valueOf(v)));
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