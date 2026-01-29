package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.QuestionBank;
import com.sphas.project03.service.QuestionBankService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 题库：用户端拉题 + 管理端维护
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionBankController extends BaseController {

    private final QuestionBankService service;

    public QuestionBankController(QuestionBankService service) {
        this.service = service;
    }

    /**
     * 用户端：获取启用题目（可按维度筛选）
     * 例：GET /api/questions/list?dimension=SPORT
     */
    @GetMapping("/list")
    public R<List<QuestionBank>> list(@RequestParam(required = false) String dimension) {

        LambdaQueryWrapper<QuestionBank> qw = new LambdaQueryWrapper<QuestionBank>()
                .eq(QuestionBank::getStatus, 1)
                .orderByAsc(QuestionBank::getId);

        if (StringUtils.hasText(dimension)) {
            qw.eq(QuestionBank::getDimension, dimension);
        }

        return R.ok(service.list(qw));
    }

    // ================ 管理端 ================

    /**
     * 管理端：分页
     */
    @GetMapping("/admin/page")
    public R<Page<QuestionBank>> adminPage(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) String dimension,
                                           HttpServletRequest request) {
        requireAdmin(request);

        Page<QuestionBank> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<QuestionBank> qw = new LambdaQueryWrapper<QuestionBank>()
                .orderByDesc(QuestionBank::getCreateTime);

        if (StringUtils.hasText(dimension)) {
            qw.eq(QuestionBank::getDimension, dimension);
        }

        return R.ok(service.page(page, qw));
    }

    /**
     * 管理端：新增/更新
     */
    @PostMapping("/admin/save")
    public R<Long> save(@RequestBody QuestionBank q, HttpServletRequest request) {
        requireAdmin(request);

        if (!StringUtils.hasText(q.getDimension())) throw new BizException("dimension不能为空");
        if (!StringUtils.hasText(q.getQuestion())) throw new BizException("question不能为空");
        if (!StringUtils.hasText(q.getOptionsJson())) throw new BizException("optionsJson不能为空");

        LocalDateTime now = LocalDateTime.now();

        if (q.getId() == null) {
            if (q.getStatus() == null) q.setStatus(1);
            q.setCreateTime(now);
            q.setUpdateTime(now);
            service.save(q);
        } else {
            QuestionBank db = service.getById(q.getId());
            if (db == null) throw new BizException("题目不存在");

            db.setDimension(q.getDimension());
            db.setQuestion(q.getQuestion());
            db.setOptionsJson(q.getOptionsJson());
            if (q.getStatus() != null) db.setStatus(q.getStatus());
            db.setUpdateTime(now);

            service.updateById(db);
        }

        return R.ok(q.getId());
    }

    /**
     * 管理端：删除（也可以改成停用）
     */
    @DeleteMapping("/admin/{id}")
    public R<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        return R.ok(service.removeById(id));
    }
}

