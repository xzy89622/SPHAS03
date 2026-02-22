package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.SocialPost;
import com.sphas.project03.entity.SysMessage;
import com.sphas.project03.service.SocialPostService;
import com.sphas.project03.service.SysMessageService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 社区审核（管理员）
 * status：2待审核 1通过 3驳回 0隐藏/删除
 */
@RestController
@RequestMapping("/api/admin/social/audit")
public class SocialAuditAdminController extends BaseController {

    private final SocialPostService postService;
    private final SysMessageService sysMessageService;

    public SocialAuditAdminController(SocialPostService postService,
                                      SysMessageService sysMessageService) {
        this.postService = postService;
        this.sysMessageService = sysMessageService;
    }

    /**
     * ✅ 待审核帖子分页（status=2）
     */
    @GetMapping("/pending/page")
    public R<Page<SocialPost>> pendingPage(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           HttpServletRequest request) {
        requireAdmin(request);

        Page<SocialPost> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SocialPost> qw = new LambdaQueryWrapper<>();
        qw.eq(SocialPost::getStatus, 2);
        qw.orderByAsc(SocialPost::getCreateTime);

        return R.ok(postService.page(page, qw));
    }

    /**
     * ✅ 审核通过（status=1）
     */
    @PostMapping("/approve")
    public R<Boolean> approve(@RequestParam Long postId, HttpServletRequest request) {
        requireAdmin(request);

        SocialPost p = postService.getById(postId);
        if (p == null) throw new BizException("帖子不存在");
        if (p.getStatus() == null || p.getStatus() != 2) throw new BizException("该帖子不在待审状态");

        p.setStatus(1);
        p.setUpdateTime(LocalDateTime.now());
        postService.updateById(p);

        // ✅ 通知作者
        sendMsg(p.getUserId(), "【审核通过】你的帖子已发布", "你的帖子已通过审核并展示在社区。");

        return R.ok(true);
    }

    /**
     * ✅ 审核驳回（status=3）
     */
    @PostMapping("/reject")
    public R<Boolean> reject(@RequestParam Long postId,
                             @RequestParam(required = false, defaultValue = "内容不符合规范") String reason,
                             HttpServletRequest request) {
        requireAdmin(request);

        SocialPost p = postService.getById(postId);
        if (p == null) throw new BizException("帖子不存在");
        if (p.getStatus() == null || p.getStatus() != 2) throw new BizException("该帖子不在待审状态");

        p.setStatus(3);
        p.setUpdateTime(LocalDateTime.now());
        postService.updateById(p);

        // ✅ 通知作者
        sendMsg(p.getUserId(), "【审核驳回】你的帖子未通过审核", "原因：" + reason + "。你可以修改后重新发布。");

        return R.ok(true);
    }

    /**
     * ✅ 隐藏帖子（status=0）
     * 用于：已通过的帖子被举报/不合规，需要管理员下架
     */
    @PostMapping("/hide")
    public R<Boolean> hide(@RequestParam Long postId,
                           @RequestParam(required = false, defaultValue = "管理员下架处理") String reason,
                           HttpServletRequest request) {
        requireAdmin(request);

        SocialPost p = postService.getById(postId);
        if (p == null) throw new BizException("帖子不存在");
        if (p.getStatus() == null || p.getStatus() == 0) throw new BizException("帖子已是隐藏状态");

        p.setStatus(0);
        p.setUpdateTime(LocalDateTime.now());
        postService.updateById(p);

        // ✅ 通知作者
        sendMsg(p.getUserId(), "【帖子已下架】你的帖子被管理员隐藏", "原因：" + reason);

        return R.ok(true);
    }

    /**
     * ✅ 恢复帖子（恢复为通过 status=1）
     * 用于：误判/复审后重新上架
     */
    @PostMapping("/restore")
    public R<Boolean> restore(@RequestParam Long postId, HttpServletRequest request) {
        requireAdmin(request);

        SocialPost p = postService.getById(postId);
        if (p == null) throw new BizException("帖子不存在");
        if (p.getStatus() == null || p.getStatus() != 0) throw new BizException("该帖子不在隐藏状态");

        p.setStatus(1);
        p.setUpdateTime(LocalDateTime.now());
        postService.updateById(p);

        // ✅ 通知作者
        sendMsg(p.getUserId(), "【帖子已恢复】你的帖子已重新上架", "你的帖子已被管理员恢复展示。");

        return R.ok(true);
    }

    /**
     * ✅ 按状态分页查看帖子（审核历史）
     * status：0隐藏 1通过 2待审 3驳回
     *
     * GET /api/admin/social/audit/page?status=1&pageNum=1&pageSize=10
     */
    @GetMapping("/page")
    public R<Page<SocialPost>> page(@RequestParam Integer status,
                                    @RequestParam(defaultValue = "1") long pageNum,
                                    @RequestParam(defaultValue = "10") long pageSize,
                                    HttpServletRequest request) {
        requireAdmin(request);
        if (status == null) throw new BizException("status不能为空");

        Page<SocialPost> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SocialPost> qw = new LambdaQueryWrapper<>();
        qw.eq(SocialPost::getStatus, status);
        qw.orderByDesc(SocialPost::getCreateTime);

        return R.ok(postService.page(page, qw));
    }

    // =========================
    // 站内消息通知
    // =========================

    private void sendMsg(Long userId, String title, String content) {
        SysMessage m = new SysMessage();
        m.setUserId(userId);
        m.setType("AUDIT");
        m.setTitle(title);
        m.setContent(content);
        m.setIsRead(0);
        m.setCreateTime(LocalDateTime.now());
        sysMessageService.save(m);
    }
}