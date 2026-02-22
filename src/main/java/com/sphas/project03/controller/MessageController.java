package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.SysMessage;
import com.sphas.project03.service.SysMessageService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/message")
public class MessageController extends BaseController {

    private final SysMessageService sysMessageService;

    public MessageController(SysMessageService sysMessageService) {
        this.sysMessageService = sysMessageService;
    }

    /** 用户端：消息分页 */
    @GetMapping("/page")
    public R<Page<SysMessage>> page(@RequestParam(defaultValue = "1") long pageNum,
                                    @RequestParam(defaultValue = "10") long pageSize,
                                    @RequestParam(required = false) Integer isRead,
                                    HttpServletRequest request) {

        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        Page<SysMessage> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SysMessage> qw = new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId)
                .orderByDesc(SysMessage::getCreateTime);

        if (isRead != null) qw.eq(SysMessage::getIsRead, isRead);

        return R.ok(sysMessageService.page(page, qw));
    }

    /** 用户端：未读数 */
    @GetMapping("/unreadCount")
    public R<Long> unreadCount(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");
        long cnt = sysMessageService.count(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId)
                .eq(SysMessage::getIsRead, 0));
        return R.ok(cnt);
    }

    /** 用户端：标记已读 */
    @PostMapping("/read/{id}")
    public R<Boolean> read(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        SysMessage m = sysMessageService.getById(id);
        if (m == null || !userId.equals(m.getUserId())) throw new BizException("消息不存在");

        if (m.getIsRead() == null || m.getIsRead() == 0) {
            m.setIsRead(1);
            m.setReadTime(LocalDateTime.now());
            sysMessageService.updateById(m);
        }
        return R.ok(true);
    }

    /**
     * 用户端：消息详情
     * 说明：小程序点进来需要拿到完整内容，不走分页兜底。
     */
    @GetMapping("/detail/{id}")
    public R<SysMessage> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        SysMessage m = sysMessageService.getById(id);
        if (m == null || !userId.equals(m.getUserId())) throw new BizException("消息不存在");
        return R.ok(m);
    }

    /** 用户端：一键已读 */
    @PostMapping("/readAll")
    public R<Boolean> readAll(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        sysMessageService.lambdaUpdate()
                .eq(SysMessage::getUserId, userId)
                .eq(SysMessage::getIsRead, 0)
                .set(SysMessage::getIsRead, 1)
                .set(SysMessage::getReadTime, LocalDateTime.now())
                .update();

        return R.ok(true);
    }
}