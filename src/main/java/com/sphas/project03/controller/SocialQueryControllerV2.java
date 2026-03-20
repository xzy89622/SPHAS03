package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.controller.vo.SocialCommentVO;
import com.sphas.project03.controller.vo.SocialPostVO;
import com.sphas.project03.entity.SocialPost;
import com.sphas.project03.mapper.SocialCommentMapper;
import com.sphas.project03.mapper.SocialPostMapper;
import com.sphas.project03.service.SocialPostService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 社交查询V2
 */
@RestController
@RequestMapping("/api/social/query/v2")
public class SocialQueryControllerV2 extends BaseController {

    private final SocialPostMapper socialPostMapper;
    private final SocialCommentMapper socialCommentMapper;
    private final SocialPostService postService;

    public SocialQueryControllerV2(SocialPostMapper socialPostMapper,
                                   SocialCommentMapper socialCommentMapper,
                                   SocialPostService postService) {
        this.socialPostMapper = socialPostMapper;
        this.socialCommentMapper = socialCommentMapper;
        this.postService = postService;
    }

    /**
     * 帖子分页（社区流）
     */
    @GetMapping("/post/page")
    public R<IPage<SocialPostVO>> postPage(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) String keyword,
                                           HttpServletRequest request) {

        Long userId = null;
        try {
            userId = getUserId(request);
        } catch (Exception ignore) {}

        Page<SocialPostVO> page = new Page<>(pageNum, pageSize);
        return R.ok(socialPostMapper.selectPostPageV2(page, userId, keyword));
    }

    /**
     * 评论分页
     */
    @GetMapping("/comment/page")
    public R<IPage<SocialCommentVO>> commentPage(@RequestParam Long postId,
                                                 @RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize) {
        if (postId == null) throw new BizException("postId不能为空");
        Page<SocialCommentVO> page = new Page<>(pageNum, pageSize);
        return R.ok(socialCommentMapper.selectCommentPageV2(page, postId));
    }

    /**
     * 我的帖子分页
     * 默认排除“用户已删除”的帖子
     */
    @GetMapping("/post/my/page")
    public R<Page<SocialPost>> myPostPage(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) Integer status,
                                          HttpServletRequest request) {

        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        Page<SocialPost> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SocialPost> qw = new LambdaQueryWrapper<>();
        qw.eq(SocialPost::getUserId, userId)
                .eq(SocialPost::getDeletedFlag, 0);

        if (status != null) {
            qw.eq(SocialPost::getStatus, status);
        }

        qw.orderByDesc(SocialPost::getCreateTime);

        return R.ok(postService.page(page, qw));
    }

    /**
     * 我的动态统计
     * 首页/我的日志页统计专用
     */
    @GetMapping("/post/my/stats")
    public R<Map<String, Long>> myPostStats(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        long pendingCount = postService.count(new LambdaQueryWrapper<SocialPost>()
                .eq(SocialPost::getUserId, userId)
                .eq(SocialPost::getDeletedFlag, 0)
                .eq(SocialPost::getStatus, 2));

        long publishedCount = postService.count(new LambdaQueryWrapper<SocialPost>()
                .eq(SocialPost::getUserId, userId)
                .eq(SocialPost::getDeletedFlag, 0)
                .eq(SocialPost::getStatus, 1));

        long rejectedCount = postService.count(new LambdaQueryWrapper<SocialPost>()
                .eq(SocialPost::getUserId, userId)
                .eq(SocialPost::getDeletedFlag, 0)
                .eq(SocialPost::getStatus, 3));

        long hiddenCount = postService.count(new LambdaQueryWrapper<SocialPost>()
                .eq(SocialPost::getUserId, userId)
                .eq(SocialPost::getDeletedFlag, 0)
                .eq(SocialPost::getStatus, 0));

        long totalCount = pendingCount + publishedCount + rejectedCount + hiddenCount;

        Map<String, Long> res = new HashMap<>();
        res.put("pendingCount", pendingCount);
        res.put("publishedCount", publishedCount);
        res.put("rejectedCount", rejectedCount);
        res.put("hiddenCount", hiddenCount);
        res.put("totalCount", totalCount);

        return R.ok(res);
    }
}