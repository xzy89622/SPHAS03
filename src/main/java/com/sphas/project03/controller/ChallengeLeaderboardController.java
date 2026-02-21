package com.sphas.project03.controller;

import com.sphas.project03.common.R;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 挑战排行榜（Redis ZSET）
 * 说明：
 * 1) 进度榜：score=progressValue
 * 2) 完成榜：score=finishTimestamp（用于排序）
 */
@RestController
@RequestMapping("/api/challenge/leaderboard")
public class ChallengeLeaderboardController extends BaseController {

    private final StringRedisTemplate stringRedisTemplate;

    public ChallengeLeaderboardController(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 进度榜TopN（不强制登录）
     */
    @GetMapping("/progress/top")
    public R<List<RankItem>> progressTop(@RequestParam Long challengeId,
                                         @RequestParam(defaultValue = "10") int topN) {

        String key = redisKeyProgress(challengeId);

        // 从大到小取TopN
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, topN - 1);

        return R.ok(convertTuples(tuples));
    }

    /**
     * 完成榜TopN（不强制登录）
     */
    @GetMapping("/finish/top")
    public R<List<RankItem>> finishTop(@RequestParam Long challengeId,
                                       @RequestParam(defaultValue = "10") int topN) {

        String key = redisKeyFinish(challengeId);

        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().rangeWithScores(key, 0, topN - 1);

        // 完成榜用时间戳从小到大（越早完成越靠前）
        return R.ok(convertTuples(tuples));
    }

    /**
     * 我的进度排名（需要登录）
     */
    @GetMapping("/progress/me")
    public R<MyRank> progressMe(@RequestParam Long challengeId, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return R.fail("未登录或 token 缺失");

        String key = redisKeyProgress(challengeId);

        // reverseRank：分数越大排名越靠前（0是第一名）
        Long rank0 = stringRedisTemplate.opsForZSet().reverseRank(key, String.valueOf(userId));
        Double score = stringRedisTemplate.opsForZSet().score(key, String.valueOf(userId));

        MyRank r = new MyRank();
        r.setUserId(userId);
        r.setRank(rank0 == null ? null : rank0 + 1);
        r.setScore(score == null ? 0 : score.intValue());

        return R.ok(r);
    }

    /**
     * 我的完成情况（需要登录）
     */
    @GetMapping("/finish/me")
    public R<MyRank> finishMe(@RequestParam Long challengeId, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return R.fail("未登录或 token 缺失");

        String key = redisKeyFinish(challengeId);

        Long rank0 = stringRedisTemplate.opsForZSet().rank(key, String.valueOf(userId));
        Double score = stringRedisTemplate.opsForZSet().score(key, String.valueOf(userId));

        MyRank r = new MyRank();
        r.setUserId(userId);
        r.setRank(rank0 == null ? null : rank0 + 1);
        // score 是完成时间戳，这里返回 1/0 表示是否完成更直观
        r.setScore(score == null ? 0 : 1);

        return R.ok(r);
    }

    // =========================
    // 工具方法
    // =========================

    private String redisKeyProgress(Long challengeId) {
        return "project03:challenge:" + challengeId + ":progress";
    }

    private String redisKeyFinish(Long challengeId) {
        return "project03:challenge:" + challengeId + ":finish";
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
            item.setScore(t.getScore() == null ? 0 : t.getScore().intValue());
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
        private Integer score;

        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
    }

    /**
     * 我的排名返回
     */
    public static class MyRank {
        private Long userId;
        private Long rank;   // 1开始；null=没上榜
        private Integer score;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Long getRank() { return rank; }
        public void setRank(Long rank) { this.rank = rank; }

        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
    }
}