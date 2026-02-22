package com.sphas.project03.controller;

import com.sphas.project03.common.R;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理端：全站统计（给后台看板柱状图/饼图用）
 * ✅ 直接查表，不依赖 service 命名
 */
@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController extends BaseController {

    private final JdbcTemplate jdbcTemplate;

    public AdminStatsController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 全站概览统计
     * GET /api/admin/stats/summary
     */
    @GetMapping("/summary")
    public R<Map<String, Object>> summary(HttpServletRequest request) {

        // ✅ 管理员权限
        requireAdmin(request);

        Map<String, Object> res = new HashMap<>();

        // 1) 用户
        Integer userTotal = qInt("select count(*) from sys_user where role='USER'");
        Integer adminTotal = qInt("select count(*) from sys_user where role='ADMIN'");
        res.put("userTotal", userTotal);
        res.put("adminTotal", adminTotal);

        // 2) 内容/业务
        res.put("noticeTotal", qInt("select count(*) from notice"));
        res.put("articleTotal", qInt("select count(*) from health_article"));
        res.put("feedbackTotal", qInt("select count(*) from feedback"));
        res.put("questionTotal", qInt("select count(*) from question_bank"));
        res.put("challengeTotal", qInt("select count(*) from challenge"));
        res.put("postTotal", qInt("select count(*) from social_post"));

        // 3) 社区审核状态分布（饼图）
        // status：0隐藏 1通过 2待审 3驳回
        Map<String, Object> postStatus = new HashMap<>();
        postStatus.put("hidden", qInt("select count(*) from social_post where status=0"));
        postStatus.put("approved", qInt("select count(*) from social_post where status=1"));
        postStatus.put("pending", qInt("select count(*) from social_post where status=2"));
        postStatus.put("rejected", qInt("select count(*) from social_post where status=3"));
        res.put("postStatus", postStatus);

        // 4) 积分流水数量（证明“积分激励系统”）
        res.put("pointRecordTotal", qInt("select count(*) from point_record"));

        // 5) 风险预警数量（证明“风险预警逻辑”）
        res.put("riskAlertTotal", qInt("select count(*) from health_risk_alert"));

        return R.ok(res);
    }

    private Integer qInt(String sql) {
        Integer v = jdbcTemplate.queryForObject(sql, Integer.class);
        return v == null ? 0 : v;
    }
}