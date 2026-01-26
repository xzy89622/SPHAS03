package com.sphas.project03.controller.dto;

import java.util.List;

/**
 * 周报返回对象
 */
public class WeeklyReportDTO {

    private String from;              // 开始日期
    private String to;                // 结束日期
    private Integer days;             // 统计天数

    private Double avgWeight;         // 平均体重
    private Integer avgSteps;         // 平均步数
    private Double avgSleepHours;     // 平均睡眠
    private String weightTrend;       // 体重趋势：上升/下降/稳定

    private Boolean bpRisk;           // 血压是否有风险
    private String summary;           // 一句话总结
    private List<String> suggestions; // 建议列表

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }

    public Double getAvgWeight() { return avgWeight; }
    public void setAvgWeight(Double avgWeight) { this.avgWeight = avgWeight; }

    public Integer getAvgSteps() { return avgSteps; }
    public void setAvgSteps(Integer avgSteps) { this.avgSteps = avgSteps; }

    public Double getAvgSleepHours() { return avgSleepHours; }
    public void setAvgSleepHours(Double avgSleepHours) { this.avgSleepHours = avgSleepHours; }

    public String getWeightTrend() { return weightTrend; }
    public void setWeightTrend(String weightTrend) { this.weightTrend = weightTrend; }

    public Boolean getBpRisk() { return bpRisk; }
    public void setBpRisk(Boolean bpRisk) { this.bpRisk = bpRisk; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
}

