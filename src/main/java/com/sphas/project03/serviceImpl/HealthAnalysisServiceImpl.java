package com.sphas.project03.serviceImpl;

import com.sphas.project03.controller.dto.HealthAnalysisDTO;
import com.sphas.project03.entity.HealthRecord;
import com.sphas.project03.service.HealthAnalysisService;
import com.sphas.project03.service.HealthRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 健康分析业务
 */
@Service
public class HealthAnalysisServiceImpl implements HealthAnalysisService {

    private final HealthRecordService healthRecordService;

    public HealthAnalysisServiceImpl(HealthRecordService healthRecordService) {
        this.healthRecordService = healthRecordService;
    }

    @Override
    public HealthAnalysisDTO analyzeLatest(Long userId) {

        List<HealthRecord> list = healthRecordService.listLatest(userId, 7);
        HealthAnalysisDTO dto = new HealthAnalysisDTO();

        if (list.isEmpty()) {
            return dto; // 没数据直接返回空分析
        }

        HealthRecord latest = list.get(0);

        // ===== BMI =====
        if (latest.getHeightCm() != null && latest.getWeightKg() != null) {
            double h = latest.getHeightCm() / 100.0;
            double bmi = latest.getWeightKg() / (h * h);
            dto.setBmi(round(bmi));
            dto.setBmiLevel(bmiLevel(bmi));
        }

        // ===== 血压 =====
        if (latest.getSystolic() != null && latest.getDiastolic() != null) {
            int s = latest.getSystolic();
            int d = latest.getDiastolic();
            if (s < 130 && d < 85) {
                dto.setBloodPressure("正常");
                dto.setBloodPressureRisk(false);
            } else {
                dto.setBloodPressure("偏高");
                dto.setBloodPressureRisk(true);
            }
        }

        // ===== 睡眠 / 步数 =====
        dto.setSleepEnough(latest.getSleepHours() != null && latest.getSleepHours() >= 7);
        dto.setStepsEnough(latest.getSteps() != null && latest.getSteps() >= 8000);

        // ===== 体重趋势 =====
        if (list.size() >= 2) {
            double diff = list.get(0).getWeightKg() - list.get(list.size() - 1).getWeightKg();
            if (Math.abs(diff) < 0.5) dto.setWeightTrend("稳定");
            else if (diff > 0) dto.setWeightTrend("上升");
            else dto.setWeightTrend("下降");
        }

        // ===== 总结 =====
        dto.setRiskSummary(buildSummary(dto));

        return dto;
    }

    private String bmiLevel(double bmi) {
        if (bmi < 18.5) return "偏瘦";
        if (bmi < 24) return "正常";
        if (bmi < 28) return "超重";
        return "肥胖";
    }

    private double round(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private String buildSummary(HealthAnalysisDTO dto) {
        if (Boolean.TRUE.equals(dto.getBloodPressureRisk())) {
            return "血压偏高，建议注意饮食与作息";
        }
        if ("超重".equals(dto.getBmiLevel()) || "肥胖".equals(dto.getBmiLevel())) {
            return "体重偏高，建议适当运动";
        }
        return "整体状态良好";
    }
}

