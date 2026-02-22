package com.sphas.project03.controller;

import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.AiPredictionDTO;
import com.sphas.project03.service.AiInsightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * AI预测接口（给前端/小程序调用，也方便你Postman测试）
 */
@RestController
@RequestMapping("/api/ai")
public class AiController extends BaseController {

    private final AiInsightService aiInsightService;

    public AiController(AiInsightService aiInsightService) {
        this.aiInsightService = aiInsightService;
    }

    /**
     * 预测未来7天体重/风险趋势
     * GET /api/ai/predict7d?riskScore=xx
     */
    @GetMapping("/predict7d")
    public R<AiPredictionDTO> predict7d(@RequestParam(required = false) Integer riskScore,
                                        HttpServletRequest request) {

        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");
        if (riskScore == null) riskScore = 0;

        return R.ok(aiInsightService.predict7Days(userId, riskScore));
    }
}