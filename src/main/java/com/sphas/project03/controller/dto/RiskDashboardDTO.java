package com.sphas.project03.controller.dto;

import java.util.List;
import java.util.Map;

/**
 * 风险看板 DTO（给前端画图用）
 */
public class RiskDashboardDTO {

    /** 查询天数 */
    private Integer days;

    /** 近N天预警总数 */
    private Long total;

    /**
     * 各等级数量：{"LOW": 3, "MID": 2, "HIGH": 1}
     */
    private Map<String, Long> levelCounts;

    /**
     * 各等级占比(0~1)：{"LOW": 0.5, "MID": 0.33, "HIGH": 0.17}
     */
    private Map<String, Double> levelRatio;

    /**
     * 趋势数据：每天统计（前端 xAxis 用 date）
     * 例：
     * [
     *  {"date":"2026-01-20","LOW":1,"MID":0,"HIGH":0,"total":1},
     *  {"date":"2026-01-21","LOW":0,"MID":1,"HIGH":1,"total":2}
     * ]
     */
    private List<Map<String, Object>> daily;

    /** 最近一次 HIGH 风险时间（字符串即可，前端展示用） */
    private String latestHighTime;
    /**
     * AI 总结（风险看板自然语言总结）
     */
    private String aiConclusion;

    public String getAiConclusion() {
        return aiConclusion;
    }

    public void setAiConclusion(String aiConclusion) {
        this.aiConclusion = aiConclusion;
    }

    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }

    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }

    public Map<String, Long> getLevelCounts() { return levelCounts; }
    public void setLevelCounts(Map<String, Long> levelCounts) { this.levelCounts = levelCounts; }

    public Map<String, Double> getLevelRatio() { return levelRatio; }
    public void setLevelRatio(Map<String, Double> levelRatio) { this.levelRatio = levelRatio; }

    public List<Map<String, Object>> getDaily() { return daily; }
    public void setDaily(List<Map<String, Object>> daily) { this.daily = daily; }

    public String getLatestHighTime() { return latestHighTime; }
    public void setLatestHighTime(String latestHighTime) { this.latestHighTime = latestHighTime; }
}
