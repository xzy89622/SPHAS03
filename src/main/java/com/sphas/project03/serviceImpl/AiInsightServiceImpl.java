package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.controller.dto.AiPredictionDTO;
import com.sphas.project03.entity.HealthMetricRecord;
import com.sphas.project03.service.AiInsightService;
import com.sphas.project03.service.AiPythonClient;
import com.sphas.project03.service.HealthMetricRecordService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI 解读 + 预测
 */
@Service
public class AiInsightServiceImpl implements AiInsightService {

    private final AiPythonClient aiPythonClient;
    private final HealthMetricRecordService metricRecordService;

    public AiInsightServiceImpl(AiPythonClient aiPythonClient,
                                HealthMetricRecordService metricRecordService) {
        this.aiPythonClient = aiPythonClient;
        this.metricRecordService = metricRecordService;
    }

    @Override
    public String buildSummary(String level, int score, List<String> reasons, String advice) {
        return "风险等级：" + level + "，风险分：" + score + "。主要原因：" + String.join("、", reasons) + "。建议：" + advice;
    }

    @Override
    public AiPredictionDTO predict7Days(Long userId, int riskScore) {

        // ✅ 1) 拉取历史体质数据（用于预测体重/趋势）
        // 取最近60天，数据太少Python会降级
        LocalDateTime from = LocalDateTime.now().minusDays(60);
        List<HealthMetricRecord> history = metricRecordService.list(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .ge(HealthMetricRecord::getRecordTime, from)
                        .orderByAsc(HealthMetricRecord::getRecordTime)
        );

        // ✅ 2) 组装给Python的payload（包含 history）
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("horizonDays", 7);
        payload.put("riskScore", riskScore);

        // history: list<map>，只放常用字段（Python端好解析）
        List<Map<String, Object>> h = new ArrayList<>();
        if (history != null) {
            for (HealthMetricRecord r : history) {
                Map<String, Object> m = new HashMap<>();
                m.put("t", r.getRecordTime() == null ? null : r.getRecordTime().toString());
                m.put("weightKg", r.getWeightKg());
                m.put("bmi", r.getBmi());
                m.put("steps", r.getSteps());
                m.put("sleepHours", r.getSleepHours());
                m.put("systolic", r.getSystolic());
                m.put("diastolic", r.getDiastolic());
                m.put("bloodSugar", r.getBloodSugar());
                h.add(m);
            }
        }
        payload.put("history", h);

        // ✅ 3) 优先调Python模型
        Map<String, Object> py = aiPythonClient.predict(payload);
        if (py != null) {
            AiPredictionDTO dto = new AiPredictionDTO();
            dto.setModel("PYTHON");
            dto.setHorizonDays(7);

            // predictedRiskScore
            Object prs = py.get("predictedRiskScore");
            dto.setPredictedRiskScore(prs == null ? riskScore : Integer.parseInt(String.valueOf(prs)));

            // predictedWeightKg
            Object pw = py.get("predictedWeightKg");
            if (pw != null) {
                try {
                    dto.setPredictedWeightKg(new BigDecimal(String.valueOf(pw)));
                } catch (Exception ignore) {
                }
            }

            // trend
            dto.setTrend(String.valueOf(py.getOrDefault("trend", "STABLE")));

            // suggestion
            dto.setSuggestion(String.valueOf(py.getOrDefault("suggestion", "请根据预测结果调整生活方式")));

            return dto;
        }

        // ✅ 4) 降级：规则预测（没Python服务也能演示）
        AiPredictionDTO dto = new AiPredictionDTO();
        dto.setModel("FALLBACK");
        dto.setHorizonDays(7);

        // 风险分简单+3（演示）
        dto.setPredictedRiskScore(Math.min(100, riskScore + 3));

        // 体重：取最新一条，没数据就null
        BigDecimal latestWeight = null;
        if (history != null && !history.isEmpty()) {
            HealthMetricRecord last = history.get(history.size() - 1);
            latestWeight = last.getWeightKg();
        }
        dto.setPredictedWeightKg(latestWeight);
        dto.setTrend("STABLE");
        dto.setSuggestion("（降级预测）建议保持规律运动与饮食，持续监测体重与风险变化");

        return dto;
    }
}