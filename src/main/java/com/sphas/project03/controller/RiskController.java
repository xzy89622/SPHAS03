package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.HealthRiskAlert;
import com.sphas.project03.service.HealthRiskAlertService;
import com.sphas.project03.service.RiskService;
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

    public RiskController(RiskService riskService,
                          HealthRiskAlertService alertService) {
        this.riskService = riskService;
        this.alertService = alertService;
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
     */
    @GetMapping("/history")
    public R<List<HealthRiskAlert>> history(@RequestParam(defaultValue = "20") int limit,
                                            HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        if (limit <= 0 || limit > 100) limit = 20;

        List<HealthRiskAlert> list = alertService.list(
                new LambdaQueryWrapper<HealthRiskAlert>()
                        .eq(HealthRiskAlert::getUserId, userId)
                        .orderByDesc(HealthRiskAlert::getCreateTime)
                        .last("limit " + limit)
        );
        return R.ok(list);
    }
}
