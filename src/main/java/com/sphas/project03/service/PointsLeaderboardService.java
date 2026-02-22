package com.sphas.project03.service;

/**
 * 积分榜服务（Redis缓存）
 */
public interface PointsLeaderboardService {

    /**
     * 增加用户积分（写入Redis）
     */
    void incrPoints(Long userId, int delta);

    /**
     * 重建积分榜（从DB汇总写入Redis）
     * 说明：Redis丢了可以用它恢复
     */
    void rebuildFromDb();
}