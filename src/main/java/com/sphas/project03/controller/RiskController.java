package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.RiskDashboardDTO;
import com.sphas.project03.entity.HealthRiskAlert;
import com.sphas.project03.service.HealthRiskAlertService;
import com.sphas.project03.service.RiskDashboardService;
import com.sphas.project03.service.RiskService;
import com.sphas.project03.utils.PrivacyUtil;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 健康风险预警（规则引擎版）
 */
@RestController
@RequestMapping("/api/risk")
public class RiskController extends BaseController {

    private final RiskService riskService;
    private final HealthRiskAlertService alertService;
    private final RiskDashboardService dashboardService;

    public RiskController(RiskService riskService,
                          HealthRiskAlertService alertService,
                          RiskDashboardService dashboardService) {
        this.riskService = riskService;
        this.alertService = alertService;
        this.dashboardService = dashboardService;
    }

    /**
     * 风险看板（旧接口，保留兼容）
     * GET /api/risk/dashboard/legacy?days=30
     */
    @GetMapping("/dashboard/legacy")
    public R<RiskDashboardDTO> dashboardLegacy(@RequestParam(defaultValue = "30") int days,
                                               HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        RiskDashboardDTO dto = dashboardService.dashboard(userId, days);
        return R.ok(dto);
    }

    /**
     * 立即评估一次并保存（用户主动点击“生成预警”）
     */
    @PostMapping("/evaluate")
    public R<Map<String, Object>> evaluate(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");
        return R.ok(riskService.evaluateAndSave(userId));
    }

    /**
     * 预警历史（最近N条）
     * GET /api/risk/history?limit=20
     */
    @GetMapping("/history")
    public R<List<HealthRiskAlert>> history(@RequestParam(defaultValue = "20") int limit,
                                            HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        // limit 合法性保护
        if (limit <= 0 || limit > 100) limit = 20;

        // 按时间倒序取最近N条
        List<HealthRiskAlert> list = alertService.list(
                new LambdaQueryWrapper<HealthRiskAlert>()
                        .eq(HealthRiskAlert::getUserId, userId)
                        .orderByDesc(HealthRiskAlert::getCreateTime)
                        .last("limit " + limit)
        );

        // reasons_json 是加密存储，这里解密后再返回给前端
        if (list != null) {
            for (HealthRiskAlert a : list) {
                a.setReasonsJson(PrivacyUtil.decrypt(a.getReasonsJson()));
            }
        }

        return R.ok(list);
    }
}
