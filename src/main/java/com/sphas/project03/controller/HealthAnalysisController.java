package com.sphas.project03.controller;

import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.HealthAnalysisDTO;
import com.sphas.project03.service.HealthAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 健康分析接口（需要登录）
 */
@RestController
@RequestMapping("/api/health/analysis")
public class HealthAnalysisController {

    private final HealthAnalysisService healthAnalysisService;

    public HealthAnalysisController(HealthAnalysisService healthAnalysisService) {
        this.healthAnalysisService = healthAnalysisService;
    }

    @GetMapping("/latest")
    public R<HealthAnalysisDTO> latest(HttpServletRequest request) {
        Long userId = Long.valueOf(String.valueOf(request.getAttribute("userId")));
        return R.ok(healthAnalysisService.analyzeLatest(userId));
    }
}

