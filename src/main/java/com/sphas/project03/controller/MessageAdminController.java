package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.SysMessage;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.service.SysMessageService;
import com.sphas.project03.service.SysUserService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端：消息提醒查看
 * 这里统一按 sys_message 查，不再和 notification 混用
 */
@RestController
@RequestMapping("/api/message/admin")
public class MessageAdminController extends BaseController {

    private final SysMessageService sysMessageService;
    private final SysUserService sysUserService;

    public MessageAdminController(SysMessageService sysMessageService,
                                  SysUserService sysUserService) {
        this.sysMessageService = sysMessageService;
        this.sysUserService = sysUserService;
    }

    /**
     * 消息分页
     * 这里把筛选条件尽量下推到数据库层，避免先分页再过滤导致总数不准
     */
    @GetMapping("/page")
    public R<Page<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) Integer isRead,
                                             HttpServletRequest request) {
        requireAdmin(request);

        LambdaQueryWrapper<SysMessage> qw = new LambdaQueryWrapper<>();

        // 类型筛选
        if (StringUtils.hasText(type)) {
            qw.eq(SysMessage::getType, type.trim());
        }

        // 已读状态筛选
        if (isRead != null) {
            qw.eq(SysMessage::getIsRead, isRead);
        }

        // 关键字筛选
        // 支持：用户名、昵称、手机号、标题、内容、业务ID
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();

            List<Long> matchedUserIds = findMatchedUserIds(kw);

            qw.and(w -> {
                boolean hasAny = false;

                if (!matchedUserIds.isEmpty()) {
                    w.in(SysMessage::getUserId, matchedUserIds);
                    hasAny = true;
                }

                if (isNumeric(kw)) {
                    if (hasAny) {
                        w.or();
                    }
                    Long num = Long.valueOf(kw);
                    w.eq(SysMessage::getBizId, num).or().eq(SysMessage::getId, num);
                    hasAny = true;
                }

                if (hasAny) {
                    w.or();
                }

                w.like(SysMessage::getTitle, kw)
                        .or()
                        .like(SysMessage::getContent, kw);
            });
        }

        qw.orderByDesc(SysMessage::getCreateTime)
                .orderByDesc(SysMessage::getId);

        Page<SysMessage> rawPage = sysMessageService.page(new Page<>(pageNum, pageSize), qw);

        // 批量查用户，避免一条条查
        Map<Long, SysUser> userMap = buildUserMap(rawPage.getRecords());

        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysMessage msg : rawPage.getRecords()) {
            rows.add(buildRow(msg, userMap.get(msg.getUserId())));
        }

        Page<Map<String, Object>> res = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        res.setRecords(rows);
        return R.ok(res);
    }

    /**
     * 根据关键字找命中的用户
     */
    private List<Long> findMatchedUserIds(String kw) {
        LambdaQueryWrapper<SysUser> userQw = new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId)
                .and(w -> w.like(SysUser::getUsername, kw)
                        .or()
                        .like(SysUser::getNickname, kw)
                        .or()
                        .like(SysUser::getPhone, kw));

        List<SysUser> users = sysUserService.list(userQw);
        List<Long> ids = new ArrayList<>();
        for (SysUser user : users) {
            if (user != null && user.getId() != null) {
                ids.add(user.getId());
            }
        }
        return ids;
    }

    /**
     * 批量构建用户映射，避免 N+1 查询
     */
    private Map<Long, SysUser> buildUserMap(List<SysMessage> msgs) {
        Map<Long, SysUser> map = new HashMap<>();
        if (msgs == null || msgs.isEmpty()) {
            return map;
        }

        List<Long> userIds = new ArrayList<>();
        for (SysMessage msg : msgs) {
            if (msg != null && msg.getUserId() != null && !userIds.contains(msg.getUserId())) {
                userIds.add(msg.getUserId());
            }
        }

        if (userIds.isEmpty()) {
            return map;
        }

        List<SysUser> users = sysUserService.listByIds(userIds);
        for (SysUser user : users) {
            if (user != null && user.getId() != null) {
                map.put(user.getId(), user);
            }
        }
        return map;
    }

    private Map<String, Object> buildRow(SysMessage msg, SysUser user) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", msg.getId());
        row.put("userId", msg.getUserId());
        row.put("type", msg.getType());
        row.put("title", msg.getTitle());
        row.put("content", msg.getContent());
        row.put("bizId", msg.getBizId());
        row.put("isRead", msg.getIsRead());
        row.put("createTime", msg.getCreateTime());
        row.put("readTime", msg.getReadTime());

        row.put("username", user == null ? "" : user.getUsername());
        row.put("nickname", user == null ? "" : user.getNickname());
        row.put("phone", user == null ? "" : user.getPhone());

        return row;
    }

    private boolean isNumeric(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}