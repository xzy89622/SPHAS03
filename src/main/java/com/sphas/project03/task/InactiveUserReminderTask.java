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
 * 长时间未登录提醒任务
 * 作用：每天定时扫描 7 天未登录的用户，自动发一条站内提醒
 */
@Component
public class InactiveUserReminderTask {

    private final SysUserService sysUserService;
    private final SysMessageService sysMessageService;

    public InactiveUserReminderTask(SysUserService sysUserService,
                                    SysMessageService sysMessageService) {
        this.sysUserService = sysUserService;
        this.sysMessageService = sysMessageService;
    }

    /**
     * 每天早上 9 点执行一次
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void runDailyCheck() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime threeDaysAgo = now.minusDays(3);

        // 查出 7 天没登录、账号正常的普通用户
        List<SysUser> users = sysUserService.list(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getStatus, 1)
                        .eq(SysUser::getRole, "USER")
        );

        for (SysUser user : users) {
            boolean inactive = false;

            // 有最后登录时间时，按最后登录时间判断
            if (user.getLastLoginTime() != null) {
                inactive = !user.getLastLoginTime().isAfter(sevenDaysAgo);
            } else if (user.getCreateTime() != null) {
                // 从未登录过时，账号创建超过 7 天也提醒一次
                inactive = !user.getCreateTime().isAfter(sevenDaysAgo);
            }

            if (!inactive) {
                continue;
            }

            // 3 天内同类提醒只发一次，避免刷屏
            Long count = sysMessageService.lambdaQuery()
                    .eq(SysMessage::getUserId, user.getId())
                    .eq(SysMessage::getType, "INACTIVE")
                    .ge(SysMessage::getCreateTime, threeDaysAgo)
                    .count();

            if (count != null && count > 0) {
                continue;
            }

            SysMessage msg = new SysMessage();
            msg.setUserId(user.getId());
            msg.setType("INACTIVE");
            msg.setTitle("【登录提醒】你已超过7天未登录");
            msg.setContent("系统检测到你已较长时间未登录，建议尽快回来查看健康记录和最新推荐，避免历史数据失效。");
            msg.setBizId(null);
            msg.setIsRead(0);
            msg.setCreateTime(now);
            msg.setReadTime(null);

            sysMessageService.save(msg);
        }
    }
}