package com.sphas.project03.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 健康风险预警记录（规则引擎输出结果留存）
 */
@TableName("health_risk_alert")
public class HealthRiskAlert {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 风险等级：LOW/MID/HIGH */
    private String riskLevel;

    /** 风险分：0~100 */
    private Integer riskScore;

    /** 触发原因 JSON（例如：["血压偏高","BMI超重","血糖升高趋势"]） */
    private String reasonsJson;

    /** 建议（展示给用户） */
    private String advice;

    /** 对应体质记录ID（可追溯） */
    private Long sourceRecordId;

    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getReasonsJson() { return reasonsJson; }
    public void setReasonsJson(String reasonsJson) { this.reasonsJson = reasonsJson; }

    public String getAdvice() { return advice; }
    public void setAdvice(String advice) { this.advice = advice; }

    public Long getSourceRecordId() { return sourceRecordId; }
    public void setSourceRecordId(Long sourceRecordId) { this.sourceRecordId = sourceRecordId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}

