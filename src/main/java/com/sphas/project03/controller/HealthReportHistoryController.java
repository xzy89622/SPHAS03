package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.WeeklyReportRecord;
import com.sphas.project03.mapper.WeeklyReportMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户端：历史报告查看
 * 这里只查当前登录用户自己的报告
 */
@RestController
@RequestMapping("/api/health/report/history")
public class HealthReportHistoryController extends BaseController {

    private final WeeklyReportMapper weeklyReportMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HealthReportHistoryController(WeeklyReportMapper weeklyReportMapper) {
        this.weeklyReportMapper = weeklyReportMapper;
    }

    /**
     * 历史报告分页
     */
    @GetMapping("/page")
    public R<Page<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String reportType,
                                             HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return R.fail("未登录");
        }

        LambdaQueryWrapper<WeeklyReportRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(WeeklyReportRecord::getUserId, userId);

        if (StringUtils.hasText(reportType)) {
            if ("WEEK".equalsIgnoreCase(reportType.trim())) {
                qw.eq(WeeklyReportRecord::getReportType, "WEEK");
            } else if ("MONTH".equalsIgnoreCase(reportType.trim())) {
                qw.eq(WeeklyReportRecord::getReportType, "MONTH");
            }
        }

        qw.orderByDesc(WeeklyReportRecord::getWeekStart)
                .orderByDesc(WeeklyReportRecord::getId);

        Page<WeeklyReportRecord> rawPage = weeklyReportMapper.selectPage(new Page<>(pageNum, pageSize), qw);

        Page<Map<String, Object>> res = new Page<>(pageNum, pageSize);
        res.setCurrent(rawPage.getCurrent());
        res.setSize(rawPage.getSize());
        res.setTotal(rawPage.getTotal());
        res.setRecords(
                rawPage.getRecords()
                        .stream()
                        .map(this::buildRow)
                        .collect(Collectors.toList())
        );

        return R.ok(res);
    }

    /**
     * 历史报告详情
     */
    @GetMapping("/detail")
    public R<Map<String, Object>> detail(@RequestParam Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return R.fail("未登录");
        }

        WeeklyReportRecord record = weeklyReportMapper.selectById(id);
        if (record == null) {
            return R.fail("报告不存在");
        }

        if (record.getUserId() == null || !userId.equals(record.getUserId())) {
            return R.fail("无权查看该报告");
        }

        return R.ok(buildRow(record));
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
        row.put("avgWeight", metrics.get("avgWeight"));
        row.put("avgSteps", metrics.get("avgSteps"));
        row.put("avgSleepHours", metrics.get("avgSleepHours"));
        row.put("weightTrend", metrics.getOrDefault("weightTrend", "数据不足"));
        row.put("bpRiskCount", metrics.getOrDefault("bpRiskCount", 0));
        row.put("riskLevel", metrics.getOrDefault("riskLevel", "LOW"));

        row.put("riskTips", toStringList(detail.get("riskTips")));
        row.put("suggestions", toStringList(detail.get("suggestions")));
        row.put("metricsJson", record.getMetricsJson());
        row.put("tableJson", record.getTableJson());

        return row;
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
}