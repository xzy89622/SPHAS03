package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.entity.BmiStandard;
import com.sphas.project03.entity.DietPlan;
import com.sphas.project03.entity.HealthRecord;
import com.sphas.project03.entity.SportPlan;
import com.sphas.project03.entity.UserRecommendation;
import com.sphas.project03.mapper.UserRecommendationMapper;
import com.sphas.project03.service.BmiStandardService;
import com.sphas.project03.service.DietPlanService;
import com.sphas.project03.service.HealthRecordService;
import com.sphas.project03.service.RecommendService;
import com.sphas.project03.service.SportPlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 推荐服务实现（规则引擎版）
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    private static final Logger log = LoggerFactory.getLogger(RecommendServiceImpl.class);

    private final HealthRecordService healthRecordService;
    private final BmiStandardService bmiStandardService;
    private final DietPlanService dietPlanService;
    private final SportPlanService sportPlanService;
    private final UserRecommendationMapper userRecommendationMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RecommendServiceImpl(HealthRecordService healthRecordService,
                                BmiStandardService bmiStandardService,
                                DietPlanService dietPlanService,
                                SportPlanService sportPlanService,
                                UserRecommendationMapper userRecommendationMapper) {
        this.healthRecordService = healthRecordService;
        this.bmiStandardService = bmiStandardService;
        this.dietPlanService = dietPlanService;
        this.sportPlanService = sportPlanService;
        this.userRecommendationMapper = userRecommendationMapper;
    }

    @Override
    public Map<String, Object> recommendToday(Long userId, Map<String, Integer> scores) {
        Map<String, Integer> safeScores = normalizeScores(scores);

        // 先看今天有没有已经生成过的推荐，有就直接复用
        UserRecommendation todayRec = findTodayRecommendation(userId);
        if (todayRec != null) {
            return buildResultFromRecord(todayRec, safeScores);
        }

        // 1) 从最新健康记录取身高体重
        List<HealthRecord> latestList = healthRecordService.listLatest(userId, 1);
        HealthRecord latest = latestList.isEmpty() ? null : latestList.get(0);

        if (latest == null || latest.getHeightCm() == null || latest.getWeightKg() == null) {
            throw new BizException("请先在健康记录页录入身高体重，系统才能推荐");
        }

        BigDecimal bmi = calcBmi(latest.getHeightCm(), latest.getWeightKg());

        // 2) BMI 等级：左闭右开 [min, max)
        BmiStandard standard = bmiStandardService.getOne(
                new LambdaQueryWrapper<BmiStandard>()
                        .eq(BmiStandard::getStatus, 1)
                        .le(BmiStandard::getMinValue, bmi)
                        .gt(BmiStandard::getMaxValue, bmi)
                        .orderByAsc(BmiStandard::getMinValue)
                        .last("limit 1")
        );

        String bmiLevel;
        if (standard != null) {
            bmiLevel = standard.getLevel();
        } else {
            double bmiVal = bmi.doubleValue();
            if (bmiVal < 18.5) {
                bmiLevel = "偏瘦";
            } else if (bmiVal < 24.0) {
                bmiLevel = "正常";
            } else if (bmiVal < 28.0) {
                bmiLevel = "超重";
            } else {
                bmiLevel = "肥胖";
            }
        }

        // 3) 根据 SPORT 得分决定运动强度
        int sportScore = resolveSportScore(safeScores);

        String intensity;
        if (sportScore >= 3) {
            intensity = "MID";
        } else if (sportScore >= 1) {
            intensity = "LOW";
        } else {
            intensity = "LOW";
        }

        // 4) 匹配方案
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

        if (diet == null) {
            diet = dietPlanService.getOne(
                    new LambdaQueryWrapper<DietPlan>()
                            .eq(DietPlan::getStatus, 1)
                            .orderByDesc(DietPlan::getId)
                            .last("limit 1")
            );
        }
        if (sport == null) {
            sport = sportPlanService.getOne(
                    new LambdaQueryWrapper<SportPlan>()
                            .eq(SportPlan::getStatus, 1)
                            .orderByDesc(SportPlan::getId)
                            .last("limit 1")
            );
        }

        String reason = "基于你最新健康记录计算的BMI(" + bmi + "，" + bmiLevel + ")与运动得分(" + sportScore + ")生成今日方案";

        UserRecommendation rec = null;
        try {
            rec = new UserRecommendation();
            rec.setUserId(userId);
            rec.setBmi(bmi);
            rec.setBmiLevel(bmiLevel);
            rec.setScoresJson(objectMapper.writeValueAsString(safeScores));
            rec.setDietPlanId(diet == null ? null : diet.getId());
            rec.setSportPlanId(sport == null ? null : sport.getId());
            rec.setReason(reason);
            rec.setCreateTime(LocalDateTime.now());
            userRecommendationMapper.insert(rec);
        } catch (Exception e) {
            log.error("用户 {} 每日推荐方案落库失败", userId, e);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("bmi", bmi);
        res.put("bmiLevel", bmiLevel);
        res.put("diet", diet);
        res.put("sport", sport);
        res.put("reason", reason);
        res.put("scores", safeScores);
        res.put("heightCm", latest.getHeightCm());
        res.put("weightKg", latest.getWeightKg());
        res.put("recordDate", latest.getRecordDate());
        if (rec != null) {
            res.put("recommendationId", rec.getId());
            res.put("recommendationCreateTime", rec.getCreateTime());
        }
        return res;
    }

    /**
     * 取今天最后一条推荐
     */
    public UserRecommendation findTodayRecommendation(Long userId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        return userRecommendationMapper.selectOne(
                new LambdaQueryWrapper<UserRecommendation>()
                        .eq(UserRecommendation::getUserId, userId)
                        .ge(UserRecommendation::getCreateTime, start)
                        .lt(UserRecommendation::getCreateTime, end)
                        .orderByDesc(UserRecommendation::getId)
                        .last("limit 1")
        );
    }

    private Map<String, Object> buildResultFromRecord(UserRecommendation rec, Map<String, Integer> fallbackScores) {
        DietPlan diet = rec.getDietPlanId() == null ? null : dietPlanService.getById(rec.getDietPlanId());
        SportPlan sport = rec.getSportPlanId() == null ? null : sportPlanService.getById(rec.getSportPlanId());

        Map<String, Integer> recordScores = parseScores(rec.getScoresJson());
        if (recordScores.isEmpty()) {
            recordScores = fallbackScores;
        }

        // 这里还是顺手把最新健康记录带回去，前端展示会更稳
        List<HealthRecord> latestList = healthRecordService.listLatest(rec.getUserId(), 1);
        HealthRecord latest = latestList.isEmpty() ? null : latestList.get(0);

        Map<String, Object> res = new HashMap<>();
        res.put("bmi", rec.getBmi());
        res.put("bmiLevel", rec.getBmiLevel());
        res.put("diet", diet);
        res.put("sport", sport);
        res.put("reason", rec.getReason());
        res.put("scores", recordScores);
        res.put("recommendationId", rec.getId());
        res.put("recommendationCreateTime", rec.getCreateTime());

        if (latest != null) {
            res.put("heightCm", latest.getHeightCm());
            res.put("weightKg", latest.getWeightKg());
            res.put("recordDate", latest.getRecordDate());
        }

        return res;
    }

    private Map<String, Integer> normalizeScores(Map<String, Integer> scores) {
        if (scores == null) {
            return new HashMap<>();
        }
        return new HashMap<>(scores);
    }

    private Map<String, Integer> parseScores(String scoresJson) {
        if (scoresJson == null || scoresJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(scoresJson, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            log.warn("解析推荐分数字段失败: {}", scoresJson, e);
            return new HashMap<>();
        }
    }

    private int resolveSportScore(Map<String, Integer> scores) {
        if (scores.containsKey("SPORT")) {
            return scores.getOrDefault("SPORT", 0);
        }
        if (scores.containsKey("default")) {
            return scores.getOrDefault("default", 0);
        }
        if (scores.containsKey("DEFAULT")) {
            return scores.getOrDefault("DEFAULT", 0);
        }
        return 0;
    }

    private BigDecimal calcBmi(Double heightCm, Double weightKg) {
        BigDecimal h = BigDecimal.valueOf(heightCm)
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(weightKg)
                .divide(h.multiply(h), 2, RoundingMode.HALF_UP);
    }
}