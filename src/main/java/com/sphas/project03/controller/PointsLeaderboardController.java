package com.sphas.project03.controller;

import com.sphas.project03.common.R;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.service.PointsLeaderboardService;
import com.sphas.project03.service.SysUserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 积分总榜（Redis ZSET）
 */
@RestController
@RequestMapping("/api/points")
public class PointsLeaderboardController extends BaseController {

    private final StringRedisTemplate stringRedisTemplate;
    private final PointsLeaderboardService pointsLeaderboardService;
    private final SysUserService sysUserService;

    public PointsLeaderboardController(StringRedisTemplate stringRedisTemplate,
                                       PointsLeaderboardService pointsLeaderboardService,
                                       SysUserService sysUserService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.pointsLeaderboardService = pointsLeaderboardService;
        this.sysUserService = sysUserService;
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

        SysUser u = sysUserService.getById(userId);
        if (u != null) {
            r.setUsername(u.getUsername());
            r.setNickname(u.getNickname());
        }

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
        if (tuples == null || tuples.isEmpty()) return list;

        List<Long> userIds = new ArrayList<>();
        int i = 1;
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            if (t == null || !StringUtils.hasText(t.getValue())) continue;

            Long userId = Long.valueOf(t.getValue());

            RankItem item = new RankItem();
            item.setRank(i++);
            item.setUserId(userId);
            item.setTotalPoints(t.getScore() == null ? 0 : t.getScore().intValue());
            list.add(item);

            userIds.add(userId);
        }

        fillUserInfo(list, userIds);
        return list;
    }

    private void fillUserInfo(List<RankItem> list, List<Long> userIds) {
        if (list == null || list.isEmpty() || userIds == null || userIds.isEmpty()) return;

        List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(userIds));
        List<SysUser> users = sysUserService.listByIds(distinctIds);
        if (users == null || users.isEmpty()) return;

        Map<Long, SysUser> userMap = new HashMap<>();
        for (SysUser u : users) {
            if (u != null && u.getId() != null) {
                userMap.put(u.getId(), u);
            }
        }

        for (RankItem item : list) {
            if (item == null || item.getUserId() == null) continue;
            SysUser u = userMap.get(item.getUserId());
            if (u == null) continue;

            item.setUsername(u.getUsername());
            item.setNickname(u.getNickname());
        }
    }

    /**
     * 排行榜条目
     */
    public static class RankItem {
        private Integer rank;
        private Long userId;
        private Integer totalPoints;
        private String username;
        private String nickname;

        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Integer getTotalPoints() { return totalPoints; }
        public void setTotalPoints(Integer totalPoints) { this.totalPoints = totalPoints; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
    }

    /**
     * 我的排名返回
     */
    public static class MyRank {
        private Long userId;
        private Long rank; // 1开始；null=没上榜
        private Integer totalPoints;
        private String username;
        private String nickname;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Long getRank() { return rank; }
        public void setRank(Long rank) { this.rank = rank; }

        public Integer getTotalPoints() { return totalPoints; }
        public void setTotalPoints(Integer totalPoints) { this.totalPoints = totalPoints; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
    }
}