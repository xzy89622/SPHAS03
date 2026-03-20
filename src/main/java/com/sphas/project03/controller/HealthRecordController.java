package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.HealthRecordAddDTO;
import com.sphas.project03.entity.HealthRecord;
import com.sphas.project03.service.HealthRecordService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康记录接口（需要登录）
 */
@RestController
@RequestMapping("/api/health")
@Validated
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    public HealthRecordController(HealthRecordService healthRecordService) {
        this.healthRecordService = healthRecordService;
    }

    @PostMapping("/record")
    public R<Map<String, Object>> upsert(@RequestBody @Valid HealthRecordAddDTO dto, HttpServletRequest request) {
        Long userId = Long.valueOf(String.valueOf(request.getAttribute("userId")));
        Long id = healthRecordService.upsert(userId, dto);

        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("recordDate", dto.getRecordDate());
        res.put("heightCm", dto.getHeightCm());
        res.put("weightKg", dto.getWeightKg());

        if (dto.getHeightCm() != null && dto.getWeightKg() != null) {
            BigDecimal bmi = calcBmi(dto.getHeightCm(), dto.getWeightKg());
            res.put("bmi", bmi);
        } else {
            res.put("bmi", null);
        }

        return R.ok(res);
    }

    @GetMapping("/latest")
    public R<List<HealthRecord>> latest(@RequestParam(defaultValue = "7") int limit,
                                        HttpServletRequest request) {
        Long userId = Long.valueOf(String.valueOf(request.getAttribute("userId")));
        return R.ok(healthRecordService.listLatest(userId, limit));
    }

    @GetMapping("/range")
    public R<List<HealthRecord>> range(@RequestParam String from,
                                       @RequestParam String to,
                                       HttpServletRequest request) {
        Long userId = Long.valueOf(String.valueOf(request.getAttribute("userId")));
        return R.ok(healthRecordService.listByDateRange(userId, from, to));
    }

    private BigDecimal calcBmi(Double heightCm, Double weightKg) {
        BigDecimal h = BigDecimal.valueOf(heightCm)
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(weightKg)
                .divide(h.multiply(h), 2, RoundingMode.HALF_UP);
    }
}