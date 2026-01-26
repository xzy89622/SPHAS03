package com.sphas.project03.service;

import com.sphas.project03.controller.dto.WeeklyReportDTO;

public interface HealthReportService {

    WeeklyReportDTO weekly(Long userId); // 生成最近7天周报

    byte[] weeklyPdf(Long userId);       // 生成周报PDF
}

