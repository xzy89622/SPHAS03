package com.sphas.project03.controller;

import com.sphas.project03.common.R;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.service.SysUserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 挑战排行榜（Redis ZSET）
 * 说明：
 * 1) 进度榜：score = progressValue
 * 2) 完成榜：score = 完成时间戳（越早完成越靠前）
 */
@RestController
@RequestMapping("/api/challenge/leaderboard")
public class ChallengeLeaderboardController extends BaseController {

    private final StringRedisTemplate stringRedisTemplate;
    private final SysUserService sysUserService;

    public ChallengeLeaderboardController(StringRedisTemplate stringRedisTemplate,
                                          SysUserService sysUserService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.sysUserService = sysUserService;
    }

    /**
     * 进度榜 TopN（不强制登录）
     */
    @GetMapping("/progress/top")
    public R<List<RankItem>> progressTop(@RequestParam Long challengeId,
                                         @RequestParam(defaultValue = "10") int topN) {

        String key = redisKeyProgress(challengeId);

        // 从大到小取 TopN
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, topN - 1);

        return R.ok(convertProgressTuples(tuples));
    }

    /**
     * 完成榜 TopN（不强制登录）
     */
    @GetMapping("/finish/top")
    public R<List<RankItem>> finishTop(@RequestParam Long challengeId,
                                       @RequestParam(defaultValue = "10") int topN) {

        String key = redisKeyFinish(challengeId);

        // 完成榜按时间从小到大，越早完成越靠前
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().rangeWithScores(key, 0, topN - 1);

        return R.ok(convertFinishTuples(tuples));
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
     * 我的完成排名（需要登录）
     */
    @GetMapping("/finish/me")
    public R<MyRank> finishMe(@RequestParam Long challengeId, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return R.fail("未登录或 token 缺失");

        String key = redisKeyFinish(challengeId);

        // 完成榜按时间从小到大
        Long rank0 = stringRedisTemplate.opsForZSet().rank(key, String.valueOf(userId));
        Double score = stringRedisTemplate.opsForZSet().score(key, String.valueOf(userId));

        MyRank r = new MyRank();
        r.setUserId(userId);
        r.setRank(rank0 == null ? null : rank0 + 1);
        // 是否完成：1已完成，0未完成
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

    /**
     * 进度榜转换
     */
    private List<RankItem> convertProgressTuples(Set<ZSetOperations.TypedTuple<String>> tuples) {
        List<RankItem> list = new ArrayList<>();
        if (tuples == null) return list;

        int i = 1;
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            if (t == null || !StringUtils.hasText(t.getValue())) continue;

            Long userId = Long.valueOf(t.getValue());
            SysUser user = sysUserService.getById(userId);

            RankItem item = new RankItem();
            item.setRank(i++);
            item.setUserId(userId);
            item.setNickname(user == null || !StringUtils.hasText(user.getNickname())
                    ? null
                    : user.getNickname());

            int progressValue = t.getScore() == null ? 0 : t.getScore().intValue();
            item.setScore(progressValue);
            item.setProgressValue(progressValue);

            list.add(item);
        }
        return list;
    }

    /**
     * 完成榜转换
     */
    private List<RankItem> convertFinishTuples(Set<ZSetOperations.TypedTuple<String>> tuples) {
        List<RankItem> list = new ArrayList<>();
        if (tuples == null) return list;

        int i = 1;
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            if (t == null || !StringUtils.hasText(t.getValue())) continue;

            Long userId = Long.valueOf(t.getValue());
            SysUser user = sysUserService.getById(userId);

            RankItem item = new RankItem();
            item.setRank(i++);
            item.setUserId(userId);
            item.setNickname(user == null || !StringUtils.hasText(user.getNickname())
                    ? null
                    : user.getNickname());

            int score = t.getScore() == null ? 0 : t.getScore().intValue();
            item.setScore(score);
            item.setFinished(true);
            item.setFinishTime(formatTimestamp(t.getScore()));

            list.add(item);
        }
        return list;
    }

    /**
     * 时间戳转字符串
     */
    private String formatTimestamp(Double score) {
        if (score == null) return "已完成";

        long ts = score.longValue();
        if (ts <= 0) return "已完成";

        LocalDateTime time = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(ts),
                ZoneId.systemDefault()
        );
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /**
     * 排行榜条目
     */
    public static class RankItem {
        private Integer rank;
        private Long userId;
        private String nickname;

        // 兼容原逻辑保留
        private Integer score;

        // 前端进度榜要用
        private Integer progressValue;

        // 前端完成榜要用
        private String finishTime;

        // 是否完成（给后面扩展用）
        private Boolean finished;

        public Integer getRank() {
            return rank;
        }

        public void setRank(Integer rank) {
            this.rank = rank;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public Integer getProgressValue() {
            return progressValue;
        }

        public void setProgressValue(Integer progressValue) {
            this.progressValue = progressValue;
        }

        public String getFinishTime() {
            return finishTime;
        }

        public void setFinishTime(String finishTime) {
            this.finishTime = finishTime;
        }

        public Boolean getFinished() {
            return finished;
        }

        public void setFinished(Boolean finished) {
            this.finished = finished;
        }
    }

    /**
     * 我的排名返回
     */
    public static class MyRank {
        private Long userId;
        private Long rank;   // 1开始；null=没上榜
        private Integer score;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getRank() {
            return rank;
        }

        public void setRank(Long rank) {
            this.rank = rank;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }
    }
}