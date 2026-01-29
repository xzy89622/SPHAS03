package com.sphas.project03.service;

import java.util.Map;

/**
 * 推荐服务：根据 BMI 等级 + 维度得分输出今日推荐
 */
public interface RecommendService {

    /**
     * scores: {"SPORT":3,"DIET":2,"SLEEP":1,"STRESS":1}
     * 返回：diet + sport + reason
     */
    Map<String, Object> recommendToday(Long userId, Map<String, Integer> scores);
}

