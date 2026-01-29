package com.sphas.project03.dto;

/**
 * AI 趋势预测 DTO（给前端展示）
 */
public class AiPredictionDTO {

    /** 趋势：UP / DOWN / FLAT */
    private String trend;

    /** 预测 7 天后的分数（0-100） */
    private Integer predictedScore;

    /** 预测 7 天后的等级：LOW / MID / HIGH */
    private String predictedLevel;

    /** 置信度（0~1） */
    private Double confidence;

    /** 一句话提示 */
    private String message;

    public String getTrend() { return trend; }
    public void setTrend(String trend) { this.trend = trend; }

    public Integer getPredictedScore() { return predictedScore; }
    public void setPredictedScore(Integer predictedScore) { this.predictedScore = predictedScore; }

    public String getPredictedLevel() { return predictedLevel; }
    public void setPredictedLevel(String predictedLevel) { this.predictedLevel = predictedLevel; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

