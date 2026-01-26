package com.sphas.project03.controller.dto;

/**
 * 健康分析结果
 */
public class HealthAnalysisDTO {

    // === 基础指标 ===
    private Double bmi;
    private String bmiLevel;        // 偏瘦/正常/超重/肥胖

    private String bloodPressure;   // 正常/偏高/高血压
    private Boolean bloodPressureRisk;

    private Boolean sleepEnough;    // 睡眠是否达标
    private Boolean stepsEnough;    // 步数是否达标

    // === 趋势 ===
    private String weightTrend;     // 上升/下降/稳定
    private String bpTrend;         // 稳定/波动

    // === 总体风险提示 ===
    private String riskSummary;     // 一句话总结

    // ===== getter / setter =====
    public Double getBmi() { return bmi; }
    public void setBmi(Double bmi) { this.bmi = bmi; }

    public String getBmiLevel() { return bmiLevel; }
    public void setBmiLevel(String bmiLevel) { this.bmiLevel = bmiLevel; }

    public String getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }

    public Boolean getBloodPressureRisk() { return bloodPressureRisk; }
    public void setBloodPressureRisk(Boolean bloodPressureRisk) { this.bloodPressureRisk = bloodPressureRisk; }

    public Boolean getSleepEnough() { return sleepEnough; }
    public void setSleepEnough(Boolean sleepEnough) { this.sleepEnough = sleepEnough; }

    public Boolean getStepsEnough() { return stepsEnough; }
    public void setStepsEnough(Boolean stepsEnough) { this.stepsEnough = stepsEnough; }

    public String getWeightTrend() { return weightTrend; }
    public void setWeightTrend(String weightTrend) { this.weightTrend = weightTrend; }

    public String getBpTrend() { return bpTrend; }
    public void setBpTrend(String bpTrend) { this.bpTrend = bpTrend; }

    public String getRiskSummary() { return riskSummary; }
    public void setRiskSummary(String riskSummary) { this.riskSummary = riskSummary; }
}

