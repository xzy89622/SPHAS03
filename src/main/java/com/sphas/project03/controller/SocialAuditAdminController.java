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
     * 待审核帖子分页（status=2）
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
     * 审核通过（status=1）
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

        // 通知作者
        sendMsg(p.getUserId(), "【审核通过】你的帖子已发布", "你的帖子已通过审核并展示在社区。");

        return R.ok(true);
    }

    /**
     * 审核驳回（status=3）
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

        sendMsg(p.getUserId(), "【审核驳回】你的帖子未通过审核", "原因：" + reason + "。你可以修改后重新发布。");

        return R.ok(true);
    }

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