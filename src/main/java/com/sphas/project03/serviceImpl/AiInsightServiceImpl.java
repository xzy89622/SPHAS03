package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.controller.dto.AiPredictionDTO;
import com.sphas.project03.entity.HealthMetricRecord;
import com.sphas.project03.service.AiInsightService;
import com.sphas.project03.service.AiPythonClient;
import com.sphas.project03.service.HealthMetricRecordService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        String levelText = "LOW".equals(level) ? "低风险" : ("MID".equals(level) ? "中风险" : "高风险");
        String reasonText = (reasons == null || reasons.isEmpty()) ? "暂无明显异常" : String.join("、", reasons);

        return "风险等级：" + levelText
                + "，风险分：" + score
                + "。主要原因：" + reasonText
                + "。建议：" + advice;
    }

    @Override
    public AiPredictionDTO predict7Days(Long userId, int riskScore) {
        LocalDateTime from = LocalDateTime.now().minusDays(60);

        List<HealthMetricRecord> history = metricRecordService.list(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .ge(HealthMetricRecord::getRecordTime, from)
                        .orderByAsc(HealthMetricRecord::getRecordTime)
        );

        List<HealthMetricRecord> validHistory = filterValidHistory(history);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("horizonDays", 7);
        payload.put("riskScore", riskScore);
        payload.put("history", buildHistoryPayload(validHistory));

        Map<String, Object> py = aiPythonClient.predict(payload);
        if (py != null && !py.isEmpty()) {
            return buildPythonResult(py, validHistory, riskScore);
        }

        return buildFallbackResult(validHistory, riskScore);
    }

    /**
     * 过滤有效历史
     */
    private List<HealthMetricRecord> filterValidHistory(List<HealthMetricRecord> history) {
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }

        List<HealthMetricRecord> list = new ArrayList<>();
        for (HealthMetricRecord r : history) {
            if (r != null) {
                list.add(r);
            }
        }
        return list;
    }

    /**
     * 组装给 Python 的 history
     */
    private List<Map<String, Object>> buildHistoryPayload(List<HealthMetricRecord> history) {
        List<Map<String, Object>> list = new ArrayList<>();

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
            list.add(m);
        }

        return list;
    }

    /**
     * Python结果转DTO
     */
    private AiPredictionDTO buildPythonResult(Map<String, Object> py,
                                              List<HealthMetricRecord> history,
                                              int currentRiskScore) {
        AiPredictionDTO dto = new AiPredictionDTO();
        dto.setModel("PYTHON");
        dto.setHorizonDays(7);
        dto.setHistoryCount(history.size());

        Integer predictedRiskScore = parseInt(py.get("predictedRiskScore"), currentRiskScore);
        dto.setPredictedRiskScore(predictedRiskScore);
        dto.setPredictedLevel(toRiskLevel(predictedRiskScore));

        BigDecimal predictedWeight = parseDecimal(py.get("predictedWeightKg"));
        dto.setPredictedWeightKg(predictedWeight);

        String trend = normalizeTrend(String.valueOf(py.getOrDefault("trend", "STABLE")));
        dto.setTrend(trend);

        dto.setConfidence(calcConfidence(history.size(), true));

        List<String> basis = buildBasis(history);
        dto.setBasis(basis);

        String suggestion = readString(py.get("suggestion"), "建议继续记录健康数据，保持干预节奏");
        dto.setSuggestion(suggestion);

        String message = buildMessage(predictedRiskScore, trend, predictedWeight, history.size(), true);
        dto.setMessage(message);

        return dto;
    }

    /**
     * 降级预测
     */
    private AiPredictionDTO buildFallbackResult(List<HealthMetricRecord> history, int currentRiskScore) {
        AiPredictionDTO dto = new AiPredictionDTO();
        dto.setModel("FALLBACK");
        dto.setHorizonDays(7);
        dto.setHistoryCount(history.size());

        BigDecimal latestWeight = latestWeight(history);
        BigDecimal predictedWeight = predictWeightByRule(history, latestWeight);
        dto.setPredictedWeightKg(predictedWeight);

        String trend = predictTrendByRule(history, latestWeight, predictedWeight);
        dto.setTrend(trend);

        int predictedRiskScore = predictRiskScoreByRule(currentRiskScore, trend, history);
        dto.setPredictedRiskScore(predictedRiskScore);
        dto.setPredictedLevel(toRiskLevel(predictedRiskScore));

        dto.setConfidence(calcConfidence(history.size(), false));
        dto.setBasis(buildBasis(history));
        dto.setMessage(buildMessage(predictedRiskScore, trend, predictedWeight, history.size(), false));
        dto.setSuggestion(buildFallbackSuggestion(trend, predictedRiskScore, history));

        return dto;
    }

    /**
     * 规则预测体重
     */
    private BigDecimal predictWeightByRule(List<HealthMetricRecord> history, BigDecimal latestWeight) {
        if (latestWeight == null) {
            return null;
        }

        if (history.size() < 2) {
            return latestWeight;
        }

        BigDecimal avgDelta = avgWeightDelta(history);
        if (avgDelta == null) {
            return latestWeight;
        }

        BigDecimal predicted = latestWeight.add(avgDelta.multiply(new BigDecimal("2")));
        return scale2(predicted);
    }

    /**
     * 规则预测趋势
     */
    private String predictTrendByRule(List<HealthMetricRecord> history,
                                      BigDecimal latestWeight,
                                      BigDecimal predictedWeight) {
        if (latestWeight == null || predictedWeight == null) {
            return "STABLE";
        }

        BigDecimal diff = predictedWeight.subtract(latestWeight).abs();
        if (diff.compareTo(new BigDecimal("0.30")) < 0) {
            return "STABLE";
        }

        return predictedWeight.compareTo(latestWeight) > 0 ? "UP" : "DOWN";
    }

    /**
     * 规则预测风险分
     */
    private int predictRiskScoreByRule(int currentRiskScore,
                                       String trend,
                                       List<HealthMetricRecord> history) {
        int score = currentRiskScore;

        if ("UP".equals(trend)) {
            score += 4;
        } else if ("DOWN".equals(trend)) {
            score -= 2;
        } else {
            score += 1;
        }

        HealthMetricRecord latest = latestRecord(history);
        if (latest != null) {
            if (latest.getSleepHours() != null && latest.getSleepHours().compareTo(new BigDecimal("6")) < 0) {
                score += 2;
            }
            if (latest.getSteps() != null && latest.getSteps() < 5000) {
                score += 2;
            }
            if (latest.getSystolic() != null && latest.getSystolic() >= 140) {
                score += 4;
            }
            if (latest.getDiastolic() != null && latest.getDiastolic() >= 90) {
                score += 4;
            }
            if (latest.getBloodSugar() != null && latest.getBloodSugar().compareTo(new BigDecimal("7.0")) >= 0) {
                score += 4;
            }
        }

        if (history.size() < 5) {
            score += 1;
        }

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 生成预测依据
     */
    private List<String> buildBasis(List<HealthMetricRecord> history) {
        List<String> list = new ArrayList<>();

        list.add("最近60天体质记录数：" + history.size());

        BigDecimal latestWeight = latestWeight(history);
        if (latestWeight != null) {
            list.add("最新体重：" + latestWeight + "kg");
        }

        BigDecimal latestBmi = latestBmi(history);
        if (latestBmi != null) {
            list.add("最新BMI：" + latestBmi);
        }

        BigDecimal avgSleep = avgSleep(history);
        if (avgSleep != null) {
            list.add("平均睡眠：" + avgSleep + "小时");
        }

        Integer avgSteps = avgSteps(history);
        if (avgSteps != null && avgSteps > 0) {
            list.add("平均步数：" + avgSteps + "步");
        }

        String bpText = latestBloodPressureText(history);
        if (bpText != null) {
            list.add("最新血压：" + bpText);
        }

        return list;
    }

    /**
     * 生成预测消息
     */
    private String buildMessage(int predictedRiskScore,
                                String trend,
                                BigDecimal predictedWeight,
                                int historyCount,
                                boolean byPython) {
        String levelText = toRiskLevelText(toRiskLevel(predictedRiskScore));
        String trendText = toTrendText(trend);

        StringBuilder sb = new StringBuilder();
        sb.append("基于最近 ").append(historyCount).append(" 条体质记录，");
        sb.append("预测未来7天整体风险为 ").append(levelText);
        sb.append("（").append(predictedRiskScore).append("分）");

        if (predictedWeight != null) {
            sb.append("，预测体重约 ").append(predictedWeight).append("kg");
        }

        sb.append("，趋势判断为 ").append(trendText).append("。");
        sb.append(byPython ? "本次结果由 Python 模型生成。" : "当前为规则降级预测结果。");

        return sb.toString();
    }

    /**
     * 降级建议
     */
    private String buildFallbackSuggestion(String trend, int predictedRiskScore, List<HealthMetricRecord> history) {
        HealthMetricRecord latest = latestRecord(history);
        List<String> list = new ArrayList<>();

        if ("UP".equals(trend)) {
            list.add("预测体重有上升趋势，建议减少高糖高油饮食");
            list.add("每周增加 3~4 次有氧运动");
        } else if ("DOWN".equals(trend)) {
            list.add("预测体重有下降趋势，建议继续保持当前管理节奏");
            list.add("注意营养均衡，避免过度节食");
        } else {
            list.add("预测整体趋势较稳定，建议继续保持规律记录");
        }

        if (latest != null) {
            if (latest.getSleepHours() != null && latest.getSleepHours().compareTo(new BigDecimal("6")) < 0) {
                list.add("建议优先改善睡眠，尽量保证 6.5~8 小时");
            }
            if (latest.getSteps() != null && latest.getSteps() < 5000) {
                list.add("建议逐步把日均步数提升到 6000~8000 步");
            }
            if (latest.getSystolic() != null && latest.getSystolic() >= 140) {
                list.add("近期血压偏高，建议减少高盐饮食并持续复测");
            }
            if (latest.getBloodSugar() != null && latest.getBloodSugar().compareTo(new BigDecimal("7.0")) >= 0) {
                list.add("近期血糖偏高，建议控制精制碳水摄入");
            }
        }

        if (predictedRiskScore >= 60) {
            list.add("预测风险较高，建议近期优先处理主要异常指标");
        }

        return String.join("；", new LinkedHashSet<>(list));
    }

    private HealthMetricRecord latestRecord(List<HealthMetricRecord> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        return history.get(history.size() - 1);
    }

    private BigDecimal latestWeight(List<HealthMetricRecord> history) {
        HealthMetricRecord latest = latestRecord(history);
        return latest == null ? null : latest.getWeightKg();
    }

    private BigDecimal latestBmi(List<HealthMetricRecord> history) {
        HealthMetricRecord latest = latestRecord(history);
        return latest == null ? null : latest.getBmi();
    }

    private String latestBloodPressureText(List<HealthMetricRecord> history) {
        HealthMetricRecord latest = latestRecord(history);
        if (latest == null || latest.getSystolic() == null || latest.getDiastolic() == null) {
            return null;
        }
        return latest.getSystolic() + "/" + latest.getDiastolic();
    }

    private BigDecimal avgWeightDelta(List<HealthMetricRecord> history) {
        List<BigDecimal> weights = new ArrayList<>();
        for (HealthMetricRecord r : history) {
            if (r.getWeightKg() != null) {
                weights.add(r.getWeightKg());
            }
        }

        if (weights.size() < 2) {
            return null;
        }

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (int i = 1; i < weights.size(); i++) {
            sum = sum.add(weights.get(i).subtract(weights.get(i - 1)));
            count++;
        }

        if (count == 0) {
            return null;
        }

        return scale2(sum.divide(new BigDecimal(count), 4, RoundingMode.HALF_UP));
    }

    private BigDecimal avgSleep(List<HealthMetricRecord> history) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (HealthMetricRecord r : history) {
            if (r.getSleepHours() != null) {
                sum = sum.add(r.getSleepHours());
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        return scale2(sum.divide(new BigDecimal(count), 4, RoundingMode.HALF_UP));
    }

    private Integer avgSteps(List<HealthMetricRecord> history) {
        long sum = 0;
        int count = 0;
        for (HealthMetricRecord r : history) {
            if (r.getSteps() != null) {
                sum += r.getSteps();
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        return (int) Math.round(sum * 1.0 / count);
    }

    private BigDecimal calcConfidence(int historyCount, boolean pythonOk) {
        BigDecimal base;
        if (historyCount >= 10) {
            base = new BigDecimal("0.86");
        } else if (historyCount >= 7) {
            base = new BigDecimal("0.78");
        } else if (historyCount >= 5) {
            base = new BigDecimal("0.70");
        } else if (historyCount >= 3) {
            base = new BigDecimal("0.58");
        } else {
            base = new BigDecimal("0.45");
        }

        if (!pythonOk) {
            base = base.subtract(new BigDecimal("0.08"));
        }

        if (base.compareTo(new BigDecimal("0.30")) < 0) {
            base = new BigDecimal("0.30");
        }
        if (base.compareTo(new BigDecimal("0.95")) > 0) {
            base = new BigDecimal("0.95");
        }

        return scale2(base);
    }

    private String toRiskLevel(int score) {
        if (score >= 60) {
            return "HIGH";
        }
        if (score >= 30) {
            return "MID";
        }
        return "LOW";
    }

    private String toRiskLevelText(String level) {
        if ("HIGH".equals(level)) {
            return "高风险";
        }
        if ("MID".equals(level)) {
            return "中风险";
        }
        return "低风险";
    }

    private String toTrendText(String trend) {
        if ("UP".equals(trend)) {
            return "上升";
        }
        if ("DOWN".equals(trend)) {
            return "下降";
        }
        return "稳定";
    }

    private String normalizeTrend(String trend) {
        if ("UP".equalsIgnoreCase(trend)) {
            return "UP";
        }
        if ("DOWN".equalsIgnoreCase(trend)) {
            return "DOWN";
        }
        return "STABLE";
    }

    private Integer parseInt(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private BigDecimal parseDecimal(Object value) {
        if (value == null || "".equals(String.valueOf(value).trim())) {
            return null;
        }
        try {
            return scale2(new BigDecimal(String.valueOf(value)));
        } catch (Exception e) {
            return null;
        }
    }

    private String readString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    private BigDecimal scale2(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}