package com.sphas.project03.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 积分流水表
 */
@TableName("point_record")
public class PointRecord {

    private Long id;
    private Long userId;           // 用户ID
    private Integer points;        // 积分（可正可负）
    private String type;           // 类型（如 CHALLENGE_FINISH）
    private Long bizId;            // 业务ID（如 challengeId）
    private String remark;         // 备注
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
