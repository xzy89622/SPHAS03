package com.sphas.project03.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 社交日志/帖子
 * status：
 * 2待审核 1通过 3驳回 0删除/隐藏
 */
@TableName("social_post")
public class SocialPost {

    private Long id;
    private Long userId;
    private String content;
    private String imagesJson;
    private Integer likeCount;
    private Integer commentCount;

    private Integer status;          // 2待审 1通过 3驳回 0隐藏/删除
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImagesJson() { return imagesJson; }
    public void setImagesJson(String imagesJson) { this.imagesJson = imagesJson; }

    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}