package com.sphas.project03.service;

import com.sphas.project03.controller.dto.WeeklyReportDTO;
import com.sphas.project03.controller.dto.MonthlyReportDTO;

public interface HealthReportService {

    WeeklyReportDTO weekly(Long userId); // 生成最近7天周报

    byte[] weeklyPdf(Long userId);       // 生成周报PDF

    MonthlyReportDTO monthly(Long userId); // 生成最近30天月报

    byte[] monthlyPdf(Long userId);        // 生成月报PDF
}