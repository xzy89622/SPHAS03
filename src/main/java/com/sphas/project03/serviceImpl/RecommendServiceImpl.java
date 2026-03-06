package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.entity.*;
import com.sphas.project03.mapper.UserRecommendationMapper;
import com.sphas.project03.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 推荐服务实现（规则引擎版）
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    private static final Logger log = LoggerFactory.getLogger(RecommendServiceImpl.class);

    private final HealthMetricRecordService metricService;
    private final BmiStandardService bmiStandardService;
    private final DietPlanService dietPlanService;
    private final SportPlanService sportPlanService;
    private final UserRecommendationMapper userRecommendationMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RecommendServiceImpl(HealthMetricRecordService metricService,
                                BmiStandardService bmiStandardService,
                                DietPlanService dietPlanService,
                                SportPlanService sportPlanService,
                                UserRecommendationMapper userRecommendationMapper) {
        this.metricService = metricService;
        this.bmiStandardService = bmiStandardService;
        this.dietPlanService = dietPlanService;
        this.sportPlanService = sportPlanService;
        this.userRecommendationMapper = userRecommendationMapper;
    }

    @Override
    public Map<String, Object> recommendToday(Long userId, Map<String, Integer> scores) {

        // ✅ 兜底：scores 允许为空，避免 NPE
        if (scores == null) scores = new HashMap<>();

        // 1) 最新体质记录（拿BMI）
        HealthMetricRecord latest = metricService.getOne(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .orderByDesc(HealthMetricRecord::getRecordTime)
                        .last("limit 1")
        );
        if (latest == null || latest.getBmi() == null) {
            throw new BizException("请先录入身高体重，系统才能推荐");
        }

        // 2) BMI 等级（复用 bmi_standard）
        BmiStandard standard = bmiStandardService.getOne(
                new LambdaQueryWrapper<BmiStandard>()
                        .eq(BmiStandard::getStatus, 1)
                        .le(BmiStandard::getMinValue, latest.getBmi())
                        .gt(BmiStandard::getMaxValue, latest.getBmi())
                        .last("limit 1")
        );
        String bmiLevel = standard == null ? "未知" : standard.getLevel();

        // 3) 规则：根据 SPORT 得分决定运动强度
        // ✅ 兼容：前端可能只传 default（删题后更容易出现）
        int sportScore = 0;
        if (scores.containsKey("SPORT")) {
            sportScore = scores.getOrDefault("SPORT", 0);
        } else if (scores.containsKey("default")) {
            sportScore = scores.getOrDefault("default", 0);
        }

        String intensity;
        if (sportScore >= 3) intensity = "MID";
        else if (sportScore >= 1) intensity = "LOW";
        else intensity = "LOW";

        // 4) 从库里挑方案（优先匹配 bmiLevel）
        DietPlan diet = dietPlanService.getOne(
                new LambdaQueryWrapper<DietPlan>()
                        .eq(DietPlan::getStatus, 1)
                        .eq(DietPlan::getBmiLevel, bmiLevel)
                        .orderByDesc(DietPlan::getId)
                        .last("limit 1")
        );
        SportPlan sport = sportPlanService.getOne(
                new LambdaQueryWrapper<SportPlan>()
                        .eq(SportPlan::getStatus, 1)
                        .eq(SportPlan::getBmiLevel, bmiLevel)
                        .eq(SportPlan::getIntensity, intensity)
                        .orderByDesc(SportPlan::getId)
                        .last("limit 1")
        );

        // 兜底：如果没有对应方案，就随便取一条启用的
        if (diet == null) {
            diet = dietPlanService.getOne(new LambdaQueryWrapper<DietPlan>()
                    .eq(DietPlan::getStatus, 1).orderByDesc(DietPlan::getId).last("limit 1"));
        }
        if (sport == null) {
            sport = sportPlanService.getOne(new LambdaQueryWrapper<SportPlan>()
                    .eq(SportPlan::getStatus, 1).orderByDesc(SportPlan::getId).last("limit 1"));
        }

        // 5) 推荐理由
        String reason = "基于你当前BMI(" + latest.getBmi() + "，" + bmiLevel + ")与运动得分(" + sportScore + ")生成今日方案";

        // 6) 落库（推荐展示/历史查看）
        try {
            UserRecommendation rec = new UserRecommendation();
            rec.setUserId(userId);
            rec.setBmi(latest.getBmi());
            rec.setBmiLevel(bmiLevel);
            rec.setScoresJson(objectMapper.writeValueAsString(scores));
            rec.setDietPlanId(diet == null ? null : diet.getId());
            rec.setSportPlanId(sport == null ? null : sport.getId());
            rec.setReason(reason);
            rec.setCreateTime(LocalDateTime.now());
            userRecommendationMapper.insert(rec);
        } catch (Exception e) {
            log.error("用户 {} 每日推荐方案落库失败", userId, e);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("bmi", latest.getBmi());
        res.put("bmiLevel", bmiLevel);
        res.put("diet", diet);
        res.put("sport", sport);
        res.put("reason", reason);
        res.put("scores", scores);
        return res;
    }
}