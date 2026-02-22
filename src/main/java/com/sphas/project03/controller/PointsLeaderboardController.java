package com.sphas.project03.controller;

import com.sphas.project03.common.R;
import com.sphas.project03.service.PointsLeaderboardService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 积分总榜（Redis ZSET）
 */
@RestController
@RequestMapping("/api/points")
public class PointsLeaderboardController extends BaseController {

    private final StringRedisTemplate stringRedisTemplate;
    private final PointsLeaderboardService pointsLeaderboardService;

    public PointsLeaderboardController(StringRedisTemplate stringRedisTemplate,
                                       PointsLeaderboardService pointsLeaderboardService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.pointsLeaderboardService = pointsLeaderboardService;
    }

    /**
     * 总积分Top榜（不强制登录）
     */
    @GetMapping("/leaderboard/top")
    public R<List<RankItem>> top(@RequestParam(defaultValue = "10") int topN) {

        String key = redisKeyTotal();

        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, topN - 1);

        return R.ok(convertTuples(tuples));
    }

    /**
     * 我的总积分与排名（需要登录）
     */
    @GetMapping("/leaderboard/me")
    public R<MyRank> me(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return R.fail("未登录或 token 缺失");

        String key = redisKeyTotal();

        Long rank0 = stringRedisTemplate.opsForZSet().reverseRank(key, String.valueOf(userId));
        Double score = stringRedisTemplate.opsForZSet().score(key, String.valueOf(userId));

        MyRank r = new MyRank();
        r.setUserId(userId);
        r.setRank(rank0 == null ? null : rank0 + 1);
        r.setTotalPoints(score == null ? 0 : score.intValue());

        return R.ok(r);
    }

    /**
     * 管理端：重建积分榜缓存（从DB汇总）
     */
    @PostMapping("/admin/rebuild")
    public R<Boolean> rebuild(HttpServletRequest request) {
        requireAdmin(request);
        pointsLeaderboardService.rebuildFromDb();
        return R.ok(true);
    }

    // =========================
    // 工具方法
    // =========================

    private String redisKeyTotal() {
        return "project03:points:total";
    }

    private List<RankItem> convertTuples(Set<ZSetOperations.TypedTuple<String>> tuples) {
        List<RankItem> list = new ArrayList<>();
        if (tuples == null) return list;

        int i = 1;
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            if (t == null || !StringUtils.hasText(t.getValue())) continue;

            RankItem item = new RankItem();
            item.setRank(i++);
            item.setUserId(Long.valueOf(t.getValue()));
            item.setTotalPoints(t.getScore() == null ? 0 : t.getScore().intValue());
            list.add(item);
        }
        return list;
    }

    /**
     * 排行榜条目
     */
    public static class RankItem {
        private Integer rank;
        private Long userId;
        private Integer totalPoints;

        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Integer getTotalPoints() { return totalPoints; }
        public void setTotalPoints(Integer totalPoints) { this.totalPoints = totalPoints; }
    }

    /**
     * 我的排名返回
     */
    public static class MyRank {
        private Long userId;
        private Long rank; // 1开始；null=没上榜
        private Integer totalPoints;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Long getRank() { return rank; }
        public void setRank(Long rank) { this.rank = rank; }

        public Integer getTotalPoints() { return totalPoints; }
        public void setTotalPoints(Integer totalPoints) { this.totalPoints = totalPoints; }
    }
}