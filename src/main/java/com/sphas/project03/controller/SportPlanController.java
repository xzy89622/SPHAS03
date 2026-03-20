package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.SportPlan;
import com.sphas.project03.service.SportPlanService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 运动方案管理
 */
@RestController
@RequestMapping("/api/sport-plan")
public class SportPlanController extends BaseController {

    private final SportPlanService sportPlanService;

    public SportPlanController(SportPlanService sportPlanService) {
        this.sportPlanService = sportPlanService;
    }

    /**
     * 用户端：启用列表
     */
    @GetMapping("/list")
    public R<List<SportPlan>> listEnabled(@RequestParam(required = false) String bmiLevel,
                                          @RequestParam(required = false) String intensity) {
        LambdaQueryWrapper<SportPlan> qw = new LambdaQueryWrapper<SportPlan>()
                .eq(SportPlan::getStatus, 1)
                .orderByDesc(SportPlan::getCreateTime);

        if (StringUtils.hasText(bmiLevel)) {
            qw.eq(SportPlan::getBmiLevel, bmiLevel.trim());
        }
        if (StringUtils.hasText(intensity)) {
            qw.eq(SportPlan::getIntensity, intensity.trim());
        }

        return R.ok(sportPlanService.list(qw));
    }

    /**
     * 管理端：分页
     */
    @GetMapping("/admin/page")
    public R<Page<SportPlan>> adminPage(@RequestParam(defaultValue = "1") long pageNum,
                                        @RequestParam(defaultValue = "10") long pageSize,
                                        @RequestParam(required = false) String name,
                                        @RequestParam(required = false) String bmiLevel,
                                        @RequestParam(required = false) String intensity,
                                        @RequestParam(required = false) Integer status,
                                        HttpServletRequest request) {

        requireAdmin(request);

        Page<SportPlan> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SportPlan> qw = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(name)) {
            qw.like(SportPlan::getName, name.trim());
        }
        if (StringUtils.hasText(bmiLevel)) {
            qw.eq(SportPlan::getBmiLevel, bmiLevel.trim());
        }
        if (StringUtils.hasText(intensity)) {
            qw.eq(SportPlan::getIntensity, intensity.trim());
        }
        if (status != null) {
            qw.eq(SportPlan::getStatus, status);
        }

        qw.orderByDesc(SportPlan::getCreateTime);

        return R.ok(sportPlanService.page(page, qw));
    }

    /**
     * 管理端：新增或更新
     */
    @PostMapping("/admin/save")
    public R<Long> save(@RequestBody SportPlan plan, HttpServletRequest request) {
        requireAdmin(request);

        if (!StringUtils.hasText(plan.getName())) {
            throw new BizException("方案名称不能为空");
        }
        if (!StringUtils.hasText(plan.getBmiLevel())) {
            throw new BizException("BMI等级不能为空");
        }
        if (!StringUtils.hasText(plan.getContent())) {
            throw new BizException("方案内容不能为空");
        }

        LocalDateTime now = LocalDateTime.now();

        if (plan.getId() == null) {
            if (plan.getStatus() == null) {
                plan.setStatus(1);
            }
            plan.setName(plan.getName().trim());
            plan.setBmiLevel(plan.getBmiLevel().trim());
            plan.setContent(plan.getContent().trim());
            if (plan.getIntensity() != null) {
                plan.setIntensity(plan.getIntensity().trim());
            }
            plan.setCreateTime(now);
            plan.setUpdateTime(now);

            sportPlanService.save(plan);
            return R.ok(plan.getId());
        }

        SportPlan db = sportPlanService.getById(plan.getId());
        if (db == null) {
            throw new BizException("运动方案不存在");
        }

        db.setName(plan.getName().trim());
        db.setBmiLevel(plan.getBmiLevel().trim());
        db.setContent(plan.getContent().trim());
        db.setIntensity(plan.getIntensity() == null ? null : plan.getIntensity().trim());
        if (plan.getStatus() != null) {
            db.setStatus(plan.getStatus());
        }
        db.setUpdateTime(now);

        sportPlanService.updateById(db);
        return R.ok(db.getId());
    }

    /**
     * 管理端：删除
     */
    @DeleteMapping("/admin/{id}")
    public R<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);

        SportPlan db = sportPlanService.getById(id);
        if (db == null) {
            throw new BizException("运动方案不存在");
        }

        return R.ok(sportPlanService.removeById(id));
    }
}