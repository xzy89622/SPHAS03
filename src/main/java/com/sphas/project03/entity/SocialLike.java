package com.sphas.project03.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 点赞
 */
@TableName("social_like")
public class SocialLike {

    private Long id;
    private Long postId;
    private Long userId;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}