package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.DashboardTrendPointDTO;
import com.sphas.project03.entity.HealthMetricRecord;
import com.sphas.project03.service.HealthMetricRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 健康看板接口（给前端 ECharts 用）
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController extends BaseController {

    private final HealthMetricRecordService metricService;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public DashboardController(HealthMetricRecordService metricService) {
        this.metricService = metricService;
    }

    /**
     * 看板总览：最新值 + 与上一条对比 + 最近7天简单统计
     */
    @GetMapping("/overview")
    public R<Map<String, Object>> overview(HttpServletRequest request) {

        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        // 取最近2条：用于最新值 + 对比差值
        List<HealthMetricRecord> lastTwo = metricService.list(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .orderByDesc(HealthMetricRecord::getRecordTime)
                        .last("limit 2")
        );

        Map<String, Object> res = new HashMap<>();

        if (lastTwo.isEmpty()) {
            res.put("hasData", false);
            res.put("msg", "暂无体质记录，请先录入体质数据");
            return R.ok(res);
        }

        HealthMetricRecord latest = lastTwo.get(0);
        HealthMetricRecord prev = lastTwo.size() > 1 ? lastTwo.get(1) : null;

        res.put("hasData", true);
        res.put("latest", latest); // 前端可直接渲染卡片（也可只取部分字段）
        res.put("latestTime", latest.getRecordTime() == null ? null : latest.getRecordTime().format(TIME_FMT));

        // 对比差值（只对常用指标做 diff）
        Map<String, Object> diff = new HashMap<>();
        if (prev != null) {
            diff.put("weightDiff", diffBD(latest.getWeightKg(), prev.getWeightKg()));
            diff.put("bmiDiff", diffBD(latest.getBmi(), prev.getBmi()));
            diff.put("stepsDiff", diffInt(latest.getSteps(), prev.getSteps()));
            diff.put("sleepDiff", diffBD(latest.getSleepHours(), prev.getSleepHours()));
            diff.put("systolicDiff", diffInt(latest.getSystolic(), prev.getSystolic()));
            diff.put("diastolicDiff", diffInt(latest.getDiastolic(), prev.getDiastolic()));
            diff.put("sugarDiff", diffBD(latest.getBloodSugar(), prev.getBloodSugar()));
        }
        res.put("diff", diff);

        // 最近7天统计：平均步数/平均睡眠/平均BMI（可用于小卡片）
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        List<HealthMetricRecord> last7 = metricService.list(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .ge(HealthMetricRecord::getRecordTime, from)
                        .orderByAsc(HealthMetricRecord::getRecordTime)
        );

        Map<String, Object> stats7 = new HashMap<>();
        stats7.put("count", last7.size());
        stats7.put("avgSteps", avgInt(last7, "steps"));
        stats7.put("avgSleep", avgBigDecimal(last7, "sleep"));
        stats7.put("avgBmi", avgBigDecimal(last7, "bmi"));
        res.put("stats7days", stats7);

        return R.ok(res);
    }

    /**
     * 趋势数据：最近days天所有记录点（给 ECharts 直接画折线）
     * 例：GET /api/dashboard/trend?days=30
     */
    @GetMapping("/trend")
    public R<List<DashboardTrendPointDTO>> trend(@RequestParam(defaultValue = "30") int days,
                                                 HttpServletRequest request) {

        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        if (days <= 0 || days > 365) {
            throw new BizException("days 参数不合理（1-365）");
        }

        LocalDateTime from = LocalDateTime.now().minusDays(days);

        List<HealthMetricRecord> list = metricService.list(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .ge(HealthMetricRecord::getRecordTime, from)
                        .orderByAsc(HealthMetricRecord::getRecordTime)
        );

        // 转成前端更好用的点数据
        List<DashboardTrendPointDTO> points = new ArrayList<>();
        for (HealthMetricRecord r : list) {
            DashboardTrendPointDTO p = new DashboardTrendPointDTO();

            // x轴时间
            if (r.getRecordTime() != null) {
                p.setTime(r.getRecordTime().format(TIME_FMT));
            }

            // y轴数据
            p.setBmi(r.getBmi());
            p.setWeightKg(r.getWeightKg());
            p.setSteps(r.getSteps());
            p.setSleepHours(r.getSleepHours());
            p.setSystolic(r.getSystolic());
            p.setDiastolic(r.getDiastolic());
            p.setBloodSugar(r.getBloodSugar());

            points.add(p);
        }

        return R.ok(points);
    }

    // ====================== 工具方法（带注释，方便你理解/答辩） ======================

    /**
     * BigDecimal 差值：a - b（保留2位小数）
     */
    private BigDecimal diffBD(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return null;
        return a.subtract(b).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Integer 差值：a - b
     */
    private Integer diffInt(Integer a, Integer b) {
        if (a == null || b == null) return null;
        return a - b;
    }

    /**
     * 平均值（整数类）：目前用于 steps
     */
    private Integer avgInt(List<HealthMetricRecord> list, String type) {
        long sum = 0;
        int cnt = 0;

        for (HealthMetricRecord r : list) {
            Integer v = null;
            if ("steps".equals(type)) v = r.getSteps();

            if (v != null) {
                sum += v;
                cnt++;
            }
        }
        if (cnt == 0) return null;
        return (int) (sum / cnt);
    }

    /**
     * 平均值（小数类）：用于 sleep/bmi
     */
    private BigDecimal avgBigDecimal(List<HealthMetricRecord> list, String type) {
        BigDecimal sum = BigDecimal.ZERO;
        int cnt = 0;

        for (HealthMetricRecord r : list) {
            BigDecimal v = null;
            if ("sleep".equals(type)) v = r.getSleepHours();
            if ("bmi".equals(type)) v = r.getBmi();

            if (v != null) {
                sum = sum.add(v);
                cnt++;
            }
        }
        if (cnt == 0) return null;
        return sum.divide(new BigDecimal(cnt), 2, RoundingMode.HALF_UP);
    }
}
