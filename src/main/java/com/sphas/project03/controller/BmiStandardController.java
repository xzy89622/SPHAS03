package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.BmiStandard;
import com.sphas.project03.service.BmiStandardService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * BMI 标准（用户端查询 + 管理端维护）
 */
@RestController
@RequestMapping("/api/standards/bmi")
public class BmiStandardController extends BaseController {

    private final BmiStandardService service;

    public BmiStandardController(BmiStandardService service) {
        this.service = service;
    }

    /**
     * 用户端：启用列表
     */
    @GetMapping("/list")
    public R<List<BmiStandard>> listEnabled() {
        List<BmiStandard> list = service.list(
                new LambdaQueryWrapper<BmiStandard>()
                        .eq(BmiStandard::getStatus, 1)
                        .orderByAsc(BmiStandard::getMinValue)
        );
        return R.ok(list);
    }

    /**
     * 管理端：分页
     */
    @GetMapping("/admin/page")
    public R<Page<BmiStandard>> adminPage(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          HttpServletRequest request) {
        requireAdmin(request);
        Page<BmiStandard> page = new Page<>(pageNum, pageSize);
        return R.ok(service.page(page, new LambdaQueryWrapper<BmiStandard>().orderByAsc(BmiStandard::getMinValue)));
    }

    /**
     * 管理端：新增/更新
     */
    @PostMapping("/admin/save")
    public R<Long> save(@RequestBody BmiStandard standard, HttpServletRequest request) {
        requireAdmin(request);

        // 参数校验
        if (standard.getMinValue() == null || standard.getMaxValue() == null) {
            throw new BizException("minValue/maxValue不能为空");
        }
        if (!StringUtils.hasText(standard.getLevel())) {
            throw new BizException("level不能为空");
        }
        if (standard.getMinValue().compareTo(standard.getMaxValue()) >= 0) {
            throw new BizException("minValue必须小于maxValue");
        }

        LocalDateTime now = LocalDateTime.now();

        if (standard.getId() == null) {
            // 新增
            if (standard.getStatus() == null) standard.setStatus(1);
            standard.setCreateTime(now);
            standard.setUpdateTime(now);
            service.save(standard);
        } else {
            // 更新
            BmiStandard db = service.getById(standard.getId());
            if (db == null) throw new BizException("记录不存在");

            db.setMinValue(standard.getMinValue());
            db.setMaxValue(standard.getMaxValue());
            db.setLevel(standard.getLevel());
            db.setAdvice(standard.getAdvice());
            if (standard.getStatus() != null) db.setStatus(standard.getStatus());
            db.setUpdateTime(now);

            service.updateById(db);
        }

        return R.ok(standard.getId());
    }

    /**
     * 管理端：删除（或你也可以只做停用）
     */
    @DeleteMapping("/admin/{id}")
    public R<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        return R.ok(service.removeById(id));
    }
}

