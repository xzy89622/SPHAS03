package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sphas.project03.controller.dto.RiskDashboardDTO;
import com.sphas.project03.entity.HealthRiskAlert;
import com.sphas.project03.mapper.HealthRiskAlertMapper;
import com.sphas.project03.service.RiskDashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 风险看板统计（告警表聚合 + 趋势）
 */
@Service
public class RiskDashboardServiceImpl implements RiskDashboardService {

    private final HealthRiskAlertMapper alertMapper;

    public RiskDashboardServiceImpl(HealthRiskAlertMapper alertMapper) {
        this.alertMapper = alertMapper;
    }

    @Override
    public RiskDashboardDTO dashboard(Long userId, int days) {
        // 参数限制：7~365
        if (days < 7) days = 7;
        if (days > 365) days = 365;

        LocalDateTime start = LocalDate.now()
                .minusDays(days - 1L)
                .atStartOfDay();

        // ============================================================
        // 1) 等级数量统计（LOW/MID/HIGH）
        //    SELECT risk_level, COUNT(*) cnt FROM health_risk_alert
        //    WHERE user_id=? AND create_time>=?
        //    GROUP BY risk_level
        // ============================================================
        QueryWrapper<HealthRiskAlert> levelQ = new QueryWrapper<>();
        levelQ.select("risk_level AS riskLevel", "COUNT(*) AS cnt")
                .eq("user_id", userId)
                .ge("create_time", start)
                .groupBy("risk_level");

        List<Map<String, Object>> levelRows = alertMapper.selectMaps(levelQ);

        // levelCounts 初始化，保证三个key始终存在
        Map<String, Long> levelCounts = new LinkedHashMap<>();
        levelCounts.put("LOW", 0L);
        levelCounts.put("MID", 0L);
        levelCounts.put("HIGH", 0L);

        long total = 0L;
        for (Map<String, Object> row : levelRows) {
            String level = String.valueOf(row.get("riskLevel"));
            long cnt = Long.parseLong(String.valueOf(row.get("cnt")));
            levelCounts.put(level, cnt);
            total += cnt; // ✅ 注意：必须累加，不能覆盖
        }

        // 计算占比 levelRatio
        Map<String, Double> levelRatio = new LinkedHashMap<>();
        for (Map.Entry<String, Long> e : levelCounts.entrySet()) {
            double ratio = (total == 0) ? 0.0 : (e.getValue() * 1.0 / total);
            // 保留 4 位小数
            ratio = Math.round(ratio * 10000.0) / 10000.0;
            levelRatio.put(e.getKey(), ratio);
        }

        // ============================================================
        // 2) 每日趋势（按天 + 风险等级）
        //    SELECT DATE_FORMAT(create_time,'%Y-%m-%d') d, risk_level, COUNT(*) cnt
        //    FROM health_risk_alert
        //    WHERE user_id=? AND create_time>=?
        //    GROUP BY d, risk_level
        //    ORDER BY d ASC
        // ============================================================
        QueryWrapper<HealthRiskAlert> dailyQ = new QueryWrapper<>();
        dailyQ.select("DATE_FORMAT(create_time,'%Y-%m-%d') AS d",
                        "risk_level AS riskLevel",
                        "COUNT(*) AS cnt")
                .eq("user_id", userId)
                .ge("create_time", start)
                .groupBy("d", "risk_level")
                .orderByAsc("d");

        List<Map<String, Object>> dailyRows = alertMapper.selectMaps(dailyQ);

        // 先补齐日期（每一天都有记录，哪怕是0）
        Map<String, Map<String, Object>> dayMap = new LinkedHashMap<>();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = days - 1; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).format(df);
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("date", date);
            one.put("LOW", 0L);
            one.put("MID", 0L);
            one.put("HIGH", 0L);
            one.put("total", 0L);
            dayMap.put(date, one);
        }

        // 填入聚合结果
        // 把真实查询结果合并回 dayMap
        for (Map<String, Object> row : dailyRows) {
            // 这里的 key 必须和 select 里的 as 别名一致
            String d = String.valueOf(row.get("d"));              // 例如 2026-01-29
            String level = String.valueOf(row.get("riskLevel"));  // LOW/MID/HIGH
            Long cnt = Long.parseLong(String.valueOf(row.get("cnt")));

            Map<String, Object> one = dayMap.get(d);
            if (one == null) continue;

            // 写入当前等级数量
            one.put(level, cnt);

            // 重新计算 total
            long low = Long.parseLong(String.valueOf(one.get("LOW")));
            long mid = Long.parseLong(String.valueOf(one.get("MID")));
            long high = Long.parseLong(String.valueOf(one.get("HIGH")));
            one.put("total", low + mid + high);
        }


        List<Map<String, Object>> daily = new ArrayList<>(dayMap.values());

        // ============================================================
        // 3) 最近一次 HIGH 的时间（可用于前端提示）
        //    SELECT create_time FROM health_risk_alert
        //    WHERE user_id=? AND risk_level='HIGH'
        //    ORDER BY create_time DESC LIMIT 1
        // ============================================================
        String latestHighTime = null;
        QueryWrapper<HealthRiskAlert> lastHighQ = new QueryWrapper<>();
        lastHighQ.select("create_time")
                .eq("user_id", userId)
                .eq("risk_level", "HIGH")
                .orderByDesc("create_time")
                .last("LIMIT 1");

        List<Map<String, Object>> lastHighRows = alertMapper.selectMaps(lastHighQ);
        if (!lastHighRows.isEmpty() && lastHighRows.get(0).get("create_time") != null) {
            latestHighTime = String.valueOf(lastHighRows.get(0).get("create_time"));
        }

        // ============================================================
        // 4) ✅ 生成“AI总结”（规则型自然语言总结）
        // ============================================================
        String aiConclusion = buildAiConclusion(days, total, levelCounts, levelRatio, latestHighTime);

        // ============================================================
        // 组装 DTO 返回
        // ============================================================
        RiskDashboardDTO dto = new RiskDashboardDTO();
        dto.setDays(days);
        dto.setTotal(total);
        dto.setLevelCounts(levelCounts);
        dto.setLevelRatio(levelRatio);
        dto.setDaily(daily);
        dto.setLatestHighTime(latestHighTime);

        // ✅ 新增：AI 看板总结
        dto.setAiConclusion(aiConclusion);
        dto.setLatestHighTime(latestHighTime);

        return dto;
    }

    /**
     * 风险看板 AI 总结（规则型NLG）
     * 目标：一句话/两句话让老师一眼看出“智能”
     */
    private String buildAiConclusion(int days,
                                     long total,
                                     Map<String, Long> levelCounts,
                                     Map<String, Double> levelRatio,
                                     String latestHighTime) {

        if (total <= 0) {
            return "近" + days + "天暂无风险评估记录，整体风险较低，建议保持良好生活习惯。";
        }

        long high = levelCounts.getOrDefault("HIGH", 0L);
        long mid = levelCounts.getOrDefault("MID", 0L);
        long low = levelCounts.getOrDefault("LOW", 0L);

        double highRatio = levelRatio.getOrDefault("HIGH", 0.0);

        StringBuilder sb = new StringBuilder();
        sb.append("近").append(days).append("天共记录").append(total).append("次风险评估，");

        if (high > 0) {
            sb.append("高风险").append(high).append("次（占比")
                    .append(Math.round(highRatio * 100)).append("%），");
            sb.append("建议优先关注血压、血糖、体重等关键指标并进行干预。");

            if (latestHighTime != null) {
                sb.append("最近一次高风险时间：").append(latestHighTime).append("。");
            }
        } else if (mid > 0) {
            sb.append("以中等风险为主（MID：").append(mid).append("次），");
            sb.append("建议持续改善饮食结构与运动频率，预防风险上升。");
        } else {
            sb.append("整体以低风险为主（LOW：").append(low).append("次），");
            sb.append("健康状况较稳定，建议继续保持规律作息与适度运动。");
        }

        return sb.toString();
    }
}
