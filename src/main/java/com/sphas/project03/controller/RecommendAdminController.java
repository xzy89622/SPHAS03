package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.DietPlan;
import com.sphas.project03.entity.SportPlan;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.entity.UserRecommendation;
import com.sphas.project03.mapper.UserRecommendationMapper;
import com.sphas.project03.service.DietPlanService;
import com.sphas.project03.service.SportPlanService;
import com.sphas.project03.service.SysUserService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端：推荐记录查看
 */
@RestController
@RequestMapping("/api/recommend/admin")
public class RecommendAdminController extends BaseController {

    private final UserRecommendationMapper userRecommendationMapper;
    private final SysUserService sysUserService;
    private final DietPlanService dietPlanService;
    private final SportPlanService sportPlanService;

    public RecommendAdminController(UserRecommendationMapper userRecommendationMapper,
                                    SysUserService sysUserService,
                                    DietPlanService dietPlanService,
                                    SportPlanService sportPlanService) {
        this.userRecommendationMapper = userRecommendationMapper;
        this.sysUserService = sysUserService;
        this.dietPlanService = dietPlanService;
        this.sportPlanService = sportPlanService;
    }

    /**
     * 推荐记录分页
     */
    @GetMapping("/page")
    public R<Page<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String bmiLevel,
                                             HttpServletRequest request) {
        requireAdmin(request);

        Page<UserRecommendation> rawPage = userRecommendationMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UserRecommendation>()
                        .orderByDesc(UserRecommendation::getCreateTime)
                        .orderByDesc(UserRecommendation::getId)
        );

        List<Map<String, Object>> records = rawPage.getRecords().stream()
                .map(this::buildRow)
                .filter(row -> matchKeyword(row, keyword) && matchBmiLevel(row, bmiLevel))
                .collect(Collectors.toList());

        Page<Map<String, Object>> res = new Page<>(pageNum, pageSize);
        res.setCurrent(rawPage.getCurrent());
        res.setSize(rawPage.getSize());
        res.setTotal(rawPage.getTotal());
        res.setRecords(records);
        return R.ok(res);
    }

    private boolean matchKeyword(Map<String, Object> row, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String kw = keyword.trim();
        String username = String.valueOf(row.getOrDefault("username", ""));
        String nickname = String.valueOf(row.getOrDefault("nickname", ""));
        String dietPlanName = String.valueOf(row.getOrDefault("dietPlanName", ""));
        String sportPlanName = String.valueOf(row.getOrDefault("sportPlanName", ""));
        String reason = String.valueOf(row.getOrDefault("reason", ""));
        return username.contains(kw)
                || nickname.contains(kw)
                || dietPlanName.contains(kw)
                || sportPlanName.contains(kw)
                || reason.contains(kw);
    }

    private boolean matchBmiLevel(Map<String, Object> row, String bmiLevel) {
        if (!StringUtils.hasText(bmiLevel)) {
            return true;
        }
        return bmiLevel.trim().equals(String.valueOf(row.get("bmiLevel")));
    }

    private Map<String, Object> buildRow(UserRecommendation rec) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", rec.getId());
        row.put("userId", rec.getUserId());
        row.put("bmi", rec.getBmi());
        row.put("bmiLevel", rec.getBmiLevel());
        row.put("scoresJson", rec.getScoresJson());
        row.put("reason", rec.getReason());
        row.put("createTime", rec.getCreateTime());

        SysUser user = rec.getUserId() == null ? null : sysUserService.getById(rec.getUserId());
        DietPlan dietPlan = rec.getDietPlanId() == null ? null : dietPlanService.getById(rec.getDietPlanId());
        SportPlan sportPlan = rec.getSportPlanId() == null ? null : sportPlanService.getById(rec.getSportPlanId());

        row.put("username", user == null ? "" : user.getUsername());
        row.put("nickname", user == null ? "" : user.getNickname());
        row.put("phone", user == null ? "" : user.getPhone());

        row.put("dietPlanId", rec.getDietPlanId());
        row.put("dietPlanName", dietPlan == null ? "" : dietPlan.getName());

        row.put("sportPlanId", rec.getSportPlanId());
        row.put("sportPlanName", sportPlan == null ? "" : sportPlan.getName());

        return row;
    }
}