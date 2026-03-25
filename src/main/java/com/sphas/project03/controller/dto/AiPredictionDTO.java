package com.sphas.project03.controller.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI预测返回DTO
 */
public class AiPredictionDTO {

    /**
     * 模型：PYTHON / FALLBACK
     */
    private String model;

    /**
     * 预测天数
     */
    private Integer horizonDays;

    /**
     * 预测风险分（0~100）
     */
    private Integer predictedRiskScore;

    /**
     * 预测风险等级：LOW / MID / HIGH
     */
    private String predictedLevel;

    /**
     * 预测体重（kg）
     */
    private BigDecimal predictedWeightKg;

    /**
     * 趋势：UP / DOWN / STABLE
     */
    private String trend;

    /**
     * 置信度（0~1）
     */
    private BigDecimal confidence;

    /**
     * 历史样本数
     */
    private Integer historyCount;

    /**
     * 预测结论
     */
    private String message;

    /**
     * 建议
     */
    private String suggestion;

    /**
     * 预测依据
     */
    private List<String> basis;

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

    public String getPredictedLevel() {
        return predictedLevel;
    }

    public void setPredictedLevel(String predictedLevel) {
        this.predictedLevel = predictedLevel;
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

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public Integer getHistoryCount() {
        return historyCount;
    }

    public void setHistoryCount(Integer historyCount) {
        this.historyCount = historyCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public List<String> getBasis() {
        return basis;
    }

    public void setBasis(List<String> basis) {
        this.basis = basis;
    }
}