package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.Badge;
import com.sphas.project03.entity.UserBadge;
import com.sphas.project03.service.BadgeService;
import com.sphas.project03.service.UserBadgeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 勋章查询
 */
@RestController
@RequestMapping("/api/badge")
public class BadgeController extends BaseController {

    private final BadgeService badgeService;
    private final UserBadgeService userBadgeService;

    public BadgeController(BadgeService badgeService, UserBadgeService userBadgeService) {
        this.badgeService = badgeService;
        this.userBadgeService = userBadgeService;
    }

    /**
     * 我的勋章列表（需要登录）
     */
    @GetMapping("/my")
    public R<List<Badge>> my(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        List<UserBadge> ubs = userBadgeService.list(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, userId)
                .orderByDesc(UserBadge::getCreateTime));

        List<Badge> res = new ArrayList<>();
        for (UserBadge ub : ubs) {
            Badge b = badgeService.getOne(new LambdaQueryWrapper<Badge>()
                    .eq(Badge::getCode, ub.getBadgeCode()));
            if (b != null) res.add(b);
        }
        return R.ok(res);
    }
}