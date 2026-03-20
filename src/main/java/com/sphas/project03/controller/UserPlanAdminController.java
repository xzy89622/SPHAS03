package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.DietPlan;
import com.sphas.project03.entity.SportPlan;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.entity.UserPlan;
import com.sphas.project03.entity.UserPlanCheckin;
import com.sphas.project03.service.DietPlanService;
import com.sphas.project03.service.SportPlanService;
import com.sphas.project03.service.SysUserService;
import com.sphas.project03.service.UserPlanCheckinService;
import com.sphas.project03.service.UserPlanService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端：用户执行计划查看
 */
@RestController
@RequestMapping("/api/user-plan/admin")
public class UserPlanAdminController extends BaseController {

    private final UserPlanService userPlanService;
    private final UserPlanCheckinService userPlanCheckinService;
    private final SysUserService sysUserService;
    private final DietPlanService dietPlanService;
    private final SportPlanService sportPlanService;

    public UserPlanAdminController(UserPlanService userPlanService,
                                   UserPlanCheckinService userPlanCheckinService,
                                   SysUserService sysUserService,
                                   DietPlanService dietPlanService,
                                   SportPlanService sportPlanService) {
        this.userPlanService = userPlanService;
        this.userPlanCheckinService = userPlanCheckinService;
        this.sysUserService = sysUserService;
        this.dietPlanService = dietPlanService;
        this.sportPlanService = sportPlanService;
    }

    /**
     * 执行计划分页
     */
    @GetMapping("/page")
    public R<Page<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String status,
                                             HttpServletRequest request) {
        requireAdmin(request);

        Page<UserPlan> rawPage = userPlanService.page(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UserPlan>()
                        .orderByDesc(UserPlan::getCreateTime)
        );

        List<Map<String, Object>> records = rawPage.getRecords().stream()
                .map(this::buildPlanRow)
                .filter(row -> matchKeyword(row, keyword) && matchStatus(row, status))
                .collect(Collectors.toList());

        Page<Map<String, Object>> res = new Page<>(pageNum, pageSize);
        res.setCurrent(rawPage.getCurrent());
        res.setSize(rawPage.getSize());
        res.setTotal(rawPage.getTotal());
        res.setRecords(records);
        return R.ok(res);
    }

    /**
     * 某个执行计划的打卡明细
     */
    @GetMapping("/checkin-page")
    public R<Page<UserPlanCheckin>> checkinPage(@RequestParam Long userPlanId,
                                                @RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                HttpServletRequest request) {
        requireAdmin(request);

        UserPlan plan = userPlanService.getById(userPlanId);
        if (plan == null) {
            throw new BizException("执行计划不存在");
        }

        Page<UserPlanCheckin> page = userPlanCheckinService.page(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UserPlanCheckin>()
                        .eq(UserPlanCheckin::getUserPlanId, userPlanId)
                        .orderByDesc(UserPlanCheckin::getCheckinDate)
                        .orderByDesc(UserPlanCheckin::getCreateTime)
        );

        return R.ok(page);
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
        return username.contains(kw)
                || nickname.contains(kw)
                || dietPlanName.contains(kw)
                || sportPlanName.contains(kw);
    }

    private boolean matchStatus(Map<String, Object> row, String status) {
        if (!StringUtils.hasText(status)) {
            return true;
        }
        return status.trim().equals(String.valueOf(row.get("status")));
    }

    private Map<String, Object> buildPlanRow(UserPlan plan) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", plan.getId());
        row.put("userId", plan.getUserId());
        row.put("startDate", plan.getStartDate());
        row.put("endDate", plan.getEndDate());
        row.put("status", plan.getStatus());
        row.put("createTime", plan.getCreateTime());
        row.put("updateTime", plan.getUpdateTime());

        SysUser user = plan.getUserId() == null ? null : sysUserService.getById(plan.getUserId());
        DietPlan dietPlan = plan.getDietPlanId() == null ? null : dietPlanService.getById(plan.getDietPlanId());
        SportPlan sportPlan = plan.getSportPlanId() == null ? null : sportPlanService.getById(plan.getSportPlanId());

        row.put("username", user == null ? "" : user.getUsername());
        row.put("nickname", user == null ? "" : user.getNickname());
        row.put("phone", user == null ? "" : user.getPhone());

        row.put("dietPlanId", plan.getDietPlanId());
        row.put("dietPlanName", dietPlan == null ? "" : dietPlan.getName());

        row.put("sportPlanId", plan.getSportPlanId());
        row.put("sportPlanName", sportPlan == null ? "" : sportPlan.getName());

        List<UserPlanCheckin> checkins = userPlanCheckinService.list(
                new LambdaQueryWrapper<UserPlanCheckin>()
                        .eq(UserPlanCheckin::getUserPlanId, plan.getId())
                        .orderByDesc(UserPlanCheckin::getCheckinDate)
                        .orderByDesc(UserPlanCheckin::getCreateTime)
        );

        int checkinCount = checkins.size();
        LocalDate lastCheckinDate = checkins.isEmpty() ? null : checkins.get(0).getCheckinDate();

        long totalDays = 0;
        if (plan.getStartDate() != null && plan.getEndDate() != null && !plan.getEndDate().isBefore(plan.getStartDate())) {
            totalDays = ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()) + 1;
        }

        BigDecimal completionRate = BigDecimal.ZERO;
        if (totalDays > 0) {
            completionRate = BigDecimal.valueOf(checkinCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalDays), 1, RoundingMode.HALF_UP);
        }

        boolean todayChecked = checkins.stream().anyMatch(item -> LocalDate.now().equals(item.getCheckinDate()));

        row.put("checkinCount", checkinCount);
        row.put("totalDays", totalDays);
        row.put("completionRate", completionRate);
        row.put("lastCheckinDate", lastCheckinDate);
        row.put("todayChecked", todayChecked ? 1 : 0);

        return row;
    }
}
