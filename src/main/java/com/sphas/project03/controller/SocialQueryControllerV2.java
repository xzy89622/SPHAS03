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

/**
 * 社交查询V2（联表查询：日志-用户-评论）
 * ✅ 给小程序/前端用：一次拿到帖子作者信息、统计信息、likedByMe
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
     * ✅ 帖子分页（联表：social_post + sys_user + like/comment统计 + likedByMe）
     * 只返回审核通过 status=1（mapper里已限制）
     */
    @GetMapping("/post/page")
    public R<IPage<SocialPostVO>> postPage(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) String keyword,
                                           HttpServletRequest request) {

        // 未登录也能浏览：likedByMe 默认 0
        Long userId = null;
        try {
            userId = getUserId(request);
        } catch (Exception ignore) {}

        Page<SocialPostVO> page = new Page<>(pageNum, pageSize);
        return R.ok(socialPostMapper.selectPostPageV2(page, userId, keyword));
    }

    /**
     * ✅ 评论分页（联表：social_comment + sys_user）
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
     * ✅ 我的帖子分页（用于展示审核流：待审/驳回/通过/隐藏都能看到）
     * status 不传=全部；传 1/2/3/0 按状态过滤
     *
     * GET /api/social/query/v2/post/my/page?pageNum=1&pageSize=10&status=2
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
        qw.eq(SocialPost::getUserId, userId);
        if (status != null) {
            qw.eq(SocialPost::getStatus, status);
        }
        qw.orderByDesc(SocialPost::getCreateTime);

        return R.ok(postService.page(page, qw));
    }
}