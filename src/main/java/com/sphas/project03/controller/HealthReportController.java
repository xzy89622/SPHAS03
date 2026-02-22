package com.sphas.project03.controller;

import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.MonthlyReportDTO;
import com.sphas.project03.controller.dto.WeeklyReportDTO;
import com.sphas.project03.service.HealthReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 健康报告接口（需要登录）
 */
@RestController
@RequestMapping("/api/health/report")
public class HealthReportController {

    private final HealthReportService healthReportService;

    public HealthReportController(HealthReportService healthReportService) {
        this.healthReportService = healthReportService;
    }

    @GetMapping("/weekly")
    public R<WeeklyReportDTO> weekly(HttpServletRequest request) {
        Long userId = Long.valueOf(String.valueOf(request.getAttribute("userId")));
        return R.ok(healthReportService.weekly(userId));
    }

    @GetMapping("/monthly")
    public R<MonthlyReportDTO> monthly(HttpServletRequest request) {
        Long userId = Long.valueOf(String.valueOf(request.getAttribute("userId")));
        return R.ok(healthReportService.monthly(userId));
    }

    @GetMapping("/weekly/pdf")
    public void weeklyPdf(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long userId = Long.valueOf(String.valueOf(request.getAttribute("userId")));

        byte[] pdf = healthReportService.weeklyPdf(userId);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=weekly-report.pdf");
        response.getOutputStream().write(pdf); // 输出PDF
    }

    @GetMapping("/monthly/pdf")
    public void monthlyPdf(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long userId = Long.valueOf(String.valueOf(request.getAttribute("userId")));

        byte[] pdf = healthReportService.monthlyPdf(userId);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=monthly-report.pdf");
        response.getOutputStream().write(pdf);
    }
}