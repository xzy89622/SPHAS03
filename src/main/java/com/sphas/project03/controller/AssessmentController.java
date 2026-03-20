package com.sphas.project03.controller;

import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.AssessmentEvaluateDTO;
import com.sphas.project03.entity.BmiStandard;
import com.sphas.project03.entity.HealthRecord;
import com.sphas.project03.service.BmiStandardService;
import com.sphas.project03.service.HealthRecordService;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 体质评估：结合最新健康记录 + BMI 标准 + 题库得分，输出评估结论
 */
@RestController
@RequestMapping("/api/assessment")
public class AssessmentController extends BaseController {

    private final HealthRecordService healthRecordService;
    private final BmiStandardService bmiStandardService;

    public AssessmentController(HealthRecordService healthRecordService,
                                BmiStandardService bmiStandardService) {
        this.healthRecordService = healthRecordService;
        this.bmiStandardService = bmiStandardService;
    }

    @PostMapping("/evaluate")
    public R<Map<String, Object>> evaluate(@RequestBody AssessmentEvaluateDTO dto,
                                           HttpServletRequest request) {

        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        if (dto == null || CollectionUtils.isEmpty(dto.getScores())) {
            throw new BizException("scores不能为空");
        }

        // 1) 取最新健康记录
        List<HealthRecord> latestList = healthRecordService.listLatest(userId, 1);
        HealthRecord latest = latestList.isEmpty() ? null : latestList.get(0);

        if (latest == null || latest.getHeightCm() == null || latest.getWeightKg() == null) {
            throw new BizException("请先在健康记录页录入身高体重");
        }

        // 2) 根据最新健康记录实时计算 BMI
        BigDecimal bmi = calcBmi(latest.getHeightCm(), latest.getWeightKg());

        // 3) 根据 bmi_standard 找区间：左闭右开 [min, max)
        BmiStandard standard = bmiStandardService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BmiStandard>()
                        .eq(BmiStandard::getStatus, 1)
                        .le(BmiStandard::getMinValue, bmi)
                        .gt(BmiStandard::getMaxValue, bmi)
                        .orderByAsc(BmiStandard::getMinValue)
                        .last("limit 1")
        );

        // 兜底：如果因为历史数据配置问题没匹配到，按常见 BMI 规则回退
        String bmiLevel;
        String bmiAdvice;
        if (standard != null) {
            bmiLevel = standard.getLevel();
            bmiAdvice = standard.getAdvice();
        } else {
            double bmiVal = bmi.doubleValue();
            if (bmiVal < 18.5) {
                bmiLevel = "偏瘦";
                bmiAdvice = "建议适当增加优质蛋白和主食摄入，保持规律作息，逐步提升体重到正常范围";
            } else if (bmiVal < 24.0) {
                bmiLevel = "正常";
                bmiAdvice = "请继续保持均衡饮食和规律运动";
            } else if (bmiVal < 28.0) {
                bmiLevel = "超重";
                bmiAdvice = "建议减少高热量摄入，增加有氧运动，并持续监测体重变化";
            } else {
                bmiLevel = "肥胖";
                bmiAdvice = "建议制定减脂计划，控制总热量摄入，并结合有氧与力量训练";
            }
        }

        // 4) 分数与综合评价
        Map<String, Integer> scores = dto.getScores();
        int totalScore = scores.values().stream().mapToInt(Integer::intValue).sum();

        String overall;
        if (totalScore >= 8) overall = "优秀";
        else if (totalScore >= 4) overall = "一般";
        else overall = "需改善";

        Map<String, Object> res = new HashMap<>();
        res.put("bmi", bmi);
        res.put("heightCm", latest.getHeightCm());
        res.put("weightKg", latest.getWeightKg());
        res.put("recordDate", latest.getRecordDate());

        res.put("bmiLevel", bmiLevel);
        res.put("bmiAdvice", bmiAdvice);

        res.put("scores", scores);
        res.put("totalScore", totalScore);
        res.put("overall", overall);

        res.put("sportTip", tipByScore(scores.get("SPORT"), "运动"));
        res.put("dietTip", tipByScore(scores.get("DIET"), "饮食"));
        res.put("sleepTip", tipByScore(scores.get("SLEEP"), "睡眠"));
        res.put("stressTip", tipByScore(scores.get("STRESS"), "压力"));

        return R.ok(res);
    }

    private BigDecimal calcBmi(Double heightCm, Double weightKg) {
        BigDecimal h = BigDecimal.valueOf(heightCm)
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(weightKg)
                .divide(h.multiply(h), 2, RoundingMode.HALF_UP);
    }

    private String tipByScore(Integer s, String name) {
        if (s == null) return name + "：未评估";
        if (s >= 3) return name + "：良好";
        if (s >= 1) return name + "：一般";
        return name + "：需要加强";
    }
}