package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.HealthConstants;
import com.sphas.project03.dto.AiPredictionDTO;
import com.sphas.project03.entity.HealthMetricRecord;
import com.sphas.project03.entity.HealthRiskAlert;
import com.sphas.project03.entity.SysMessage;
import com.sphas.project03.service.AiInsightService;
import com.sphas.project03.service.HealthMetricRecordService;
import com.sphas.project03.service.HealthRiskAlertService;
import com.sphas.project03.service.RiskService;
import com.sphas.project03.service.SysMessageService;
import com.sphas.project03.utils.PrivacyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 风险评估（规则引擎版）
 * 说明：规则简单可解释，适合答辩；并且把敏感原因加密存储 + 链式Hash模拟“区块链不可篡改”。
 */
@Service
public class RiskServiceImpl implements RiskService {

    private static final Logger log = LoggerFactory.getLogger(RiskServiceImpl.class);

    private final HealthMetricRecordService metricService;
    private final HealthRiskAlertService alertService;
    private final SysMessageService sysMessageService;
    private final AiInsightService aiInsightService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RiskServiceImpl(HealthMetricRecordService metricService,
                           HealthRiskAlertService alertService,
                           SysMessageService sysMessageService,
                           AiInsightService aiInsightService) {
        this.metricService = metricService;
        this.alertService = alertService;
        this.sysMessageService = sysMessageService;
        this.aiInsightService = aiInsightService;
    }

    @Override
    public Map<String, Object> evaluateAndSave(Long userId) {

        // 1) 取最新一条体质记录
        HealthMetricRecord latest = metricService.getOne(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .orderByDesc(HealthMetricRecord::getRecordTime)
                        .last("limit 1")
        );
        if (latest == null) {
            throw new BizException("暂无体质记录，请先录入体质数据");
        }

        // 2) 取上一条（用于趋势风险）
        HealthMetricRecord prev = metricService.getOne(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .orderByDesc(HealthMetricRecord::getRecordTime)
                        .last("limit 1,1")
        );

        int score = 0; // 0~100
        List<String> reasons = new ArrayList<>();
        List<String> adviceList = new ArrayList<>();

        // ========== A. BMI 规则 ==========
        if (latest.getBmi() != null) {
            BigDecimal bmi = latest.getBmi();

            if (bmi.compareTo(new BigDecimal("28")) >= 0) {
                score += 35;
                reasons.add("BMI偏高（肥胖）");
                adviceList.add("建议控制总热量摄入，增加有氧+力量训练，持续监测体重变化");
            } else if (bmi.compareTo(new BigDecimal("24")) >= 0) {
                score += 18;
                reasons.add("BMI偏高（超重）");
                adviceList.add("建议减少高糖高油饮食，保持每周≥3次有氧运动");
            } else if (bmi.compareTo(new BigDecimal("18.5")) < 0) {
                score += 10;
                reasons.add("BMI偏低（偏瘦）");
                adviceList.add("建议适当增加优质蛋白与力量训练，避免过度节食");
            }
        }

        // ========== B. 血压规则 ==========
        Integer sys = latest.getSystolic();
        Integer dia = latest.getDiastolic();
        if (sys != null && dia != null) {
            if (sys >= HealthConstants.BP_SYS_HIGH || dia >= HealthConstants.BP_DIA_HIGH) {
                score += 35;
                reasons.add("血压偏高（需就医评估）");
                adviceList.add("建议严格控制盐摄入，规律作息；如长期偏高建议就医评估");
            } else if (sys >= HealthConstants.BP_SYS_MID || dia >= HealthConstants.BP_DIA_MID) {
                score += 18;
                reasons.add("血压处于临界值");
                adviceList.add("建议减少盐摄入、增加运动、每日监测血压趋势");
            }
        }

        // ========== C. 血糖规则 ==========
        BigDecimal sugar = latest.getBloodSugar();
        if (sugar != null) {
            if (sugar.compareTo(new BigDecimal(HealthConstants.SUGAR_HIGH)) >= 0) {
                score += 30;
                reasons.add("血糖偏高");
                adviceList.add("建议严格控制精制碳水摄入，餐后适度运动");
            } else if (sugar.compareTo(new BigDecimal(HealthConstants.SUGAR_MID)) >= 0) {
                score += 12;
                reasons.add("血糖临界偏高");
                adviceList.add("建议减少含糖饮料与甜食，保持运动与作息规律");
            }
        }

        // ========== D. 趋势规则 ==========
        if (prev != null) {
            if (latest.getBmi() != null && prev.getBmi() != null && latest.getBmi().compareTo(prev.getBmi()) > 0) {
                score += 6;
                reasons.add("BMI较上次上升");
            }
            if (sys != null && prev.getSystolic() != null && sys > prev.getSystolic()) {
                score += 6;
                reasons.add("收缩压较上次上升");
            }
            if (dia != null && prev.getDiastolic() != null && dia > prev.getDiastolic()) {
                score += 6;
                reasons.add("舒张压较上次上升");
            }
            if (sugar != null && prev.getBloodSugar() != null && sugar.compareTo(prev.getBloodSugar()) > 0) {
                score += 6;
                reasons.add("血糖较上次上升");
            }
        }

        // ========== E. 生活方式风险 ==========
        if (latest.getSleepHours() != null && latest.getSleepHours().compareTo(new BigDecimal("6")) < 0) {
            score += 8;
            reasons.add("睡眠不足（<6小时）");
            adviceList.add("建议保持规律作息，逐步提高到6.5-8小时睡眠");
        }
        if (latest.getSteps() != null && latest.getSteps() < 5000) {
            score += 8;
            reasons.add("日均步数偏低（<5000）");
            adviceList.add("建议每天增加步行量，逐步达到≥6000-8000步");
        }

        // 分数封顶
        score = Math.min(score, 100);

        // 风险等级
        String level;
        if (score >= 60) level = "HIGH";
        else if (score >= 30) level = "MID";
        else level = "LOW";

        // 建议（去重）
        String advice = String.join("；", new LinkedHashSet<>(adviceList));
        if (advice.trim().isEmpty()) {
            advice = "整体健康风险较低，建议保持良好饮食与运动习惯";
        }

        // ===================== AI 解读 + 预测 =====================
        String aiSummary = aiInsightService.buildSummary(level, score, reasons, advice);
        AiPredictionDTO aiPrediction = aiInsightService.predict7Days(userId, score);

        String aiPredictionJson;
        try {
            aiPredictionJson = objectMapper.writeValueAsString(aiPrediction);
        } catch (Exception e) {
            aiPredictionJson = null;
        }

        // ===================== 落库：health_risk_alert =====================
        saveAlert(userId, latest.getId(), level, score, reasons, advice, aiSummary, aiPredictionJson);

        // ===================== HIGH 风险：实时预警（站内消息）====================
        if ("HIGH".equals(level)) {
            pushHighRiskMessageOncePerDay(userId, reasons, advice);
        }

        // 返回给前端
        Map<String, Object> res = new HashMap<>();
        res.put("riskLevel", level);
        res.put("riskScore", score);
        res.put("reasons", reasons);
        res.put("advice", advice);
        res.put("latestRecordId", latest.getId());
        res.put("aiSummary", aiSummary);
        res.put("aiPrediction", aiPrediction);

        return res;
    }

    /**
     * 保存预警记录（含：原因加密 + 链式Hash）
     */
    private void saveAlert(Long userId,
                           Long sourceRecordId,
                           String level,
                           int score,
                           List<String> reasons,
                           String advice,
                           String aiSummary,
                           String aiPredictionJson) {

        try {
            // 1) 找到上一条预警，拿到 prevHash（形成链）
            HealthRiskAlert last = alertService.getOne(
                    new LambdaQueryWrapper<HealthRiskAlert>()
                            .eq(HealthRiskAlert::getUserId, userId)
                            .orderByDesc(HealthRiskAlert::getCreateTime)
                            .last("limit 1")
            );

            String prevHash = (last == null || last.getBlockHash() == null || last.getBlockHash().trim().isEmpty())
                    ? "0000000000000000"
                    : last.getBlockHash();

            // 2) 原因 JSON -> 加密存储
            String rawReasons = objectMapper.writeValueAsString(reasons);
            String encryptedReasons = PrivacyUtil.encrypt(rawReasons);

            // 3) 计算本次 blockHash
            String blockHash = PrivacyUtil.calculateBlockHash(userId, score, rawReasons, prevHash);

            // 4) 落库
            HealthRiskAlert alert = new HealthRiskAlert();
            alert.setUserId(userId);
            alert.setRiskLevel(level);
            alert.setRiskScore(score);
            alert.setPrevHash(prevHash);
            alert.setBlockHash(blockHash);
            alert.setReasonsJson(encryptedReasons);
            alert.setAdvice(advice);
            alert.setSourceRecordId(sourceRecordId);
            alert.setAiSummary(aiSummary);
            alert.setAiPredictionJson(aiPredictionJson);
            alert.setCreateTime(LocalDateTime.now());

            alertService.save(alert);

            // 打个日志，便于你演示“区块链指纹”
            log.info("【数据上链】userId={}, prevHash={}, blockHash={}", userId, prevHash, blockHash);

        } catch (Exception e) {
            log.error("用户 {} 风险评估记录落库失败", userId, e);
        }
    }

    /**
     * HIGH 风险：同一天只发一次站内提醒
     */
    private void pushHighRiskMessageOncePerDay(Long userId, List<String> reasons, String advice) {

        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();

        long sent = sysMessageService.count(
                new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getUserId, userId)
                        .eq(SysMessage::getType, "RISK")
                        .ge(SysMessage::getCreateTime, todayStart)
        );
        if (sent > 0) return;

        SysMessage m = new SysMessage();
        m.setUserId(userId);
        m.setType("RISK");
        m.setTitle("【风险预警】今日检测到较高健康风险");
        m.setContent("触发原因：" + String.join("、", reasons) + "\n建议：" + advice);
        m.setIsRead(0);
        m.setCreateTime(LocalDateTime.now());

        sysMessageService.save(m);
    }
}
