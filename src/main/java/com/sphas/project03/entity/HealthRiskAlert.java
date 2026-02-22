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

    /** 触发原因 JSON（加密后存储也可以） */
    private String reasonsJson;

    /** 模拟区块Hash（数据指纹） */
    private String blockHash;

    /** 上一区块Hash（链式校验） */
    private String prevHash;

    /** 建议（展示给用户） */
    private String advice;

    /** 对应体质记录ID（可追溯） */
    private Long sourceRecordId;

    private LocalDateTime createTime;

    /** AI 风险解读文本 */
    private String aiSummary;

    /** AI 预测结果（JSON） */
    private String aiPredictionJson;

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

    public String getBlockHash() { return blockHash; }
    public void setBlockHash(String blockHash) { this.blockHash = blockHash; }

    public String getPrevHash() { return prevHash; }
    public void setPrevHash(String prevHash) { this.prevHash = prevHash; }

    public String getAdvice() { return advice; }
    public void setAdvice(String advice) { this.advice = advice; }

    public Long getSourceRecordId() { return sourceRecordId; }
    public void setSourceRecordId(Long sourceRecordId) { this.sourceRecordId = sourceRecordId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public String getAiPredictionJson() { return aiPredictionJson; }
    public void setAiPredictionJson(String aiPredictionJson) { this.aiPredictionJson = aiPredictionJson; }
}