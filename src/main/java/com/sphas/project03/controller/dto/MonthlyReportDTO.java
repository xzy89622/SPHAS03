package com.sphas.project03.controller.dto;

import java.util.List;

/**
 * 月报DTO（最近30天）
 * 注：字段和 WeeklyReportDTO 保持一致，前端用起来更省事。
 */
public class MonthlyReportDTO {

    private String from;
    private String to;

    private Integer days;
    private Double avgWeight;
    private Integer avgSteps;
    private Double avgSleepHours;

    private String weightTrend;
    private Boolean bpRisk;

    private List<String> suggestions;
    private String summary;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public Double getAvgWeight() {
        return avgWeight;
    }

    public void setAvgWeight(Double avgWeight) {
        this.avgWeight = avgWeight;
    }

    public Integer getAvgSteps() {
        return avgSteps;
    }

    public void setAvgSteps(Integer avgSteps) {
        this.avgSteps = avgSteps;
    }

    public Double getAvgSleepHours() {
        return avgSleepHours;
    }

    public void setAvgSleepHours(Double avgSleepHours) {
        this.avgSleepHours = avgSleepHours;
    }

    public String getWeightTrend() {
        return weightTrend;
    }

    public void setWeightTrend(String weightTrend) {
        this.weightTrend = weightTrend;
    }

    public Boolean getBpRisk() {
        return bpRisk;
    }

    public void setBpRisk(Boolean bpRisk) {
        this.bpRisk = bpRisk;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}