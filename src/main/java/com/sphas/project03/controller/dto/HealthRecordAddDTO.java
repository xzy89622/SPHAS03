package com.sphas.project03.controller.dto;

import javax.validation.constraints.NotNull;

/**
 * 新增/更新健康记录入参
 */
public class HealthRecordAddDTO {

    @NotNull
    private String recordDate; // 日期：yyyy-MM-dd（前端更好传）

    private Double heightCm;
    private Double weightKg;
    private Integer systolic;
    private Integer diastolic;
    private Integer heartRate;
    private Integer steps;
    private Double sleepHours;
    private String remark;

    public String getRecordDate() { return recordDate; }
    public void setRecordDate(String recordDate) { this.recordDate = recordDate; }

    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public Integer getSystolic() { return systolic; }
    public void setSystolic(Integer systolic) { this.systolic = systolic; }

    public Integer getDiastolic() { return diastolic; }
    public void setDiastolic(Integer diastolic) { this.diastolic = diastolic; }

    public Integer getHeartRate() { return heartRate; }
    public void setHeartRate(Integer heartRate) { this.heartRate = heartRate; }

    public Integer getSteps() { return steps; }
    public void setSteps(Integer steps) { this.steps = steps; }

    public Double getSleepHours() { return sleepHours; }
    public void setSleepHours(Double sleepHours) { this.sleepHours = sleepHours; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}

