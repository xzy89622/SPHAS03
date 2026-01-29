package com.sphas.project03.controller.dto;

import java.util.Map;

/**
 * 用户提交的健康评估数据
 * scores: 每个维度的得分（前端可按题目score相加后传）
 * 例：{"SPORT":3,"SLEEP":1,"DIET":2}
 */
public class AssessmentEvaluateDTO {

    private Map<String, Integer> scores;

    public Map<String, Integer> getScores() {
        return scores;
    }

    public void setScores(Map<String, Integer> scores) {
        this.scores = scores;
    }
}
