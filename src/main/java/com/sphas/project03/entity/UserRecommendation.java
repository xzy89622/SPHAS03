package com.sphas.project03.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户推荐结果（落库，便于历史查看/答辩展示）
 */
@TableName("user_recommendation")
public class UserRecommendation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private BigDecimal bmi;
    private String bmiLevel;
    private String scoresJson;

    private Long dietPlanId;
    private Long sportPlanId;

    private String reason;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getBmi() { return bmi; }
    public void setBmi(BigDecimal bmi) { this.bmi = bmi; }

    public String getBmiLevel() { return bmiLevel; }
    public void setBmiLevel(String bmiLevel) { this.bmiLevel = bmiLevel; }

    public String getScoresJson() { return scoresJson; }
    public void setScoresJson(String scoresJson) { this.scoresJson = scoresJson; }

    public Long getDietPlanId() { return dietPlanId; }
    public void setDietPlanId(Long dietPlanId) { this.dietPlanId = dietPlanId; }

    public Long getSportPlanId() { return sportPlanId; }
    public void setSportPlanId(Long sportPlanId) { this.sportPlanId = sportPlanId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}

