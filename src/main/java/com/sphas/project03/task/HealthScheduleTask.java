package com.sphas.project03.task;

import com.sphas.project03.service.HealthRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HealthScheduleTask {
    @Autowired
    private HealthRecordService healthRecordService;
    // 每天凌晨2点执行，查询超过7天未登录/未打卡的用户，生成系统通知(Notice)
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkUnactiveUsers() {
        // ... 业务逻辑：查出7天无记录用户，插入一条消息提醒
    }
}