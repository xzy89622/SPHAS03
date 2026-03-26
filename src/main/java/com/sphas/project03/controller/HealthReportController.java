package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.MonthlyReportDTO;
import com.sphas.project03.controller.dto.WeeklyReportDTO;
import com.sphas.project03.entity.WeeklyReportRecord;
import com.sphas.project03.mapper.WeeklyReportMapper;
import com.sphas.project03.service.HealthReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康报告接口（需要登录）
 * 这里把当前周报 / 月报 和历史报告统一收口
 */
@RestController
@RequestMapping("/api/health/report")
public class HealthReportController extends BaseController {

    private final HealthReportService healthReportService;
    private final WeeklyReportMapper weeklyReportMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HealthReportController(HealthReportService healthReportService,
                                  WeeklyReportMapper weeklyReportMapper) {
        this.healthReportService = healthReportService;
        this.weeklyReportMapper = weeklyReportMapper;
    }

    /**
     * 当前周报
     */
    @GetMapping("/weekly")
    public R<WeeklyReportDTO> weekly(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return R.fail("未登录");
        }

        WeeklyReportDTO dto = healthReportService.weekly(userId);
        saveWeeklyHistory(userId, dto);

        return R.ok(dto);
    }

    /**
     * 当前月报
     */
    @GetMapping("/monthly")
    public R<MonthlyReportDTO> monthly(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return R.fail("未登录");
        }

        MonthlyReportDTO dto = healthReportService.monthly(userId);
        saveMonthlyHistory(userId, dto);

        return R.ok(dto);
    }

    /**
     * 周报 PDF
     * 这里也顺手把历史报告落库，避免用户只导出不查看时历史里没有记录
     */
    @GetMapping("/weekly/pdf")
    public void weeklyPdf(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long userId = getUserId(request);
        if (userId == null) {
            writeUnauthorized(response);
            return;
        }

        WeeklyReportDTO dto = healthReportService.weekly(userId);
        saveWeeklyHistory(userId, dto);

        byte[] pdf = healthReportService.weeklyPdf(userId);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=weekly-report.pdf");
        response.getOutputStream().write(pdf);
    }

    /**
     * 月报 PDF
     * 这里也顺手把历史报告落库，避免用户只导出不查看时历史里没有记录
     */
    @GetMapping("/monthly/pdf")
    public void monthlyPdf(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long userId = getUserId(request);
        if (userId == null) {
            writeUnauthorized(response);
            return;
        }

        MonthlyReportDTO dto = healthReportService.monthly(userId);
        saveMonthlyHistory(userId, dto);

        byte[] pdf = healthReportService.monthlyPdf(userId);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=monthly-report.pdf");
        response.getOutputStream().write(pdf);
    }

    /**
     * 保存周报历史
     */
    private void saveWeeklyHistory(Long userId, WeeklyReportDTO dto) {
        if (dto == null || dto.getFrom() == null || dto.getTo() == null) {
            return;
        }

        LocalDate from = LocalDate.parse(dto.getFrom());
        LocalDate to = LocalDate.parse(dto.getTo());

        WeeklyReportRecord db = weeklyReportMapper.selectOne(
                new LambdaQueryWrapper<WeeklyReportRecord>()
                        .eq(WeeklyReportRecord::getUserId, userId)
                        .eq(WeeklyReportRecord::getReportType, "WEEK")
                        .eq(WeeklyReportRecord::getWeekStart, from)
                        .eq(WeeklyReportRecord::getWeekEnd, to)
                        .last("limit 1")
        );

        LocalDateTime now = LocalDateTime.now();
        if (db == null) {
            db = new WeeklyReportRecord();
            db.setCreatedAt(now);
        }

        db.setUserId(userId);
        db.setReportType("WEEK");
        db.setWeekStart(from);
        db.setWeekEnd(to);
        db.setTitle("[WEEK] " + from + " ~ " + to + " 我的周健康报告");
        db.setSummary(dto.getSummary());
        db.setMetricsJson(writeJson(buildWeeklyMetrics(dto)));
        db.setTableJson(writeJson(buildWeeklyDetail(dto)));
        db.setUpdatedAt(now);

        if (db.getId() == null) {
            weeklyReportMapper.insert(db);
        } else {
            weeklyReportMapper.updateById(db);
        }
    }

    /**
     * 保存月报历史
     */
    private void saveMonthlyHistory(Long userId, MonthlyReportDTO dto) {
        if (dto == null || dto.getFrom() == null || dto.getTo() == null) {
            return;
        }

        LocalDate from = LocalDate.parse(dto.getFrom());
        LocalDate to = LocalDate.parse(dto.getTo());

        WeeklyReportRecord db = weeklyReportMapper.selectOne(
                new LambdaQueryWrapper<WeeklyReportRecord>()
                        .eq(WeeklyReportRecord::getUserId, userId)
                        .eq(WeeklyReportRecord::getReportType, "MONTH")
                        .eq(WeeklyReportRecord::getWeekStart, from)
                        .eq(WeeklyReportRecord::getWeekEnd, to)
                        .last("limit 1")
        );

        LocalDateTime now = LocalDateTime.now();
        if (db == null) {
            db = new WeeklyReportRecord();
            db.setCreatedAt(now);
        }

        db.setUserId(userId);
        db.setReportType("MONTH");
        db.setWeekStart(from);
        db.setWeekEnd(to);
        db.setTitle("[MONTH] " + from + " ~ " + to + " 我的月健康报告");
        db.setSummary(dto.getSummary());
        db.setMetricsJson(writeJson(buildMonthlyMetrics(dto)));
        db.setTableJson(writeJson(buildMonthlyDetail(dto)));
        db.setUpdatedAt(now);

        if (db.getId() == null) {
            weeklyReportMapper.insert(db);
        } else {
            weeklyReportMapper.updateById(db);
        }
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
        detail.put("suggestions", safeList(dto.getSuggestions()));
        return detail;
    }

    private Map<String, Object> buildMonthlyDetail(MonthlyReportDTO dto) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("summary", dto.getSummary());
        detail.put("riskTips", buildRiskTips(dto.getBpRisk(), dto.getWeightTrend(), dto.getAvgSleepHours(), dto.getAvgSteps()));
        detail.put("suggestions", safeList(dto.getSuggestions()));
        return detail;
    }

    private List<String> buildRiskTips(Boolean bpRisk, String weightTrend, Double avgSleepHours, Integer avgSteps) {
        java.util.ArrayList<String> list = new java.util.ArrayList<>();

        if (Boolean.TRUE.equals(bpRisk)) {
            list.add("存在血压偏高风险，需要重点关注");
        }
        if ("上升".equals(weightTrend)) {
            list.add("体重呈上升趋势，需要关注饮食控制与运动执行");
        }
        if (avgSleepHours != null && avgSleepHours < 7) {
            list.add("睡眠时长偏短，存在作息不规律风险");
        }
        if (avgSteps != null && avgSteps > 0 && avgSteps < 7000) {
            list.add("活动量偏低，平均步数未达到推荐水平");
        }

        if (list.isEmpty()) {
            list.add("本周期整体健康趋势平稳，暂未发现明显风险");
        }

        return list;
    }

    private List<String> safeList(List<String> list) {
        return list == null ? new java.util.ArrayList<>() : list;
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

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"未登录\",\"data\":null}");
    }
}