package com.sphas.project03.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.entity.SysMessage;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.service.SysMessageService;
import com.sphas.project03.service.SysUserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务：不活跃用户提醒（7天未登录）
 */
@Component
public class HealthScheduleTask {

    private final SysUserService sysUserService;
    private final SysMessageService sysMessageService;

    public HealthScheduleTask(SysUserService sysUserService, SysMessageService sysMessageService) {
        this.sysUserService = sysUserService;
        this.sysMessageService = sysMessageService;
    }

    // 每天凌晨2点执行
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkUnactiveUsers() {

        LocalDateTime threshold = LocalDateTime.now().minusDays(7);

        // 1) 找出7天未登录的普通用户
        List<SysUser> users = sysUserService.list(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "USER")
                .and(qw -> qw.isNull(SysUser::getLastLoginTime).or().lt(SysUser::getLastLoginTime, threshold))
        );

        // 2) 为每个人发一条提醒（同类型当天只发一次，避免刷屏）
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();

        for (SysUser u : users) {
            long sent = sysMessageService.count(new LambdaQueryWrapper<SysMessage>()
                    .eq(SysMessage::getUserId, u.getId())
                    .eq(SysMessage::getType, "INACTIVE")
                    .ge(SysMessage::getCreateTime, todayStart)
            );
            if (sent > 0) continue;

            SysMessage m = new SysMessage();
            m.setUserId(u.getId());
            m.setType("INACTIVE");
            m.setTitle("好久不见～来记录一下健康数据吧");
            m.setContent("你已超过7天未登录/未进行健康记录。建议打开小程序完成一次体质记录，系统将为你更新健康风险与推荐计划。");
            m.setIsRead(0);
            m.setCreateTime(LocalDateTime.now());
            sysMessageService.save(m);
        }
    }
}
