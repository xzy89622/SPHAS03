package com.sphas.project03.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户勋章
 */
@TableName("user_badge")
public class UserBadge {
    private Long id;
    private Long userId;
    private String badgeCode;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getBadgeCode() { return badgeCode; }
    public void setBadgeCode(String badgeCode) { this.badgeCode = badgeCode; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}