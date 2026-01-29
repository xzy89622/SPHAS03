package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.Notice;
import com.sphas.project03.service.NoticeService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 公告模块
 */
@RestController
@RequestMapping("/api/notice")
public class NoticeController extends BaseController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    // ================= 用户端：列表/详情 =================

    /**
     * 公告列表（只看已发布）
     */
    @GetMapping("/list")
    public R<Page<Notice>> list(@RequestParam(defaultValue = "1") long pageNum,
                                @RequestParam(defaultValue = "10") long pageSize) {

        Page<Notice> page = new Page<>(pageNum, pageSize);

        Page<Notice> res = noticeService.page(
                page,
                new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getStatus, 1)
                        .orderByDesc(Notice::getCreateTime)
        );
        return R.ok(res);
    }

    /**
     * 公告详情（只看已发布）
     */
    @GetMapping("/{id}")
    public R<Notice> detail(@PathVariable Long id) {

        Notice n = noticeService.getById(id);
        if (n == null || n.getStatus() == null || n.getStatus() != 1) {
            throw new BizException("公告不存在或已下线");
        }
        return R.ok(n);
    }

    // ================= 管理端：新增/编辑/下线/管理列表 =================

    /**
     * 管理端：公告分页（看全部，可按标题模糊）
     */
    @GetMapping("/admin/page")
    public R<Page<Notice>> adminPage(@RequestParam(defaultValue = "1") long pageNum,
                                     @RequestParam(defaultValue = "10") long pageSize,
                                     @RequestParam(required = false) String title,
                                     HttpServletRequest request) {

        requireAdmin(request);

        Page<Notice> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Notice> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(title)) {
            qw.like(Notice::getTitle, title);
        }
        qw.orderByDesc(Notice::getCreateTime);

        return R.ok(noticeService.page(page, qw));
    }

    /**
     * 管理端：新增或更新（id有值=更新；id为空=新增）
     * status：1发布/0下线
     */
    @PostMapping("/admin/save")
    public R<Long> save(@RequestBody Notice notice, HttpServletRequest request) {

        requireAdmin(request);

        LocalDateTime now = LocalDateTime.now();

        if (notice.getId() == null) {
            // 新增
            if (!StringUtils.hasText(notice.getTitle())) throw new BizException("title不能为空");
            if (!StringUtils.hasText(notice.getContent())) throw new BizException("content不能为空");

            if (notice.getStatus() == null) notice.setStatus(1); // 默认发布
            notice.setCreateTime(now);
            notice.setUpdateTime(now);

            noticeService.save(notice);
        } else {
            // 更新
            Notice db = noticeService.getById(notice.getId());
            if (db == null) throw new BizException("公告不存在");

            if (StringUtils.hasText(notice.getTitle())) db.setTitle(notice.getTitle());
            if (StringUtils.hasText(notice.getContent())) db.setContent(notice.getContent());
            if (notice.getStatus() != null) db.setStatus(notice.getStatus());

            db.setUpdateTime(now);
            noticeService.updateById(db);
        }

        return R.ok(notice.getId());
    }

    /**
     * 管理端：下线公告
     */
    @PostMapping("/admin/offline/{id}")
    public R<Boolean> offline(@PathVariable Long id, HttpServletRequest request) {

        requireAdmin(request);

        Notice db = noticeService.getById(id);
        if (db == null) throw new BizException("公告不存在");

        db.setStatus(0);
        db.setUpdateTime(LocalDateTime.now());
        return R.ok(noticeService.updateById(db));
    }
}

