package com.sphas.project03.serviceImpl;

import com.sphas.project03.mapper.PointRecordMapper;
import com.sphas.project03.service.PointsLeaderboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 积分榜服务实现
 */
@Service
public class PointsLeaderboardServiceImpl implements PointsLeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(PointsLeaderboardServiceImpl.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final PointRecordMapper pointRecordMapper;

    public PointsLeaderboardServiceImpl(StringRedisTemplate stringRedisTemplate,
                                        PointRecordMapper pointRecordMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.pointRecordMapper = pointRecordMapper;
    }

    @Override
    public void incrPoints(Long userId, int delta) {
        if (userId == null) return;

        try {
            // ZINCRBY key delta member
            stringRedisTemplate.opsForZSet().incrementScore(redisKeyTotal(), String.valueOf(userId), delta);
        } catch (Exception e) {
            // Redis挂了不影响主流程（DB里积分流水仍然是对的）
            log.warn("积分榜Redis累加失败（不影响业务），userId={}, delta={}, err={}", userId, delta, e.getMessage());
        }
    }

    @Override
    public void rebuildFromDb() {
        try {
            // 先清空
            stringRedisTemplate.delete(redisKeyTotal());

            // 从DB汇总：user_id -> total_points
            List<Map<String, Object>> rows = pointRecordMapper.sumPointsGroupByUser();
            if (rows == null || rows.isEmpty()) return;

            for (Map<String, Object> r : rows) {
                Object uidObj = r.get("user_id");
                Object totalObj = r.get("total_points");
                if (uidObj == null || totalObj == null) continue;

                long userId = ((Number) uidObj).longValue();
                double total = ((Number) totalObj).doubleValue();

                stringRedisTemplate.opsForZSet().add(redisKeyTotal(), String.valueOf(userId), total);
            }
        } catch (Exception e) {
            log.warn("重建积分榜失败，err={}", e.getMessage());
        }
    }

    private String redisKeyTotal() {
        return "project03:points:total";
    }
}