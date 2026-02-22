package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.HealthConstants;
import com.sphas.project03.controller.dto.AiPredictionDTO;
import com.sphas.project03.entity.HealthMetricRecord;
import com.sphas.project03.entity.HealthRiskAlert;
import com.sphas.project03.entity.Notice;
import com.sphas.project03.entity.SysMessage;
import com.sphas.project03.service.*;
import com.sphas.project03.utils.PrivacyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 风险评估（规则引擎版）
 * 说明：HIGH风险时写入站内消息（同一天只发一次）
 */
@Service
public class RiskServiceImpl implements RiskService {

    private static final Logger log = LoggerFactory.getLogger(RiskServiceImpl.class);

    private final HealthMetricRecordService metricService;
    private final HealthRiskAlertService alertService;
    private final NoticeService noticeService;
    private final AiInsightService aiInsightService;
    private final SysMessageService sysMessageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RiskServiceImpl(HealthMetricRecordService metricService,
                           HealthRiskAlertService alertService,
                           NoticeService noticeService,
                           AiInsightService aiInsightService,
                           SysMessageService sysMessageService) {
        this.metricService = metricService;
        this.alertService = alertService;
        this.noticeService = noticeService;
        this.aiInsightService = aiInsightService;
        this.sysMessageService = sysMessageService;
    }

    @Override
    public Map<String, Object> evaluateAndSave(Long userId) {

        // 1) 最新体质记录
        HealthMetricRecord latest = metricService.getOne(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .orderByDesc(HealthMetricRecord::getRecordTime)
                        .last("limit 1")
        );
        if (latest == null) throw new BizException("暂无体质记录，请先录入体质数据");

        // 2) 上一条记录（用于趋势判断）
        HealthMetricRecord prev = metricService.getOne(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .orderByDesc(HealthMetricRecord::getRecordTime)
                        .last("limit 1,1")
        );

        int score = 0;
        List<String> reasons = new ArrayList<>();
        List<String> adviceList = new ArrayList<>();

        // A. BMI
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

        // B. 血压
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

        // C. 血糖
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

        // D. 趋势（变差加分）
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

        // E. 生活方式
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

        score = Math.min(score, 100);

        String level;
        if (score >= 60) level = "HIGH";
        else if (score >= 30) level = "MID";
        else level = "LOW";

        // 建议拼接（去重）
        String advice = String.join("；", new LinkedHashSet<>(adviceList));
        if (advice.isEmpty()) advice = "整体健康风险较低，建议保持良好饮食与运动习惯";

        // AI 解读 + 预测
        String aiSummary = aiInsightService.buildSummary(level, score, reasons, advice);

        AiPredictionDTO aiPrediction = aiInsightService.predict7Days(userId, score);
        String aiPredictionJson;
        try {
            aiPredictionJson = objectMapper.writeValueAsString(aiPrediction);
        } catch (Exception e) {
            aiPredictionJson = null;
        }

        // ========== 落库 health_risk_alert（含链式hash） ==========
        HealthRiskAlert last = alertService.getOne(new LambdaQueryWrapper<HealthRiskAlert>()
                .eq(HealthRiskAlert::getUserId, userId)
                .orderByDesc(HealthRiskAlert::getCreateTime)
                .last("limit 1"));

        String prevHash = (last == null || last.getBlockHash() == null) ? "GENESIS" : last.getBlockHash();

        try {
            HealthRiskAlert alert = new HealthRiskAlert();
            alert.setUserId(userId);
            alert.setRiskLevel(level);
            alert.setRiskScore(score);

            // 原因加密存储
            String rawReasons = objectMapper.writeValueAsString(reasons);
            alert.setReasonsJson(PrivacyUtil.encrypt(rawReasons));

            // 生成区块hash（模拟可追溯）
            String blockHash = PrivacyUtil.calculateBlockHash(userId, score, rawReasons, prevHash);
            alert.setPrevHash(prevHash);
            alert.setBlockHash(blockHash);

            alert.setAdvice(advice);
            alert.setSourceRecordId(latest.getId());
            alert.setAiSummary(aiSummary);
            alert.setAiPredictionJson(aiPredictionJson);
            alert.setCreateTime(LocalDateTime.now());

            alertService.save(alert);
        } catch (Exception e) {
            log.error("用户 {} 风险评估落库失败", userId, e);
        }

        // ========== HIGH 风险实时预警：写 sys_message（同一天只发一次）==========
        if ("HIGH".equals(level)) {
            LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();

            long sent = sysMessageService.count(new LambdaQueryWrapper<SysMessage>()
                    .eq(SysMessage::getUserId, userId)
                    .eq(SysMessage::getType, "RISK")
                    .ge(SysMessage::getCreateTime, todayStart));

            if (sent == 0) {
                // 1) 写站内消息（小程序端拉取用）
                SysMessage m = new SysMessage();
                m.setUserId(userId);
                m.setType("RISK");
                m.setTitle("【风险预警】今日检测到较高健康风险");
                m.setContent("触发原因：" + String.join("、", reasons) + "\n建议：" + advice);
                m.setIsRead(0);
                m.setCreateTime(LocalDateTime.now());
                sysMessageService.save(m);

                // 2) 可选：联动公告（你原本就有）
                Notice n = new Notice();
                n.setTitle("【健康风险预警】检测到较高风险，请及时关注");
                n.setContent("触发原因：" + String.join("、", reasons) + "\n建议：" + advice + "\n（系统自动生成）");
                n.setStatus(1);
                noticeService.save(n);
            }
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
}