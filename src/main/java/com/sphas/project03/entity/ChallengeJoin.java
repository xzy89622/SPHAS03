package com.sphas.project03.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 挑战报名表
 */
@TableName("challenge_join")
public class ChallengeJoin {

    private Long id;
    private Long challengeId;        // 挑战ID
    private Long userId;             // 用户ID
    private Integer progressValue;   // 当前进度
    private Integer finished;        // 0未完成 1已完成
    private LocalDateTime finishTime;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getChallengeId() { return challengeId; }
    public void setChallengeId(Long challengeId) { this.challengeId = challengeId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getProgressValue() { return progressValue; }
    public void setProgressValue(Integer progressValue) { this.progressValue = progressValue; }

    public Integer getFinished() { return finished; }
    public void setFinished(Integer finished) { this.finished = finished; }

    public LocalDateTime getFinishTime() { return finishTime; }
    public void setFinishTime(LocalDateTime finishTime) { this.finishTime = finishTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
