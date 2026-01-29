package com.sphas.project03.controller;

import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.RiskDashboardDTO;
import com.sphas.project03.service.RiskDashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/risk/dashboard")
public class RiskDashboardController {

    private final RiskDashboardService riskDashboardService;

    public RiskDashboardController(RiskDashboardService riskDashboardService) {
        this.riskDashboardService = riskDashboardService;
    }

    /**
     * 风险看板（含 AI 总结）
     * days：统计天数（7 / 30 / 90）
     */
    @GetMapping
    public R<RiskDashboardDTO> dashboard(
            @RequestParam(defaultValue = "30") int days
    ) {
        Long userId = getCurrentUserId();
        return R.ok(riskDashboardService.dashboard(userId, days));
    }

    /**
     * 从 JWT 中取 userId
     */
    private Long getCurrentUserId() {
        // 你项目里已经有 BaseController 的话
        // 可以直接继承 BaseController 然后用 getUserId()
        return 1L; // TODO：替换成真实用户
    }
}

