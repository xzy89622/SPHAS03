package com.sphas.project03.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户计划打卡
 */
@TableName("user_plan_checkin")
public class UserPlanCheckin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userPlanId;
    private Long userId;
    private LocalDate checkinDate;
    private Integer dietDone;
    private Integer sportDone;
    private String remark;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserPlanId() {
        return userPlanId;
    }

    public void setUserPlanId(Long userPlanId) {
        this.userPlanId = userPlanId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getCheckinDate() {
        return checkinDate;
    }

    public void setCheckinDate(LocalDate checkinDate) {
        this.checkinDate = checkinDate;
    }

    public Integer getDietDone() {
        return dietDone;
    }

    public void setDietDone(Integer dietDone) {
        this.dietDone = dietDone;
    }

    public Integer getSportDone() {
        return sportDone;
    }

    public void setSportDone(Integer sportDone) {
        this.sportDone = sportDone;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}