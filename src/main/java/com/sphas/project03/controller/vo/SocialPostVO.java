package com.sphas.project03.controller.vo;

import java.time.LocalDateTime;

/**
 * 帖子列表VO（联表返回）
 */
public class SocialPostVO {
    private Long id;
    private Long userId;

    // sys_user
    private String nickname;
    private String avatar;

    // social_post
    private String content;
    private String imagesJson;
    private Integer status;
    private LocalDateTime createTime;

    // 统计
    private Long likeCount;
    private Long commentCount;

    // 当前用户是否点赞
    private Integer likedByMe;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImagesJson() { return imagesJson; }
    public void setImagesJson(String imagesJson) { this.imagesJson = imagesJson; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }

    public Long getCommentCount() { return commentCount; }
    public void setCommentCount(Long commentCount) { this.commentCount = commentCount; }

    public Integer getLikedByMe() { return likedByMe; }
    public void setLikedByMe(Integer likedByMe) { this.likedByMe = likedByMe; }
}