package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.HealthMetricRecord;
import com.sphas.project03.service.HealthMetricRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 体质档案（多维指标动态记录）
 */
@RestController
@RequestMapping("/api/user/metrics")
public class HealthMetricRecordController extends BaseController {

    private final HealthMetricRecordService metricService;

    public HealthMetricRecordController(HealthMetricRecordService metricService) {
        this.metricService = metricService;
    }

    /**
     * 新增一次体质记录（recordTime可不传，默认当前时间）
     */
    @PostMapping
    public R<Long> add(@RequestBody HealthMetricRecord record, HttpServletRequest request) {

        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        record.setUserId(userId);
        if (record.getRecordTime() == null) {
            record.setRecordTime(LocalDateTime.now());
        }

        // ✅ 基本合理性校验（防止乱填）
        if (record.getHeightCm() != null &&
                (record.getHeightCm().compareTo(new BigDecimal("80")) < 0 ||
                        record.getHeightCm().compareTo(new BigDecimal("250")) > 0)) {
            throw new BizException("身高不合理");
        }
        if (record.getWeightKg() != null &&
                (record.getWeightKg().compareTo(new BigDecimal("20")) < 0 ||
                        record.getWeightKg().compareTo(new BigDecimal("300")) > 0)) {
            throw new BizException("体重不合理");
        }

        // ✅ 自动计算 BMI：bmi = weight / (height_m^2)
        if (record.getHeightCm() != null && record.getWeightKg() != null) {
            BigDecimal hMeter = record.getHeightCm().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal bmi = record.getWeightKg().divide(hMeter.multiply(hMeter), 2, RoundingMode.HALF_UP);
            record.setBmi(bmi);
        }

        metricService.save(record);
        return R.ok(record.getId());
    }

    /**
     * 最新一条记录
     */
    @GetMapping("/latest")
    public R<HealthMetricRecord> latest(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        HealthMetricRecord latest = metricService.getOne(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .orderByDesc(HealthMetricRecord::getRecordTime)
                        .last("limit 1")
        );
        return R.ok(latest);
    }

    /**
     * 区间列表（给 ECharts 用）
     */
    @GetMapping
    public R<List<HealthMetricRecord>> range(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime to,
            HttpServletRequest request) {

        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        List<HealthMetricRecord> list = metricService.list(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .ge(HealthMetricRecord::getRecordTime, from)
                        .le(HealthMetricRecord::getRecordTime, to)
                        .orderByAsc(HealthMetricRecord::getRecordTime)
        );
        return R.ok(list);
    }

    /**
     * 对比分析报告：最新 vs 上一次 + 近7天趋势（简单版）
     */
    @GetMapping("/report")
    public R<Map<String, Object>> report(HttpServletRequest request) {

        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        // 取最近2条记录做对比
        List<HealthMetricRecord> lastTwo = metricService.list(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .orderByDesc(HealthMetricRecord::getRecordTime)
                        .last("limit 2")
        );

        Map<String, Object> res = new HashMap<>();
        if (lastTwo.isEmpty()) {
            res.put("msg", "暂无体质记录");
            return R.ok(res);
        }

        HealthMetricRecord latest = lastTwo.get(0);
        HealthMetricRecord prev = lastTwo.size() > 1 ? lastTwo.get(1) : null;

        res.put("latest", latest);
        res.put("previous", prev);

        // 差值（只算常用指标）
        Map<String, Object> diff = new HashMap<>();
        if (prev != null) {
            diff.put("weightDiff", diffBD(latest.getWeightKg(), prev.getWeightKg()));
            diff.put("bmiDiff", diffBD(latest.getBmi(), prev.getBmi()));
            diff.put("systolicDiff", diffInt(latest.getSystolic(), prev.getSystolic()));
            diff.put("diastolicDiff", diffInt(latest.getDiastolic(), prev.getDiastolic()));
            diff.put("sugarDiff", diffBD(latest.getBloodSugar(), prev.getBloodSugar()));
        }
        res.put("diff", diff);

        // 近7天记录（给趋势图）
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<HealthMetricRecord> last7days = metricService.list(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .ge(HealthMetricRecord::getRecordTime, sevenDaysAgo)
                        .orderByAsc(HealthMetricRecord::getRecordTime)
        );
        res.put("last7days", last7days);

        return R.ok(res);
    }

    private BigDecimal diffBD(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return null;
        return a.subtract(b).setScale(2, RoundingMode.HALF_UP);
    }

    private Integer diffInt(Integer a, Integer b) {
        if (a == null || b == null) return null;
        return a - b;
    }
}

