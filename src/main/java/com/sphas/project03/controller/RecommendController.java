package com.sphas.project03.controller;

import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.AssessmentEvaluateDTO;
import com.sphas.project03.service.RecommendService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 个性化推荐
 */
@RestController
@RequestMapping("/api/recommend")
public class RecommendController extends BaseController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    /**
     * 今日推荐：直接复用评估的 scores
     */
    @PostMapping("/today")
    public R<Map<String, Object>> today(@RequestBody AssessmentEvaluateDTO dto, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");
        Map<String, Object> res = recommendService.recommendToday(userId, dto.getScores());
        return R.ok(res);
    }
}

