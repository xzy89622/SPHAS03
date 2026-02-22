package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.controller.vo.SocialCommentVO;
import com.sphas.project03.controller.vo.SocialPostVO;
import com.sphas.project03.mapper.SocialCommentMapper;
import com.sphas.project03.mapper.SocialPostMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 社交查询V2（只做联表查询，不影响你原来的 SocialControllerV2）
 */
@RestController
@RequestMapping("/api/social/query/v2")
public class SocialQueryControllerV2 extends BaseController {

    private final SocialPostMapper socialPostMapper;
    private final SocialCommentMapper socialCommentMapper;

    public SocialQueryControllerV2(SocialPostMapper socialPostMapper, SocialCommentMapper socialCommentMapper) {
        this.socialPostMapper = socialPostMapper;
        this.socialCommentMapper = socialCommentMapper;
    }

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

    @GetMapping("/comment/page")
    public R<IPage<SocialCommentVO>> commentPage(@RequestParam Long postId,
                                                 @RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize) {
        if (postId == null) throw new BizException("postId不能为空");
        Page<SocialCommentVO> page = new Page<>(pageNum, pageSize);
        return R.ok(socialCommentMapper.selectCommentPageV2(page, postId));
    }
}