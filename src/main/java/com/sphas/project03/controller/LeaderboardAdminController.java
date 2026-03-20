package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.Challenge;
import com.sphas.project03.entity.ChallengeJoin;
import com.sphas.project03.entity.PointRecord;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.mapper.ChallengeJoinMapper;
import com.sphas.project03.mapper.PointRecordMapper;
import com.sphas.project03.service.ChallengeService;
import com.sphas.project03.service.SysUserService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端：排行榜查看
 */
@RestController
@RequestMapping("/api/leaderboard/admin")
public class LeaderboardAdminController extends BaseController {

    private final ChallengeJoinMapper challengeJoinMapper;
    private final PointRecordMapper pointRecordMapper;
    private final ChallengeService challengeService;
    private final SysUserService sysUserService;

    public LeaderboardAdminController(ChallengeJoinMapper challengeJoinMapper,
                                      PointRecordMapper pointRecordMapper,
                                      ChallengeService challengeService,
                                      SysUserService sysUserService) {
        this.challengeJoinMapper = challengeJoinMapper;
        this.pointRecordMapper = pointRecordMapper;
        this.challengeService = challengeService;
        this.sysUserService = sysUserService;
    }

    /**
     * 挑战下拉选项
     */
    @GetMapping("/challenge-options")
    public R<List<Map<String, Object>>> challengeOptions(HttpServletRequest request) {
        requireAdmin(request);

        List<Challenge> list = challengeService.list(
                new LambdaQueryWrapper<Challenge>().orderByDesc(Challenge::getId)
        );

        List<Map<String, Object>> res = list.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("title", item.getTitle());
            return map;
        }).collect(Collectors.toList());

        return R.ok(res);
    }

    /**
     * 挑战排行榜
     */
    @GetMapping("/challenge-rank")
    public R<Page<Map<String, Object>>> challengeRank(@RequestParam(defaultValue = "1") long pageNum,
                                                      @RequestParam(defaultValue = "10") long pageSize,
                                                      @RequestParam(required = false) Long challengeId,
                                                      @RequestParam(required = false) String keyword,
                                                      HttpServletRequest request) {
        requireAdmin(request);

        LambdaQueryWrapper<ChallengeJoin> qw = new LambdaQueryWrapper<>();
        if (challengeId != null) {
            qw.eq(ChallengeJoin::getChallengeId, challengeId);
        }

        List<ChallengeJoin> joins = challengeJoinMapper.selectList(qw);
        List<Map<String, Object>> rows = joins.stream()
                .map(this::buildChallengeRankRow)
                .filter(row -> matchKeyword(row, keyword))
                .sorted(this::compareChallengeRank)
                .collect(Collectors.toList());

        setRank(rows);

        return R.ok(toPage(rows, pageNum, pageSize));
    }

    /**
     * 积分排行榜
     */
    @GetMapping("/point-rank")
    public R<Page<Map<String, Object>>> pointRank(@RequestParam(defaultValue = "1") long pageNum,
                                                  @RequestParam(defaultValue = "10") long pageSize,
                                                  @RequestParam(required = false) String keyword,
                                                  HttpServletRequest request) {
        requireAdmin(request);

        List<PointRecord> records = pointRecordMapper.selectList(
                new LambdaQueryWrapper<PointRecord>().orderByDesc(PointRecord::getCreateTime)
        );

        Map<Long, Integer> pointMap = new HashMap<>();
        Map<Long, LocalDateTime> lastTimeMap = new HashMap<>();

        for (PointRecord item : records) {
            pointMap.merge(item.getUserId(), item.getPoints(), Integer::sum);

            LocalDateTime oldTime = lastTimeMap.get(item.getUserId());
            if (oldTime == null || (item.getCreateTime() != null && item.getCreateTime().isAfter(oldTime))) {
                lastTimeMap.put(item.getUserId(), item.getCreateTime());
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : pointMap.entrySet()) {
            Long userId = entry.getKey();
            Integer totalPoints = entry.getValue();

            SysUser user = sysUserService.getById(userId);

            Map<String, Object> row = new HashMap<>();
            row.put("userId", userId);
            row.put("username", user == null ? "" : user.getUsername());
            row.put("nickname", user == null ? "" : user.getNickname());
            row.put("phone", user == null ? "" : user.getPhone());
            row.put("totalPoints", totalPoints);
            row.put("lastPointTime", lastTimeMap.get(userId));

            rows.add(row);
        }

        rows = rows.stream()
                .filter(row -> matchKeyword(row, keyword))
                .sorted((a, b) -> {
                    Integer p1 = (Integer) a.getOrDefault("totalPoints", 0);
                    Integer p2 = (Integer) b.getOrDefault("totalPoints", 0);
                    int cmp = Integer.compare(p2, p1);
                    if (cmp != 0) return cmp;

                    LocalDateTime t1 = (LocalDateTime) a.get("lastPointTime");
                    LocalDateTime t2 = (LocalDateTime) b.get("lastPointTime");
                    if (t1 == null && t2 == null) return 0;
                    if (t1 == null) return 1;
                    if (t2 == null) return -1;
                    return t2.compareTo(t1);
                })
                .collect(Collectors.toList());

        setRank(rows);

        return R.ok(toPage(rows, pageNum, pageSize));
    }

    private Map<String, Object> buildChallengeRankRow(ChallengeJoin join) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", join.getId());
        row.put("challengeId", join.getChallengeId());
        row.put("userId", join.getUserId());
        row.put("progressValue", join.getProgressValue());
        row.put("finished", join.getFinished());
        row.put("finishTime", join.getFinishTime());
        row.put("createTime", join.getCreateTime());

        Challenge challenge = join.getChallengeId() == null ? null : challengeService.getById(join.getChallengeId());
        SysUser user = join.getUserId() == null ? null : sysUserService.getById(join.getUserId());

        row.put("challengeTitle", challenge == null ? "" : challenge.getTitle());
        row.put("challengeType", challenge == null ? "" : challenge.getType());
        row.put("targetValue", challenge == null ? null : challenge.getTargetValue());

        row.put("username", user == null ? "" : user.getUsername());
        row.put("nickname", user == null ? "" : user.getNickname());
        row.put("phone", user == null ? "" : user.getPhone());

        return row;
    }

    private int compareChallengeRank(Map<String, Object> a, Map<String, Object> b) {
        Integer f1 = (Integer) a.getOrDefault("finished", 0);
        Integer f2 = (Integer) b.getOrDefault("finished", 0);

        if (!f1.equals(f2)) {
            return Integer.compare(f2, f1);
        }

        Integer p1 = (Integer) a.getOrDefault("progressValue", 0);
        Integer p2 = (Integer) b.getOrDefault("progressValue", 0);
        int cmp = Integer.compare(p2, p1);
        if (cmp != 0) {
            return cmp;
        }

        LocalDateTime t1 = (LocalDateTime) a.get("finishTime");
        LocalDateTime t2 = (LocalDateTime) b.get("finishTime");

        if (t1 == null && t2 == null) return 0;
        if (t1 == null) return 1;
        if (t2 == null) return -1;
        return t1.compareTo(t2);
    }

    private boolean matchKeyword(Map<String, Object> row, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String kw = keyword.trim();
        String username = String.valueOf(row.getOrDefault("username", ""));
        String nickname = String.valueOf(row.getOrDefault("nickname", ""));
        String challengeTitle = String.valueOf(row.getOrDefault("challengeTitle", ""));
        return username.contains(kw) || nickname.contains(kw) || challengeTitle.contains(kw);
    }

    private void setRank(List<Map<String, Object>> rows) {
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).put("rankNo", i + 1);
        }
    }

    private Page<Map<String, Object>> toPage(List<Map<String, Object>> rows, long pageNum, long pageSize) {
        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        page.setTotal(rows.size());

        int fromIndex = (int) ((pageNum - 1) * pageSize);
        int toIndex = (int) Math.min(fromIndex + pageSize, rows.size());

        if (fromIndex >= rows.size()) {
            page.setRecords(new ArrayList<>());
        } else {
            page.setRecords(rows.subList(fromIndex, toIndex));
        }

        return page;
    }
}