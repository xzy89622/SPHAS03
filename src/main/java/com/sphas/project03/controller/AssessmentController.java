package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.AssessmentEvaluateDTO;
import com.sphas.project03.entity.BmiStandard;
import com.sphas.project03.entity.HealthMetricRecord;
import com.sphas.project03.service.BmiStandardService;
import com.sphas.project03.service.HealthMetricRecordService;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 体质评估：结合 BMI 标准 + 题库得分，输出评估结论
 */
@RestController
@RequestMapping("/api/assessment")
public class AssessmentController extends BaseController {

    private final HealthMetricRecordService metricService;
    private final BmiStandardService bmiStandardService;

    public AssessmentController(HealthMetricRecordService metricService,
                                BmiStandardService bmiStandardService) {
        this.metricService = metricService;
        this.bmiStandardService = bmiStandardService;
    }

    /**
     * 用户提交评估：scores（维度得分）+ 系统取最新BMI → 输出结论
     */
    @PostMapping("/evaluate")
    public R<Map<String, Object>> evaluate(@RequestBody AssessmentEvaluateDTO dto,
                                           HttpServletRequest request) {

        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        if (dto == null || CollectionUtils.isEmpty(dto.getScores())) {
            throw new BizException("scores不能为空");
        }

        // 1) 取最新体质记录（拿 bmi）
        HealthMetricRecord latest = metricService.getOne(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .orderByDesc(HealthMetricRecord::getRecordTime)
                        .last("limit 1")
        );
        if (latest == null || latest.getBmi() == null) {
            throw new BizException("请先录入身高体重，系统才能评估BMI");
        }

        BigDecimal bmi = latest.getBmi();

        // 2) 根据 bmi_standard 找到对应区间
        BmiStandard standard = bmiStandardService.getOne(
                new LambdaQueryWrapper<BmiStandard>()
                        .eq(BmiStandard::getStatus, 1)
                        .le(BmiStandard::getMinValue, bmi)
                        .gt(BmiStandard::getMaxValue, bmi)
                        .last("limit 1")
        );

        // 如果你希望 maxValue 也包含，就把 gt 改成 ge

        // 3) 计算总分/给维度建议（先规则版，后面可升级）
        Map<String, Integer> scores = dto.getScores();
        int totalScore = scores.values().stream().mapToInt(Integer::intValue).sum();

        String overall;
        if (totalScore >= 8) overall = "优秀";
        else if (totalScore >= 4) overall = "一般";
        else overall = "需改善";

        Map<String, Object> res = new HashMap<>();
        res.put("bmi", bmi);
        if (standard != null) {
            res.put("bmiLevel", standard.getLevel());
            res.put("bmiAdvice", standard.getAdvice());
        } else {
            res.put("bmiLevel", "未知");
            res.put("bmiAdvice", "暂无匹配的BMI标准，请联系管理员维护区间");
        }

        res.put("scores", scores);
        res.put("totalScore", totalScore);
        res.put("overall", overall);

        // 维度提示（简单版）
        res.put("sportTip", tipByScore(scores.get("SPORT"), "运动"));
        res.put("dietTip", tipByScore(scores.get("DIET"), "饮食"));
        res.put("sleepTip", tipByScore(scores.get("SLEEP"), "睡眠"));
        res.put("stressTip", tipByScore(scores.get("STRESS"), "压力"));

        return R.ok(res);
    }

    private String tipByScore(Integer s, String name) {
        if (s == null) return name + "：未评估";
        if (s >= 3) return name + "：良好";
        if (s >= 1) return name + "：一般";
        return name + "：需要加强";
    }
}
