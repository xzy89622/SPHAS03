package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.DietPlan;
import com.sphas.project03.service.DietPlanService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 饮食方案管理
 */
@RestController
@RequestMapping("/api/diet-plan")
public class DietPlanController extends BaseController {

    private final DietPlanService dietPlanService;

    public DietPlanController(DietPlanService dietPlanService) {
        this.dietPlanService = dietPlanService;
    }

    /**
     * 用户端：启用列表
     */
    @GetMapping("/list")
    public R<List<DietPlan>> listEnabled(@RequestParam(required = false) String bmiLevel) {
        LambdaQueryWrapper<DietPlan> qw = new LambdaQueryWrapper<DietPlan>()
                .eq(DietPlan::getStatus, 1)
                .orderByDesc(DietPlan::getCreateTime);

        if (StringUtils.hasText(bmiLevel)) {
            qw.eq(DietPlan::getBmiLevel, bmiLevel.trim());
        }

        return R.ok(dietPlanService.list(qw));
    }

    /**
     * 管理端：分页
     */
    @GetMapping("/admin/page")
    public R<Page<DietPlan>> adminPage(@RequestParam(defaultValue = "1") long pageNum,
                                       @RequestParam(defaultValue = "10") long pageSize,
                                       @RequestParam(required = false) String name,
                                       @RequestParam(required = false) String bmiLevel,
                                       @RequestParam(required = false) Integer status,
                                       HttpServletRequest request) {

        requireAdmin(request);

        Page<DietPlan> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<DietPlan> qw = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(name)) {
            qw.like(DietPlan::getName, name.trim());
        }
        if (StringUtils.hasText(bmiLevel)) {
            qw.eq(DietPlan::getBmiLevel, bmiLevel.trim());
        }
        if (status != null) {
            qw.eq(DietPlan::getStatus, status);
        }

        qw.orderByDesc(DietPlan::getCreateTime);

        return R.ok(dietPlanService.page(page, qw));
    }

    /**
     * 管理端：新增或更新
     */
    @PostMapping("/admin/save")
    public R<Long> save(@RequestBody DietPlan plan, HttpServletRequest request) {
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
            if (plan.getTags() != null) {
                plan.setTags(plan.getTags().trim());
            }
            plan.setCreateTime(now);
            plan.setUpdateTime(now);

            dietPlanService.save(plan);
            return R.ok(plan.getId());
        }

        DietPlan db = dietPlanService.getById(plan.getId());
        if (db == null) {
            throw new BizException("饮食方案不存在");
        }

        db.setName(plan.getName().trim());
        db.setBmiLevel(plan.getBmiLevel().trim());
        db.setContent(plan.getContent().trim());
        db.setTags(plan.getTags() == null ? null : plan.getTags().trim());
        if (plan.getStatus() != null) {
            db.setStatus(plan.getStatus());
        }
        db.setUpdateTime(now);

        dietPlanService.updateById(db);
        return R.ok(db.getId());
    }

    /**
     * 管理端：删除
     */
    @DeleteMapping("/admin/{id}")
    public R<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);

        DietPlan db = dietPlanService.getById(id);
        if (db == null) {
            throw new BizException("饮食方案不存在");
        }

        return R.ok(dietPlanService.removeById(id));
    }
}