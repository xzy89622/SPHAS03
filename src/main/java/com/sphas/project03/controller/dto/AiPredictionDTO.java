package com.sphas.project03.controller.dto;

import java.math.BigDecimal;

/**
 * AI预测返回DTO
 */
public class AiPredictionDTO {

    /**
     * 模型：PYTHON / FALLBACK
     */
    private String model;

    /**
     * 预测天数（例如 7）
     */
    private Integer horizonDays;

    /**
     * 预测风险分（0~100）
     */
    private Integer predictedRiskScore;

    /**
     * 预测体重（kg）
     */
    private BigDecimal predictedWeightKg;

    /**
     * 趋势：UP / DOWN / STABLE
     */
    private String trend;

    /**
     * 建议
     */
    private String suggestion;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getHorizonDays() {
        return horizonDays;
    }

    public void setHorizonDays(Integer horizonDays) {
        this.horizonDays = horizonDays;
    }

    public Integer getPredictedRiskScore() {
        return predictedRiskScore;
    }

    public void setPredictedRiskScore(Integer predictedRiskScore) {
        this.predictedRiskScore = predictedRiskScore;
    }

    public BigDecimal getPredictedWeightKg() {
        return predictedWeightKg;
    }

    public void setPredictedWeightKg(BigDecimal predictedWeightKg) {
        this.predictedWeightKg = predictedWeightKg;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
}