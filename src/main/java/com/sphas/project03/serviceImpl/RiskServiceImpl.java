package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.entity.HealthMetricRecord;
import com.sphas.project03.entity.HealthRiskAlert;
import com.sphas.project03.service.HealthMetricRecordService;
import com.sphas.project03.service.HealthRiskAlertService;
import com.sphas.project03.service.RiskService;
import com.sphas.project03.utils.PrivacyUtil;
import org.springframework.stereotype.Service;
import com.sphas.project03.entity.Notice;
import com.sphas.project03.service.NoticeService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import com.sphas.project03.service.AiInsightService;
import com.sphas.project03.dto.AiPredictionDTO;
import com.sphas.project03.dto.AiPredictionDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sphas.project03.common.BizException;
// 【新增】引入常量类，统一标准
import com.sphas.project03.common.HealthConstants;
import com.sphas.project03.entity.HealthMetricRecord;
import com.sphas.project03.entity.HealthRiskAlert;
import com.sphas.project03.entity.Notice;
// 【新增】引入隐私加密工具
import com.sphas.project03.utils.PrivacyUtil;
import com.sphas.project03.service.*; // (保持原有)
import org.springframework.stereotype.Service;
// 【新增】引入日志工具
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
/**
 * 风险评估（规则引擎版）
 * 规则建议：简单、可解释、可答辩
 */
@Service
public class RiskServiceImpl implements RiskService {

    private final HealthMetricRecordService metricService;
    private final HealthRiskAlertService alertService;
    private final NoticeService noticeService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiInsightService aiInsightService;
    // 【新增】添加日志对象，方便排查问题
    private static final Logger log = LoggerFactory.getLogger(RiskServiceImpl.class);

    public RiskServiceImpl(HealthMetricRecordService metricService,
                           HealthRiskAlertService alertService,
                           NoticeService noticeService,
                           AiInsightService aiInsightService) {
        this.metricService = metricService;
        this.alertService = alertService;
        this.noticeService = noticeService;
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

        // 2) 取上一条（用于“趋势风险”：变差则加分）
        HealthMetricRecord prev = metricService.getOne(
                new LambdaQueryWrapper<HealthMetricRecord>()
                        .eq(HealthMetricRecord::getUserId, userId)
                        .orderByDesc(HealthMetricRecord::getRecordTime)
                        .last("limit 1,1") // MySQL: 从第2条开始取1条（上一条）
        );

        int score = 0; // 0~100
        List<String> reasons = new ArrayList<>();
        List<String> adviceList = new ArrayList<>();

        // ========== A. BMI 规则 ==========
        if (latest.getBmi() != null) {
            BigDecimal bmi = latest.getBmi();

            // 参考常见区间：>=28 肥胖，高风险；>=24 超重，中风险
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
            // 使用常量判断，确保与分析模块标准一致
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
        // ========== C. 血糖规则 ==========
        BigDecimal sugar = latest.getBloodSugar();
        if (sugar != null) {
            // 依据毕设通用阈值判断
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
        // ========== D. 趋势规则（上一条变差则加分） ==========
        if (prev != null) {
            // BMI 上升
            if (latest.getBmi() != null && prev.getBmi() != null
                    && latest.getBmi().compareTo(prev.getBmi()) > 0) {
                score += 6;
                reasons.add("BMI较上次上升");
            }
            // 血压上升
            if (sys != null && prev.getSystolic() != null && sys > prev.getSystolic()) {
                score += 6;
                reasons.add("收缩压较上次上升");
            }
            if (dia != null && prev.getDiastolic() != null && dia > prev.getDiastolic()) {
                score += 6;
                reasons.add("舒张压较上次上升");
            }
            // 血糖上升
            if (sugar != null && prev.getBloodSugar() != null
                    && sugar.compareTo(prev.getBloodSugar()) > 0) {
                score += 6;
                reasons.add("血糖较上次上升");
            }
        }

        // ========== E. 睡眠/步数（生活方式风险） ==========
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

      // 把建议列表拼成一个字符串（去重）
        String advice = String.join("；", new LinkedHashSet<>(adviceList));
        if (advice.isEmpty())  {
            advice = "整体健康风险较低，建议保持良好饮食与运动习惯";
        }

        // ======================= HIGH 风险自动通知（公告联动）=======================
        if ("HIGH".equals(level)) {

            // 同一天只推送一次 HIGH（防刷屏）
            LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
            boolean hasHighToday = alertService.count(
                    new LambdaQueryWrapper<HealthRiskAlert>()
                            .eq(HealthRiskAlert::getUserId, userId)
                            .eq(HealthRiskAlert::getRiskLevel, "HIGH")
                            .ge(HealthRiskAlert::getCreateTime, todayStart)
            ) > 0;

            if (!hasHighToday) {
                String title = "【健康风险预警】检测到较高风险，请及时关注";
                String content = "触发原因：" + String.join("、", reasons)
                        + "。\n建议：" + advice
                        + "。\n（系统自动生成）";

                // 注意：Notice 对象只能在这里 new
                Notice n = new Notice();
                n.setTitle(title);
                n.setContent(content);
                n.setStatus(1);

                noticeService.save(n);
            }
        }

//        // 建议拼接（去重）
//        String advice = String.join("；", new LinkedHashSet<>(adviceList));
//        if (advice.trim().isEmpty()) advice = "整体风险较低，建议保持良好饮食、运动与作息习惯";
        // ===================== AI 解读 + 预测（生成） =====================
// 1) AI 解读（自然语言总结）
        String aiSummary = aiInsightService.buildSummary(level, score, reasons, advice);

// 2) AI 预测（DTO） -> 转成 JSON 字符串，方便落库
        AiPredictionDTO aiPrediction = aiInsightService.predict7Days(userId, score);
        String aiPredictionJson;
        try {
            aiPredictionJson = objectMapper.writeValueAsString(aiPrediction);
        } catch (Exception e) {
            // 兜底：不因为 AI JSON 序列化失败影响主流程
            aiPredictionJson = null;
        }

// ===================== 落库：health_risk_alert =====================
        // ===================== 落库：health_risk_alert =====================
        try {
            HealthRiskAlert alert = new HealthRiskAlert();
            alert.setUserId(userId);
            alert.setRiskLevel(level);
            alert.setRiskScore(score);

            // 1. 敏感数据处理：原因包含隐私，进行AES加密存储
            String rawReasons = objectMapper.writeValueAsString(reasons);
            String encryptedReasons = PrivacyUtil.encrypt(rawReasons);
            alert.setReasonsJson(encryptedReasons);

            // 2. 模拟区块链特性：生成哈希摘要，确保数据未被篡改
            // (这里模拟上链过程，实际可存入 blockchain_hash 字段)
            String blockHash = PrivacyUtil.calculateBlockHash(userId, score, rawReasons, "PREV_HASH_GENESIS");
            log.info("【数据上链】风险评估记录已生成，区块Hash：{}", blockHash);

            // 建议（明文保存以便展示）
            alert.setAdvice(advice);

            // 关联来源记录
            alert.setSourceRecordId(latest.getId());

            // AI 字段落库
            alert.setAiSummary(aiSummary);
            alert.setAiPredictionJson(aiPredictionJson);

            // 创建时间
            alert.setCreateTime(LocalDateTime.now());

            alertService.save(alert);
        } catch (Exception e) {
            // 记录错误日志，防止异常被吞掉导致前端无响应
            log.error("用户 {} 风险评估记录落库失败", userId, e);
            // 这里不抛出异常，保证评估结果能正常返回给前端展示
        }

        // 4) 返回给前端
        Map<String, Object> res = new HashMap<>();
        res.put("riskLevel", level);
        res.put("riskScore", score);
        res.put("reasons", reasons);
        res.put("advice", advice);
        res.put("latestRecordId", latest.getId());

// ====== AI 解读 + 预测 ======
        res.put("aiSummary", aiSummary);
        res.put("aiPrediction", aiPrediction);

        return res;


    }
}
