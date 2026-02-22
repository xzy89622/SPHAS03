package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.Challenge;
import com.sphas.project03.entity.ChallengeJoin;
import com.sphas.project03.entity.PointRecord;
import com.sphas.project03.service.AchievementService;
import com.sphas.project03.service.ChallengeJoinService;
import com.sphas.project03.service.ChallengeService;
import com.sphas.project03.service.PointRecordService;
import com.sphas.project03.service.PointsLeaderboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 挑战模块
 */
@RestController
@RequestMapping("/api/challenge")
public class ChallengeController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(ChallengeController.class);

    private final ChallengeService challengeService;
    private final ChallengeJoinService joinService;
    private final PointRecordService pointRecordService;

    private final StringRedisTemplate stringRedisTemplate;
    private final PointsLeaderboardService pointsLeaderboardService;
    private final AchievementService achievementService;

    public ChallengeController(ChallengeService challengeService,
                               ChallengeJoinService joinService,
                               PointRecordService pointRecordService,
                               StringRedisTemplate stringRedisTemplate,
                               PointsLeaderboardService pointsLeaderboardService,
                               AchievementService achievementService) {
        this.challengeService = challengeService;
        this.joinService = joinService;
        this.pointRecordService = pointRecordService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.pointsLeaderboardService = pointsLeaderboardService;
        this.achievementService = achievementService;
    }

    // =========================
    // 用户端接口
    // =========================

    /**
     * 挑战分页（用户端只看上架 status=1）
     */
    @GetMapping("/page")
    public R<Page<Challenge>> page(@RequestParam(defaultValue = "1") long pageNum,
                                   @RequestParam(defaultValue = "10") long pageSize,
                                   @RequestParam(required = false) String type) {

        Page<Challenge> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Challenge> qw = new LambdaQueryWrapper<>();
        qw.eq(Challenge::getStatus, 1);
        if (StringUtils.hasText(type)) {
            qw.eq(Challenge::getType, type);
        }
        qw.orderByDesc(Challenge::getCreateTime);

        return R.ok(challengeService.page(page, qw));
    }

    /**
     * 挑战详情（带我是否报名 + 我的进度）
     */
    @GetMapping("/detail/{challengeId}")
    public R<ChallengeDetailVO> detail(@PathVariable Long challengeId, HttpServletRequest request) {
        Challenge ch = challengeService.getById(challengeId);
        if (ch == null || ch.getStatus() == 0) {
            throw new BizException("挑战不存在或已下架");
        }

        Long userId = getUserId(request);

        ChallengeDetailVO vo = new ChallengeDetailVO();
        vo.setChallenge(ch);

        // 未登录也能看详情
        if (userId == null) {
            vo.setJoined(false);
            vo.setMyProgress(0);
            vo.setFinished(false);
            return R.ok(vo);
        }

        ChallengeJoin join = joinService.getOne(new LambdaQueryWrapper<ChallengeJoin>()
                .eq(ChallengeJoin::getChallengeId, challengeId)
                .eq(ChallengeJoin::getUserId, userId));

        if (join == null) {
            vo.setJoined(false);
            vo.setMyProgress(0);
            vo.setFinished(false);
        } else {
            vo.setJoined(true);
            vo.setMyProgress(join.getProgressValue() == null ? 0 : join.getProgressValue());
            vo.setFinished(join.getFinished() != null && join.getFinished() == 1);
        }

        return R.ok(vo);
    }

    /**
     * 报名挑战（报名后写入Redis进度榜）
     */
    @PostMapping("/join/{challengeId}")
    public R<Boolean> join(@PathVariable Long challengeId, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        Challenge ch = challengeService.getById(challengeId);
        if (ch == null || ch.getStatus() == 0) throw new BizException("挑战不存在或已下架");

        LocalDate today = LocalDate.now();
        if (today.isBefore(ch.getStartDate())) throw new BizException("挑战未开始，暂不可报名");
        if (today.isAfter(ch.getEndDate())) throw new BizException("挑战已结束，无法报名");

        ChallengeJoin exist = joinService.getOne(new LambdaQueryWrapper<ChallengeJoin>()
                .eq(ChallengeJoin::getChallengeId, challengeId)
                .eq(ChallengeJoin::getUserId, userId));
        if (exist != null) {
            // DB报名存在就算成功，Redis写失败不影响
            ensureRedisProgress(challengeId, userId, exist.getProgressValue());
            return R.ok(true);
        }

        ChallengeJoin cj = new ChallengeJoin();
        cj.setChallengeId(challengeId);
        cj.setUserId(userId);
        cj.setProgressValue(0);
        cj.setFinished(0);
        cj.setCreateTime(LocalDateTime.now());
        joinService.save(cj);

        // 报名即上榜：score=0
        ensureRedisProgress(challengeId, userId, 0);

        return R.ok(true);
    }

    /**
     * 我的挑战（我报名过的）
     */
    @GetMapping("/my")
    public R<List<ChallengeJoin>> my(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        List<ChallengeJoin> list = joinService.list(new LambdaQueryWrapper<ChallengeJoin>()
                .eq(ChallengeJoin::getUserId, userId)
                .orderByDesc(ChallengeJoin::getCreateTime));

        return R.ok(list);
    }

    /**
     * 更新我的进度（同步更新Redis进度榜）
     */
    @PostMapping("/progress/set")
    public R<Boolean> setProgress(@RequestBody ProgressSetDTO dto, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");
        if (dto == null || dto.getChallengeId() == null) throw new BizException("challengeId不能为空");
        if (dto.getProgressValue() == null || dto.getProgressValue() < 0) throw new BizException("progressValue不合法");

        ChallengeJoin join = joinService.getOne(new LambdaQueryWrapper<ChallengeJoin>()
                .eq(ChallengeJoin::getChallengeId, dto.getChallengeId())
                .eq(ChallengeJoin::getUserId, userId));
        if (join == null) throw new BizException("你还没报名该挑战");
        if (join.getFinished() != null && join.getFinished() == 1) throw new BizException("挑战已完成，不能再改进度");

        join.setProgressValue(dto.getProgressValue());
        joinService.updateById(join);

        // 写入Redis进度榜（score=progressValue）
        ensureRedisProgress(dto.getChallengeId(), userId, dto.getProgressValue());

        return R.ok(true);
    }

    /**
     * 完成挑战（达标后调用，发积分 + 写完成榜 + 触发勋章）
     */
    @PostMapping("/finish/{challengeId}")
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> finish(@PathVariable Long challengeId, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        Challenge ch = challengeService.getById(challengeId);
        if (ch == null || ch.getStatus() == 0) throw new BizException("挑战不存在或已下架");

        ChallengeJoin join = joinService.getOne(new LambdaQueryWrapper<ChallengeJoin>()
                .eq(ChallengeJoin::getChallengeId, challengeId)
                .eq(ChallengeJoin::getUserId, userId));
        if (join == null) throw new BizException("你还没报名该挑战");

        if (join.getFinished() != null && join.getFinished() == 1) {
            // 已完成：确保完成榜在Redis
            ensureRedisFinish(challengeId, userId, join.getFinishTime());
            return R.ok(true);
        }

        int progress = join.getProgressValue() == null ? 0 : join.getProgressValue();
        int target = ch.getTargetValue() == null ? 0 : ch.getTargetValue();
        if (progress < target) throw new BizException("进度未达标，无法完成挑战");

        // 标记完成
        join.setFinished(1);
        join.setFinishTime(LocalDateTime.now());
        joinService.updateById(join);

        // 完成榜写入：score=完成时间戳（越早越靠前）
        ensureRedisFinish(challengeId, userId, join.getFinishTime());

        // 发放积分（幂等：同一挑战只发一次）
        PointRecord exist = pointRecordService.getOne(new LambdaQueryWrapper<PointRecord>()
                .eq(PointRecord::getUserId, userId)
                .eq(PointRecord::getType, "CHALLENGE_FINISH")
                .eq(PointRecord::getBizId, challengeId));

        if (exist == null) {
            int reward = ch.getRewardPoints() == null ? 50 : ch.getRewardPoints();

            PointRecord pr = new PointRecord();
            pr.setUserId(userId);
            pr.setPoints(reward);
            pr.setType("CHALLENGE_FINISH");
            pr.setBizId(challengeId);
            pr.setRemark("完成挑战：" + ch.getTitle());
            pr.setCreateTime(LocalDateTime.now());
            pointRecordService.save(pr);

            // 累加积分榜（Redis）
            pointsLeaderboardService.incrPoints(userId, reward);

            // 触发成就：挑战完成 + 积分达标（总分从Redis读，读不到就用 reward 做降级）
            achievementService.onChallengeFinished(userId);
            achievementService.onPointsChanged(userId, readTotalPointsFromRedisOrFallback(userId, reward));
        }

        return R.ok(true);
    }

    // =========================
    // 管理端接口（管理员）
    // =========================

    /**
     * 管理端：创建挑战
     */
    @PostMapping("/admin/create")
    public R<Long> adminCreate(@RequestBody Challenge c, HttpServletRequest request) {
        requireAdmin(request);
        validateChallenge(c);

        Challenge ch = new Challenge();
        ch.setTitle(c.getTitle());
        ch.setDescription(c.getDescription());
        ch.setType(c.getType());
        ch.setTargetValue(c.getTargetValue());
        ch.setStartDate(c.getStartDate());
        ch.setEndDate(c.getEndDate());
        ch.setStatus(c.getStatus() == null ? 1 : c.getStatus());
        ch.setRewardPoints(c.getRewardPoints() == null ? 50 : c.getRewardPoints());
        ch.setCreateTime(LocalDateTime.now());
        ch.setUpdateTime(LocalDateTime.now());

        challengeService.save(ch);
        return R.ok(ch.getId());
    }

    /**
     * 管理端：更新挑战
     */
    @PostMapping("/admin/update")
    public R<Boolean> adminUpdate(@RequestBody Challenge c, HttpServletRequest request) {
        requireAdmin(request);
        if (c == null || c.getId() == null) throw new BizException("id不能为空");
        validateChallenge(c);

        Challenge ch = challengeService.getById(c.getId());
        if (ch == null) throw new BizException("挑战不存在");

        ch.setTitle(c.getTitle());
        ch.setDescription(c.getDescription());
        ch.setType(c.getType());
        ch.setTargetValue(c.getTargetValue());
        ch.setStartDate(c.getStartDate());
        ch.setEndDate(c.getEndDate());
        if (c.getStatus() != null) ch.setStatus(c.getStatus());
        if (c.getRewardPoints() != null) ch.setRewardPoints(c.getRewardPoints());
        ch.setUpdateTime(LocalDateTime.now());

        challengeService.updateById(ch);
        return R.ok(true);
    }

    /**
     * 管理端：上下架
     */
    @PostMapping("/admin/status")
    public R<Boolean> adminStatus(@RequestParam Long challengeId,
                                  @RequestParam Integer status,
                                  HttpServletRequest request) {
        requireAdmin(request);

        Challenge ch = challengeService.getById(challengeId);
        if (ch == null) throw new BizException("挑战不存在");

        ch.setStatus(status);
        ch.setUpdateTime(LocalDateTime.now());
        challengeService.updateById(ch);

        return R.ok(true);
    }

    /**
     * 管理端：分页（可看全部）
     */
    @GetMapping("/admin/page")
    public R<Page<Challenge>> adminPage(@RequestParam(defaultValue = "1") long pageNum,
                                        @RequestParam(defaultValue = "10") long pageSize,
                                        @RequestParam(required = false) Integer status,
                                        @RequestParam(required = false) String type,
                                        HttpServletRequest request) {
        requireAdmin(request);

        Page<Challenge> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Challenge> qw = new LambdaQueryWrapper<>();
        if (status != null) qw.eq(Challenge::getStatus, status);
        if (StringUtils.hasText(type)) qw.eq(Challenge::getType, type);
        qw.orderByDesc(Challenge::getCreateTime);

        return R.ok(challengeService.page(page, qw));
    }

    // =========================
    // Redis写入（失败不影响业务）
    // =========================

    private void ensureRedisProgress(Long challengeId, Long userId, Integer progressValue) {
        try {
            String key = "project03:challenge:" + challengeId + ":progress";
            int v = progressValue == null ? 0 : progressValue;
            // ZADD member=userId score=progressValue
            stringRedisTemplate.opsForZSet().add(key, String.valueOf(userId), v);
        } catch (Exception e) {
            log.warn("写入Redis进度榜失败（不影响业务），challengeId={}, userId={}, err={}",
                    challengeId, userId, e.getMessage());
        }
    }

    private void ensureRedisFinish(Long challengeId, Long userId, LocalDateTime finishTime) {
        try {
            if (finishTime == null) return;
            String key = "project03:challenge:" + challengeId + ":finish";
            long ts = finishTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            // ZADD member=userId score=finishTimestamp（越早越靠前）
            stringRedisTemplate.opsForZSet().add(key, String.valueOf(userId), ts);
        } catch (Exception e) {
            log.warn("写入Redis完成榜失败（不影响业务），challengeId={}, userId={}, err={}",
                    challengeId, userId, e.getMessage());
        }
    }

    /**
     * 从积分总榜Redis取总分；失败则用 fallback（演示兜底）
     */
    private int readTotalPointsFromRedisOrFallback(Long userId, int fallback) {
        try {
            Double score = stringRedisTemplate.opsForZSet().score("project03:points:total", String.valueOf(userId));
            if (score == null) return fallback;
            return score.intValue();
        } catch (Exception e) {
            return fallback;
        }
    }

    // =========================
    // DTO/VO/校验
    // =========================

    private void validateChallenge(Challenge c) {
        if (c == null) throw new BizException("参数不能为空");
        if (!StringUtils.hasText(c.getTitle())) throw new BizException("title不能为空");
        if (!StringUtils.hasText(c.getDescription())) throw new BizException("description不能为空");
        if (!StringUtils.hasText(c.getType())) throw new BizException("type不能为空");
        if (c.getTargetValue() == null || c.getTargetValue() <= 0) throw new BizException("targetValue不合法");
        if (c.getStartDate() == null || c.getEndDate() == null) throw new BizException("startDate/endDate不能为空");
        if (c.getEndDate().isBefore(c.getStartDate())) throw new BizException("endDate不能早于startDate");
    }

    /**
     * 进度设置DTO
     */
    public static class ProgressSetDTO {
        private Long challengeId;
        private Integer progressValue;

        public Long getChallengeId() { return challengeId; }
        public void setChallengeId(Long challengeId) { this.challengeId = challengeId; }

        public Integer getProgressValue() { return progressValue; }
        public void setProgressValue(Integer progressValue) { this.progressValue = progressValue; }
    }

    /**
     * 详情VO
     */
    public static class ChallengeDetailVO {
        private Challenge challenge;
        private boolean joined;
        private int myProgress;
        private boolean finished;

        public Challenge getChallenge() { return challenge; }
        public void setChallenge(Challenge challenge) { this.challenge = challenge; }

        public boolean isJoined() { return joined; }
        public void setJoined(boolean joined) { this.joined = joined; }

        public int getMyProgress() { return myProgress; }
        public void setMyProgress(int myProgress) { this.myProgress = myProgress; }

        public boolean isFinished() { return finished; }
        public void setFinished(boolean finished) { this.finished = finished; }
    }
}