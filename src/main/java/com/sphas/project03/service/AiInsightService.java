package com.sphas.project03.service;

import com.sphas.project03.controller.dto.AiPredictionDTO;

import java.util.List;

/**
 * AI 洞察服务：解读（NLG） + 趋势预测（简化时间序列）
 */
public interface AiInsightService {

    /**
     * 生成 AI 解读文本（可解释 AI：基于 level/score/reasons/advice）
     */
    String buildSummary(String level, int score, List<String> reasons, String advice);

    /**
     * 基于历史评分趋势预测未来 7 天（简化时间序列）
     */
    AiPredictionDTO predict7Days(Long userId, int currentScore);
}
