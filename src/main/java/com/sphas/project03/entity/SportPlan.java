package com.sphas.project03.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 运动方案（后台可配置）
 */
@TableName("sport_plan")
public class SportPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String bmiLevel;
    private String content;
    private String intensity; // LOW/MID/HIGH
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBmiLevel() { return bmiLevel; }
    public void setBmiLevel(String bmiLevel) { this.bmiLevel = bmiLevel; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getIntensity() { return intensity; }
    public void setIntensity(String intensity) { this.intensity = intensity; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
