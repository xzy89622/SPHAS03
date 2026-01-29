package com.sphas.project03.service;

import java.util.Map;

/**
 * 风险评估（规则引擎版）
 */
public interface RiskService {

    /**
     * 对用户最新体质记录进行风险评估，并落库预警记录
     * 返回：riskLevel/riskScore/reasons/advice
     */
    Map<String, Object> evaluateAndSave(Long userId);
}

