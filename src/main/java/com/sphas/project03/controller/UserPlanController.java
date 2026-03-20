package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.DietPlan;
import com.sphas.project03.entity.SportPlan;
import com.sphas.project03.entity.UserPlan;
import com.sphas.project03.entity.UserPlanCheckin;
import com.sphas.project03.entity.UserRecommendation;
import com.sphas.project03.service.DietPlanService;
import com.sphas.project03.service.RecommendService;
import com.sphas.project03.service.SportPlanService;
import com.sphas.project03.service.UserPlanCheckinService;
import com.sphas.project03.service.UserPlanService;
import com.sphas.project03.serviceImpl.RecommendServiceImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户计划与打卡
 */
@RestController
@RequestMapping("/api/user-plan")
public class UserPlanController extends BaseController {

    private final RecommendService recommendService;
    private final RecommendServiceImpl recommendServiceImpl;
    private final UserPlanService userPlanService;
    private final UserPlanCheckinService userPlanCheckinService;
    private final DietPlanService dietPlanService;
    private final SportPlanService sportPlanService;

    public UserPlanController(RecommendService recommendService,
                              RecommendServiceImpl recommendServiceImpl,
                              UserPlanService userPlanService,
                              UserPlanCheckinService userPlanCheckinService,
                              DietPlanService dietPlanService,
                              SportPlanService sportPlanService) {
        this.recommendService = recommendService;
        this.recommendServiceImpl = recommendServiceImpl;
        this.userPlanService = userPlanService;
        this.userPlanCheckinService = userPlanCheckinService;
        this.dietPlanService = dietPlanService;
        this.sportPlanService = sportPlanService;
    }

    /**
     * 采纳今日推荐，生成当前计划
     */
    @PostMapping("/adopt-today")
    public R<Map<String, Object>> adoptToday(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            throw new BizException("未登录");
        }

        // 先复用今天已经生成过的推荐，没有再补生成
        UserRecommendation todayRec = recommendServiceImpl.findTodayRecommendation(userId);
        Map<String, Object> recommend;
        if (todayRec != null) {
            recommend = buildRecommendFromRecord(todayRec);
        } else {
            recommend = recommendService.recommendToday(userId, new HashMap<>());
        }

        DietPlan diet = recommend.get("diet") instanceof DietPlan ? (DietPlan) recommend.get("diet") : null;
        SportPlan sport = recommend.get("sport") instanceof SportPlan ? (SportPlan) recommend.get("sport") : null;

        if (diet == null && sport == null) {
            throw new BizException("当前没有可采纳的推荐方案");
        }

        LocalDate today = LocalDate.now();

        UserPlan activePlan = userPlanService.getOne(
                new LambdaQueryWrapper<UserPlan>()
                        .eq(UserPlan::getUserId, userId)
                        .eq(UserPlan::getStatus, "ACTIVE")
                        .orderByDesc(UserPlan::getId)
                        .last("limit 1")
        );

        if (activePlan == null) {
            activePlan = new UserPlan();
            activePlan.setUserId(userId);
            activePlan.setCreateTime(LocalDateTime.now());
        }

        activePlan.setDietPlanId(diet == null ? null : diet.getId());
        activePlan.setSportPlanId(sport == null ? null : sport.getId());
        activePlan.setStartDate(today);
        activePlan.setEndDate(today.plusDays(6));
        activePlan.setStatus("ACTIVE");
        activePlan.setUpdateTime(LocalDateTime.now());

        if (activePlan.getId() == null) {
            userPlanService.save(activePlan);
        } else {
            userPlanService.updateById(activePlan);
        }

        Map<String, Object> res = buildPlanVO(activePlan);
        res.put("message", "已采纳今日方案");
        return R.ok(res);
    }

    /**
     * 获取我的当前执行计划
     */
    @GetMapping("/current")
    public R<Map<String, Object>> current(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            throw new BizException("未登录");
        }

        UserPlan activePlan = userPlanService.getOne(
                new LambdaQueryWrapper<UserPlan>()
                        .eq(UserPlan::getUserId, userId)
                        .eq(UserPlan::getStatus, "ACTIVE")
                        .orderByDesc(UserPlan::getId)
                        .last("limit 1")
        );

        if (activePlan == null) {
            return R.ok(new HashMap<>());
        }

        return R.ok(buildPlanVO(activePlan));
    }

    /**
     * 获取今天的打卡状态
     */
    @GetMapping("/today-checkin")
    public R<Map<String, Object>> todayCheckin(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            throw new BizException("未登录");
        }

        UserPlan activePlan = userPlanService.getOne(
                new LambdaQueryWrapper<UserPlan>()
                        .eq(UserPlan::getUserId, userId)
                        .eq(UserPlan::getStatus, "ACTIVE")
                        .orderByDesc(UserPlan::getId)
                        .last("limit 1")
        );

        if (activePlan == null) {
            return R.ok(new HashMap<>());
        }

        UserPlanCheckin checkin = userPlanCheckinService.getOne(
                new LambdaQueryWrapper<UserPlanCheckin>()
                        .eq(UserPlanCheckin::getUserPlanId, activePlan.getId())
                        .eq(UserPlanCheckin::getCheckinDate, LocalDate.now())
                        .last("limit 1")
        );

        Map<String, Object> res = new HashMap<>();
        res.put("userPlanId", activePlan.getId());
        res.put("checkinDate", LocalDate.now());
        res.put("dietDone", checkin == null ? 0 : checkin.getDietDone());
        res.put("sportDone", checkin == null ? 0 : checkin.getSportDone());
        res.put("remark", checkin == null ? "" : checkin.getRemark());
        return R.ok(res);
    }

    /**
     * 保存今天的打卡状态
     */
    @PostMapping("/today-checkin")
    public R<Map<String, Object>> saveTodayCheckin(@RequestBody CheckinDTO dto, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            throw new BizException("未登录");
        }

        UserPlan activePlan = userPlanService.getOne(
                new LambdaQueryWrapper<UserPlan>()
                        .eq(UserPlan::getUserId, userId)
                        .eq(UserPlan::getStatus, "ACTIVE")
                        .orderByDesc(UserPlan::getId)
                        .last("limit 1")
        );

        if (activePlan == null) {
            throw new BizException("你还没有采纳执行计划");
        }

        UserPlanCheckin checkin = userPlanCheckinService.getOne(
                new LambdaQueryWrapper<UserPlanCheckin>()
                        .eq(UserPlanCheckin::getUserPlanId, activePlan.getId())
                        .eq(UserPlanCheckin::getCheckinDate, LocalDate.now())
                        .last("limit 1")
        );

        if (checkin == null) {
            checkin = new UserPlanCheckin();
            checkin.setUserPlanId(activePlan.getId());
            checkin.setUserId(userId);
            checkin.setCheckinDate(LocalDate.now());
            checkin.setCreateTime(LocalDateTime.now());
        }

        checkin.setDietDone(dto.getDietDone() != null && dto.getDietDone() == 1 ? 1 : 0);
        checkin.setSportDone(dto.getSportDone() != null && dto.getSportDone() == 1 ? 1 : 0);
        checkin.setRemark(dto.getRemark() == null ? "" : dto.getRemark().trim());

        if (checkin.getId() == null) {
            userPlanCheckinService.save(checkin);
        } else {
            userPlanCheckinService.updateById(checkin);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("userPlanId", activePlan.getId());
        res.put("checkinDate", checkin.getCheckinDate());
        res.put("dietDone", checkin.getDietDone());
        res.put("sportDone", checkin.getSportDone());
        res.put("remark", checkin.getRemark());
        return R.ok(res);
    }

    private Map<String, Object> buildPlanVO(UserPlan plan) {
        Map<String, Object> res = new HashMap<>();
        res.put("id", plan.getId());
        res.put("userId", plan.getUserId());
        res.put("startDate", plan.getStartDate());
        res.put("endDate", plan.getEndDate());
        res.put("status", plan.getStatus());

        DietPlan diet = plan.getDietPlanId() == null ? null : dietPlanService.getById(plan.getDietPlanId());
        SportPlan sport = plan.getSportPlanId() == null ? null : sportPlanService.getById(plan.getSportPlanId());

        res.put("diet", diet);
        res.put("sport", sport);
        return res;
    }

    private Map<String, Object> buildRecommendFromRecord(UserRecommendation rec) {
        Map<String, Object> res = new HashMap<>();
        res.put("bmi", rec.getBmi());
        res.put("bmiLevel", rec.getBmiLevel());
        res.put("reason", rec.getReason());
        res.put("recommendationId", rec.getId());
        res.put("recommendationCreateTime", rec.getCreateTime());

        DietPlan diet = rec.getDietPlanId() == null ? null : dietPlanService.getById(rec.getDietPlanId());
        SportPlan sport = rec.getSportPlanId() == null ? null : sportPlanService.getById(rec.getSportPlanId());

        res.put("diet", diet);
        res.put("sport", sport);
        return res;
    }

    public static class CheckinDTO {
        private Integer dietDone;
        private Integer sportDone;
        private String remark;

        public Integer getDietDone() {
            return dietDone;
        }

        public void setDietDone(Integer dietDone) {
            this.dietDone = dietDone;
        }

        public Integer getSportDone() {
            return sportDone;
        }

        public void setSportDone(Integer sportDone) {
            this.sportDone = sportDone;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}