package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.RiskDashboardDTO;
import com.sphas.project03.entity.HealthRiskAlert;
import com.sphas.project03.entity.SysMessage;
import com.sphas.project03.service.HealthRiskAlertService;
import com.sphas.project03.service.RiskDashboardService;
import com.sphas.project03.service.RiskService;
import com.sphas.project03.service.SysMessageService;
import com.sphas.project03.utils.PrivacyUtil;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 健康风险预警（规则引擎版）
 */
@RestController
@RequestMapping("/api/risk")
public class RiskController extends BaseController {

    private final RiskService riskService;
    private final HealthRiskAlertService alertService;
    private final RiskDashboardService dashboardService;
    private final SysMessageService sysMessageService;

    public RiskController(RiskService riskService,
                          HealthRiskAlertService alertService,
                          RiskDashboardService dashboardService,
                          SysMessageService sysMessageService) {
        this.riskService = riskService;
        this.alertService = alertService;
        this.dashboardService = dashboardService;
        this.sysMessageService = sysMessageService;
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

        if (limit <= 0 || limit > 100) limit = 20;

        List<HealthRiskAlert> list = alertService.list(
                new LambdaQueryWrapper<HealthRiskAlert>()
                        .eq(HealthRiskAlert::getUserId, userId)
                        .orderByDesc(HealthRiskAlert::getCreateTime)
                        .last("limit " + limit)
        );

        if (list != null) {
            for (HealthRiskAlert a : list) {
                a.setReasonsJson(PrivacyUtil.decrypt(a.getReasonsJson()));
            }
        }

        return R.ok(list);
    }

    /**
     * 预警历史 + 顾问建议联动
     * 说明：
     * 1. 给每条风险记录补最近一条 ADVICE 消息
     * 2. 小程序风险页可以直接展示顾问建议
     * 3. 也能从风险页跳到消息详情
     */
    @GetMapping("/history-with-advice")
    public R<List<Map<String, Object>>> historyWithAdvice(@RequestParam(defaultValue = "20") int limit,
                                                          HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        if (limit <= 0 || limit > 100) limit = 20;

        List<HealthRiskAlert> alertList = alertService.list(
                new LambdaQueryWrapper<HealthRiskAlert>()
                        .eq(HealthRiskAlert::getUserId, userId)
                        .orderByDesc(HealthRiskAlert::getCreateTime)
                        .last("limit " + limit)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (HealthRiskAlert a : alertList) {
            if (a == null) {
                continue;
            }

            String reasonsJson = PrivacyUtil.decrypt(a.getReasonsJson());

            SysMessage adviceMsg = sysMessageService.getOne(
                    new LambdaQueryWrapper<SysMessage>()
                            .eq(SysMessage::getUserId, userId)
                            .eq(SysMessage::getType, "ADVICE")
                            .eq(SysMessage::getBizId, a.getId())
                            .orderByDesc(SysMessage::getCreateTime)
                            .last("limit 1"),
                    false
            );

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("riskLevel", a.getRiskLevel());
            item.put("riskScore", a.getRiskScore());
            item.put("reasonsJson", reasonsJson);
            item.put("advice", a.getAdvice());
            item.put("aiSummary", a.getAiSummary());
            item.put("createTime", a.getCreateTime());

            if (adviceMsg != null) {
                Map<String, Object> adviceInfo = new LinkedHashMap<>();
                adviceInfo.put("messageId", adviceMsg.getId());
                adviceInfo.put("title", adviceMsg.getTitle());
                adviceInfo.put("content", adviceMsg.getContent());
                adviceInfo.put("isRead", adviceMsg.getIsRead());
                adviceInfo.put("createTime", adviceMsg.getCreateTime());
                adviceInfo.put("readTime", adviceMsg.getReadTime());
                item.put("advisorAdvice", adviceInfo);
            } else {
                item.put("advisorAdvice", null);
            }

            result.add(item);
        }

        return R.ok(result);
    }
}