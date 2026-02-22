package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.DashboardTrendPointDTO;
import com.sphas.project03.entity.HealthMetricRecord;
import com.sphas.project03.service.HealthMetricRecordService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils; // ✅ 就是缺这个
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
import java.util.concurrent.TimeUnit;

/**
 * 健康看板接口（给前端 ECharts 用）
 * ✅ 加了Redis缓存：减少DB压力（60秒过期）
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController extends BaseController {

    private final HealthMetricRecordService metricService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public DashboardController(HealthMetricRecordService metricService,
                               StringRedisTemplate stringRedisTemplate,
                               ObjectMapper objectMapper) {
        this.metricService = metricService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 看板总览：最新值 + 与上一条对比 + 最近7天简单统计
     */
    @GetMapping("/overview")
    public R<Map<String, Object>> overview(HttpServletRequest request) {

        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        // ✅ 1) 先读缓存
        String cacheKey = "project03:dashboard:overview:" + userId;
        Map<String, Object> cached = readMapCache(cacheKey);
        if (cached != null) {
            return R.ok(cached);
        }

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
            writeCache(cacheKey, res);
            return R.ok(res);
        }

        HealthMetricRecord latest = lastTwo.get(0);
        HealthMetricRecord prev = lastTwo.size() > 1 ? lastTwo.get(1) : null;

        res.put("hasData", true);
        res.put("latest", latest);
        res.put("latestTime", latest.getRecordTime() == null ? null : latest.getRecordTime().format(TIME_FMT));

        // 对比差值
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

        // 最近7天统计
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        List<HealthMetricRecord> last7 = metricService.list(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .ge(HealthMetricRecord::getRecordTime, from)
                        .orderByAsc(HealthMetricRecord::getRecordTime)
        );

        Map<String, Object> stats7 = new HashMap<>();
        stats7.put("count", last7.size());
        stats7.put("avgSteps", avgSteps(last7));
        stats7.put("avgSleep", avgSleep(last7));
        stats7.put("avgBmi", avgBmi(last7));
        res.put("stats7days", stats7);

        // ✅ 2) 写缓存（60s）
        writeCache(cacheKey, res);

        return R.ok(res);
    }

    /**
     * 趋势数据：最近days天所有记录点（给 ECharts 直接画折线）
     * GET /api/dashboard/trend?days=30
     */
    @GetMapping("/trend")
    public R<List<DashboardTrendPointDTO>> trend(@RequestParam(defaultValue = "30") int days,
                                                 HttpServletRequest request) {

        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        if (days <= 0 || days > 365) {
            throw new BizException("days 参数不合理（1-365）");
        }

        // ✅ 1) 先读缓存
        String cacheKey = "project03:dashboard:trend:" + userId + ":" + days;
        List<DashboardTrendPointDTO> cached = readListCache(cacheKey);
        if (cached != null) {
            return R.ok(cached);
        }

        LocalDateTime from = LocalDateTime.now().minusDays(days);

        List<HealthMetricRecord> list = metricService.list(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .ge(HealthMetricRecord::getRecordTime, from)
                        .orderByAsc(HealthMetricRecord::getRecordTime)
        );

        List<DashboardTrendPointDTO> points = new ArrayList<>();
        for (HealthMetricRecord r : list) {
            DashboardTrendPointDTO p = new DashboardTrendPointDTO();

            if (r.getRecordTime() != null) {
                p.setTime(r.getRecordTime().format(TIME_FMT));
            }

            p.setBmi(r.getBmi());
            p.setWeightKg(r.getWeightKg());
            p.setSteps(r.getSteps());
            p.setSleepHours(r.getSleepHours());
            p.setSystolic(r.getSystolic());
            p.setDiastolic(r.getDiastolic());
            p.setBloodSugar(r.getBloodSugar());

            points.add(p);
        }

        // ✅ 2) 写缓存（60s）
        writeCache(cacheKey, points);

        return R.ok(points);
    }

    // ====================== Redis缓存（简单实用版） ======================

    private void writeCache(String key, Object obj) {
        try {
            String json = objectMapper.writeValueAsString(obj);
            stringRedisTemplate.opsForValue().set(key, json, 60, TimeUnit.SECONDS);
        } catch (Exception ignore) {
        }
    }

    private Map<String, Object> readMapCache(String key) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) return null;
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private List<DashboardTrendPointDTO> readListCache(String key) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) return null;
            return objectMapper.readValue(json, new TypeReference<List<DashboardTrendPointDTO>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    // ====================== 工具方法 ======================

    private BigDecimal diffBD(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return null;
        return a.subtract(b).setScale(2, RoundingMode.HALF_UP);
    }

    private Integer diffInt(Integer a, Integer b) {
        if (a == null || b == null) return null;
        return a - b;
    }

    // ✅ 平均步数
    private Integer avgSteps(List<HealthMetricRecord> list) {
        long sum = 0;
        int cnt = 0;
        for (HealthMetricRecord r : list) {
            if (r.getSteps() != null) {
                sum += r.getSteps();
                cnt++;
            }
        }
        if (cnt == 0) return null;
        return (int) (sum / cnt);
    }

    // ✅ 平均睡眠
    private BigDecimal avgSleep(List<HealthMetricRecord> list) {
        BigDecimal sum = BigDecimal.ZERO;
        int cnt = 0;
        for (HealthMetricRecord r : list) {
            if (r.getSleepHours() != null) {
                sum = sum.add(r.getSleepHours());
                cnt++;
            }
        }
        if (cnt == 0) return null;
        return sum.divide(new BigDecimal(cnt), 2, RoundingMode.HALF_UP);
    }

    // ✅ 平均BMI
    private BigDecimal avgBmi(List<HealthMetricRecord> list) {
        BigDecimal sum = BigDecimal.ZERO;
        int cnt = 0;
        for (HealthMetricRecord r : list) {
            if (r.getBmi() != null) {
                sum = sum.add(r.getBmi());
                cnt++;
            }
        }
        if (cnt == 0) return null;
        return sum.divide(new BigDecimal(cnt), 2, RoundingMode.HALF_UP);
    }
}