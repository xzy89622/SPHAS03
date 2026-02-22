package com.sphas.project03.service;

/**
 * 成就/勋章发放
 */
public interface AchievementService {

    /**
     * 某个用户完成挑战后检查发放
     */
    void onChallengeFinished(Long userId);

    /**
     * 某个用户积分变化后检查发放
     */
    void onPointsChanged(Long userId, int totalPoints);

    /**
     * 某个用户发帖后检查发放
     */
    void onPostCreated(Long userId);
}