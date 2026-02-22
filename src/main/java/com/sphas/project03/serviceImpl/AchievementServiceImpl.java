package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.entity.ChallengeJoin;
import com.sphas.project03.entity.SysMessage;
import com.sphas.project03.entity.UserBadge;
import com.sphas.project03.entity.SocialPost;
import com.sphas.project03.service.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 成就发放实现（最小可用版）
 */
@Service
public class AchievementServiceImpl implements AchievementService {

    private final UserBadgeService userBadgeService;
    private final ChallengeJoinService challengeJoinService;
    private final SocialPostService socialPostService;
    private final SysMessageService sysMessageService;

    public AchievementServiceImpl(UserBadgeService userBadgeService,
                                  ChallengeJoinService challengeJoinService,
                                  SocialPostService socialPostService,
                                  SysMessageService sysMessageService) {
        this.userBadgeService = userBadgeService;
        this.challengeJoinService = challengeJoinService;
        this.socialPostService = socialPostService;
        this.sysMessageService = sysMessageService;
    }

    @Override
    public void onChallengeFinished(Long userId) {
        // FIRST_CHALLENGE：只要完成过任意挑战就发
        long finishedCount = challengeJoinService.count(new LambdaQueryWrapper<ChallengeJoin>()
                .eq(ChallengeJoin::getUserId, userId)
                .eq(ChallengeJoin::getFinished, 1));
        if (finishedCount >= 1) {
            grantOnce(userId, "FIRST_CHALLENGE", "获得勋章：初次挑战");
        }
    }

    @Override
    public void onPointsChanged(Long userId, int totalPoints) {
        // POINTS_50：总积分>=50
        if (totalPoints >= 50) {
            grantOnce(userId, "POINTS_50", "获得勋章：积分达人");
        }
    }

    @Override
    public void onPostCreated(Long userId) {
        // POST_3：发帖>=3
        long cnt = socialPostService.count(new LambdaQueryWrapper<SocialPost>()
                .eq(SocialPost::getUserId, userId));
        if (cnt >= 3) {
            grantOnce(userId, "POST_3", "获得勋章：社区活跃");
        }
    }

    private void grantOnce(Long userId, String badgeCode, String msg) {
        UserBadge exist = userBadgeService.getOne(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, userId)
                .eq(UserBadge::getBadgeCode, badgeCode));
        if (exist != null) return;

        UserBadge ub = new UserBadge();
        ub.setUserId(userId);
        ub.setBadgeCode(badgeCode);
        ub.setCreateTime(LocalDateTime.now());
        userBadgeService.save(ub);

        // 发站内消息提醒
        SysMessage m = new SysMessage();
        m.setUserId(userId);
        m.setType("BADGE");
        m.setTitle("【勋章达成】恭喜获得新勋章");
        m.setContent(msg);
        m.setIsRead(0);
        m.setCreateTime(LocalDateTime.now());
        sysMessageService.save(m);
    }
}