package com.sphas.project03.controller;

import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.RiskDashboardDTO;
import com.sphas.project03.service.RiskDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 风险看板接口
 */
@RestController
@RequestMapping("/api/risk/dashboard")
public class RiskDashboardController extends BaseController {

    private final RiskDashboardService riskDashboardService;

    public RiskDashboardController(RiskDashboardService riskDashboardService) {
        this.riskDashboardService = riskDashboardService;
    }

    /**
     * 风险看板（含 AI 总结）
     * days：统计天数（7 / 30 / 90）
     */
    @GetMapping
    public R<RiskDashboardDTO> dashboard(@RequestParam(defaultValue = "30") int days,
                                         HttpServletRequest request) {

        // 从 JwtInterceptor 写入的 request attribute 取 userId
        Long userId = getUserId(request);
        if (userId == null) {
            throw new BizException("未登录或 token 缺失");
        }

        return R.ok(riskDashboardService.dashboard(userId, days));
    }
}
