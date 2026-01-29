package com.sphas.project03.controller.dto;

import java.math.BigDecimal;

/**
 * 看板趋势点（给 ECharts 使用）
 * time: 时间点字符串（前端直接当 xAxis）
 * 各字段可为 null（表示这条记录没填该指标）
 */
public class DashboardTrendPointDTO {

    private String time;

    private BigDecimal bmi;
    private BigDecimal weightKg;

    private Integer steps;
    private BigDecimal sleepHours;

    private Integer systolic;
    private Integer diastolic;

    private BigDecimal bloodSugar;

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public BigDecimal getBmi() { return bmi; }
    public void setBmi(BigDecimal bmi) { this.bmi = bmi; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public Integer getSteps() { return steps; }
    public void setSteps(Integer steps) { this.steps = steps; }

    public BigDecimal getSleepHours() { return sleepHours; }
    public void setSleepHours(BigDecimal sleepHours) { this.sleepHours = sleepHours; }

    public Integer getSystolic() { return systolic; }
    public void setSystolic(Integer systolic) { this.systolic = systolic; }

    public Integer getDiastolic() { return diastolic; }
    public void setDiastolic(Integer diastolic) { this.diastolic = diastolic; }

    public BigDecimal getBloodSugar() { return bloodSugar; }
    public void setBloodSugar(BigDecimal bloodSugar) { this.bloodSugar = bloodSugar; }
}
