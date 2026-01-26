package com.sphas.project03.controller;

import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.HealthRecordAddDTO;
import com.sphas.project03.entity.HealthRecord;
import com.sphas.project03.service.HealthRecordService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

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
    public R<Long> upsert(@RequestBody @Valid HealthRecordAddDTO dto, HttpServletRequest request) {
        Long userId = Long.valueOf(String.valueOf(request.getAttribute("userId"))); // 从token取用户ID
        Long id = healthRecordService.upsert(userId, dto);
        return R.ok(id);
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
}
