package com.sphas.project03.service;

import com.sphas.project03.controller.dto.HealthAnalysisDTO;

public interface HealthAnalysisService {

    HealthAnalysisDTO analyzeLatest(Long userId);
}

