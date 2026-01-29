package com.sphas.project03.service;

import com.sphas.project03.controller.dto.RiskDashboardDTO;

public interface RiskDashboardService {

    /**
     * 风险看板数据（近N天）
     */
    RiskDashboardDTO dashboard(Long userId, int days);
}

