package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sphas.project03.common.HealthConstants;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.HealthRecord;
import com.sphas.project03.entity.WeeklyReportRecord;
import com.sphas.project03.mapper.HealthRecordMapper;
import com.sphas.project03.mapper.WeeklyReportMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理端：系统周报 / 月报查看与生成
 * 这里只看 userId = 0 的系统汇总报告
 */
@RestController
@RequestMapping("/api/health/report/admin")
public class HealthReportAdminController extends BaseController {

    private final WeeklyReportMapper weeklyReportMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HealthReportAdminController(WeeklyReportMapper weeklyReportMapper,
                                       HealthRecordMapper healthRecordMapper) {
        this.weeklyReportMapper = weeklyReportMapper;
        this.healthRecordMapper = healthRecordMapper;
    }

    /**
     * 报告分页
     */
    @GetMapping("/page")
    public R<Page<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String reportType,
                                             @RequestParam(required = false) String keyword,
                                             HttpServletRequest request) {
        requireAdmin(request);

        LambdaQueryWrapper<WeeklyReportRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(WeeklyReportRecord::getUserId, 0L);

        if (StringUtils.hasText(reportType)) {
            if ("WEEK".equalsIgnoreCase(reportType.trim())) {
                qw.eq(WeeklyReportRecord::getReportType, "WEEK");
            } else if ("MONTH".equalsIgnoreCase(reportType.trim())) {
                qw.eq(WeeklyReportRecord::getReportType, "MONTH");
            }
        }

        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(WeeklyReportRecord::getTitle, keyword.trim())
                    .or()
                    .like(WeeklyReportRecord::getSummary, keyword.trim()));
        }

        qw.orderByDesc(WeeklyReportRecord::getWeekStart)
                .orderByDesc(WeeklyReportRecord::getId);

        Page<WeeklyReportRecord> rawPage = weeklyReportMapper.selectPage(new Page<>(pageNum, pageSize), qw);

        List<Map<String, Object>> records = rawPage.getRecords().stream()
                .map(this::buildRow)
                .collect(Collectors.toList());

        Page<Map<String, Object>> res = new Page<>(pageNum, pageSize);
        res.setCurrent(rawPage.getCurrent());
        res.setSize(rawPage.getSize());
        res.setTotal(rawPage.getTotal());
        res.setRecords(records);

        return R.ok(res);
    }

    /**
     * 手动生成周报 / 月报
     */
    @PostMapping("/generate")
    public R<Map<String, Object>> generate(@RequestBody GenerateDTO dto, HttpServletRequest request) {
        requireAdmin(request);

        String reportType = dto == null ? null : dto.getReportType();
        if (!"WEEK".equals(reportType) && !"MONTH".equals(reportType)) {
            return R.fail("报告类型只能是 WEEK 或 MONTH");
        }

        LocalDate to = LocalDate.now();
        LocalDate from = "WEEK".equals(reportType) ? to.minusDays(6) : to.minusDays(29);

        List<HealthRecord> list = healthRecordMapper.selectList(
                new LambdaQueryWrapper<HealthRecord>()
                        .ge(HealthRecord::getRecordDate, from)
                        .le(HealthRecord::getRecordDate, to)
                        .orderByAsc(HealthRecord::getRecordDate)
        );

        Map<String, Object> metrics = buildMetrics(reportType, list, from, to);
        Map<String, Object> detail = buildDetail(reportType, metrics);

        WeeklyReportRecord db = weeklyReportMapper.selectOne(
                new LambdaQueryWrapper<WeeklyReportRecord>()
                        .eq(WeeklyReportRecord::getUserId, 0L)
                        .eq(WeeklyReportRecord::getReportType, reportType)
                        .eq(WeeklyReportRecord::getWeekStart, from)
                        .eq(WeeklyReportRecord::getWeekEnd, to)
                        .last("limit 1")
        );

        LocalDateTime now = LocalDateTime.now();
        if (db == null) {
            db = new WeeklyReportRecord();
            db.setCreatedAt(now);
        }

        db.setUserId(0L);
        db.setReportType(reportType);
        db.setWeekStart(from);
        db.setWeekEnd(to);
        db.setTitle(buildTitle(reportType, from, to));
        db.setSummary(String.valueOf(detail.getOrDefault("summary", "")));
        db.setMetricsJson(writeJson(metrics));
        db.setTableJson(writeJson(detail));
        db.setUpdatedAt(now);

        if (db.getId() == null) {
            weeklyReportMapper.insert(db);
        } else {
            weeklyReportMapper.updateById(db);
        }

        return R.ok(buildRow(db));
    }

    private String buildTitle(String reportType, LocalDate from, LocalDate to) {
        if ("MONTH".equals(reportType)) {
            return "[MONTH] " + from + " ~ " + to + " 月健康总结报告";
        }
        return "[WEEK] " + from + " ~ " + to + " 周健康总结报告";
    }

    private Map<String, Object> buildRow(WeeklyReportRecord record) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", record.getId());
        row.put("userId", record.getUserId());
        row.put("reportType", normalizeType(record));
        row.put("weekStart", record.getWeekStart());
        row.put("weekEnd", record.getWeekEnd());
        row.put("title", record.getTitle());
        row.put("summary", record.getSummary());
        row.put("createdAt", record.getCreatedAt());
        row.put("updatedAt", record.getUpdatedAt());

        Map<String, Object> metrics = parseJsonMap(record.getMetricsJson());
        Map<String, Object> detail = parseJsonMap(record.getTableJson());

        row.put("recordCount", metrics.getOrDefault("recordCount", 0));
        row.put("userCount", metrics.getOrDefault("userCount", 0));
        row.put("avgWeight", metrics.get("avgWeight"));
        row.put("avgSteps", metrics.get("avgSteps"));
        row.put("avgSleepHours", metrics.get("avgSleepHours"));
        row.put("weightTrend", metrics.getOrDefault("weightTrend", "数据不足"));
        row.put("bpRiskCount", metrics.getOrDefault("bpRiskCount", 0));

        row.put("metricsJson", record.getMetricsJson());
        row.put("tableJson", record.getTableJson());
        row.put("riskTips", toStringList(detail.get("riskTips")));
        row.put("suggestions", toStringList(detail.get("suggestions")));

        return row;
    }

    private Map<String, Object> buildMetrics(String reportType,
                                             List<HealthRecord> list,
                                             LocalDate from,
                                             LocalDate to) {
        Map<String, Object> metrics = new HashMap<>();

        Set<Long> userIds = list.stream()
                .map(HealthRecord::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        metrics.put("reportType", reportType);
        metrics.put("scope", "SYSTEM");
        metrics.put("from", from.toString());
        metrics.put("to", to.toString());
        metrics.put("recordCount", list.size());
        metrics.put("userCount", userIds.size());
        metrics.put("avgWeight", avgWeight(list));
        metrics.put("avgSteps", avgSteps(list));
        metrics.put("avgSleepHours", avgSleep(list));
        metrics.put("weightTrend", weightTrend(list));
        metrics.put("bpRiskCount", bpRiskCount(list));

        return metrics;
    }

    private Map<String, Object> buildDetail(String reportType, Map<String, Object> metrics) {
        Map<String, Object> detail = new HashMap<>();

        List<String> riskTips = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        Integer recordCount = toInteger(metrics.get("recordCount"));
        Integer userCount = toInteger(metrics.get("userCount"));
        Double avgWeight = toDouble(metrics.get("avgWeight"));
        Integer avgSteps = toInteger(metrics.get("avgSteps"));
        Double avgSleepHours = toDouble(metrics.get("avgSleepHours"));
        String weightTrend = String.valueOf(metrics.getOrDefault("weightTrend", "数据不足"));
        Integer bpRiskCount = toInteger(metrics.get("bpRiskCount"));

        if (recordCount == 0) {
            riskTips.add("当前周期暂无健康记录，报告结论参考性较低。");
            suggestions.add("建议提醒用户坚持每日录入健康数据。");
        }

        if (bpRiskCount > 0) {
            riskTips.add("本周期存在 " + bpRiskCount + " 条血压偏高记录，需要重点关注。");
            suggestions.add("建议加强高血压风险用户的复测提醒与饮食干预。");
        }

        int stepLine = "WEEK".equals(reportType) ? 8000 : 7000;
        if (avgSteps != null && avgSteps < stepLine) {
            riskTips.add("整体活动量偏低，平均步数未达到推荐水平。");
            suggestions.add("建议提升用户日均活动量，逐步达到 " + stepLine + " 步以上。");
        }

        if (avgSleepHours != null && avgSleepHours < 7) {
            riskTips.add("整体睡眠时长偏短，存在作息不规律风险。");
            suggestions.add("建议优化作息安排，保证 7 小时以上睡眠。");
        }

        if ("上升".equals(weightTrend)) {
            riskTips.add("体重呈上升趋势，需要关注饮食控制与运动执行。");
            suggestions.add("建议结合推荐计划跟踪饮食与运动完成情况。");
        }

        if (avgWeight != null && avgWeight > 85) {
            riskTips.add("样本用户平均体重偏高，肥胖相关风险需要持续关注。");
            suggestions.add("建议增加有氧运动和饮食热量控制。");
        }

        if (riskTips.isEmpty()) {
            riskTips.add("本周期整体健康趋势平稳，暂未发现明显集中风险。");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("整体状态较稳定，建议继续保持当前健康管理节奏。");
        }

        String summary = "本周期共统计 " + recordCount + " 条记录，覆盖 "
                + userCount + " 位用户，平均体重 " + formatDouble(avgWeight)
                + "kg，平均步数 " + (avgSteps == null ? 0 : avgSteps)
                + " 步，平均睡眠 " + formatDouble(avgSleepHours)
                + " 小时，体重趋势为 " + weightTrend + "。";

        detail.put("summary", summary);
        detail.put("riskTips", riskTips);
        detail.put("suggestions", suggestions);

        return detail;
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
        return cnt == 0 ? 0 : (int) Math.round(sum * 1.0 / cnt);
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
        List<HealthRecord> weightList = list.stream()
                .filter(item -> item.getWeightKg() != null)
                .collect(Collectors.toList());

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

    private int bpRiskCount(List<HealthRecord> list) {
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

    private String normalizeType(WeeklyReportRecord record) {
        if (StringUtils.hasText(record.getReportType())) {
            return record.getReportType();
        }
        if (record.getTitle() != null && record.getTitle().startsWith("[MONTH]")) {
            return "MONTH";
        }
        return "WEEK";
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (!StringUtils.hasText(json)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private List<String> toStringList(Object value) {
        List<String> res = new ArrayList<>();
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            for (Object item : list) {
                if (item != null) {
                    res.add(String.valueOf(item));
                }
            }
        }
        return res;
    }

    private Double toDouble(Object v) {
        if (v == null) return null;
        try {
            return Double.valueOf(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInteger(Object v) {
        if (v == null) return 0;
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(v)));
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatDouble(Double value) {
        if (value == null) {
            return "0";
        }
        return String.valueOf(round(value));
    }

    private double round(double v) {
        return Math.round(v * 10) / 10.0;
    }

    public static class GenerateDTO {
        private String reportType;

        public String getReportType() {
            return reportType;
        }

        public void setReportType(String reportType) {
            this.reportType = reportType;
        }
    }
}