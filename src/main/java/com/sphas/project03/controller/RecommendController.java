package com.sphas.project03.controller;

import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.AssessmentEvaluateDTO;
import com.sphas.project03.service.RecommendService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
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
     * 今日推荐（GET/POST 都支持）
     * - POST 可以带 scores（从评估页带过来）
     * - GET 不带 body，也能用默认 scores 生成推荐
     */
    @GetMapping("/today")
    public R<Map<String, Object>> todayGet(HttpServletRequest request) {
        // ✅ GET 没有 body，就传空 scores
        return todayPost(null, request);
    }

    @PostMapping("/today")
    public R<Map<String, Object>> todayPost(@RequestBody(required = false) AssessmentEvaluateDTO dto,
                                            HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        // ✅ dto 或 scores 允许为空，避免 NPE
        Map<String, Integer> scores = (dto == null || dto.getScores() == null)
                ? new HashMap<>()
                : dto.getScores();

        Map<String, Object> res = recommendService.recommendToday(userId, scores);
        return R.ok(res);
    }
}