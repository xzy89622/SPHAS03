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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端：消息提醒查看
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
     */
    @GetMapping("/page")
    public R<Page<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) Integer isRead,
                                             HttpServletRequest request) {
        requireAdmin(request);

        Page<SysMessage> rawPage = sysMessageService.page(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysMessage>()
                        .orderByDesc(SysMessage::getCreateTime)
                        .orderByDesc(SysMessage::getId)
        );

        List<Map<String, Object>> records = rawPage.getRecords().stream()
                .map(this::buildRow)
                .filter(row -> matchKeyword(row, keyword) && matchType(row, type) && matchRead(row, isRead))
                .collect(Collectors.toList());

        Page<Map<String, Object>> res = new Page<>(pageNum, pageSize);
        res.setCurrent(rawPage.getCurrent());
        res.setSize(rawPage.getSize());
        res.setTotal(rawPage.getTotal());
        res.setRecords(records);
        return R.ok(res);
    }

    private Map<String, Object> buildRow(SysMessage msg) {
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

        SysUser user = msg.getUserId() == null ? null : sysUserService.getById(msg.getUserId());
        row.put("username", user == null ? "" : user.getUsername());
        row.put("nickname", user == null ? "" : user.getNickname());
        row.put("phone", user == null ? "" : user.getPhone());

        return row;
    }

    private boolean matchKeyword(Map<String, Object> row, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String kw = keyword.trim();
        String username = String.valueOf(row.getOrDefault("username", ""));
        String nickname = String.valueOf(row.getOrDefault("nickname", ""));
        String title = String.valueOf(row.getOrDefault("title", ""));
        String content = String.valueOf(row.getOrDefault("content", ""));
        String bizId = String.valueOf(row.getOrDefault("bizId", ""));
        return username.contains(kw)
                || nickname.contains(kw)
                || title.contains(kw)
                || content.contains(kw)
                || bizId.contains(kw);
    }

    private boolean matchType(Map<String, Object> row, String type) {
        if (!StringUtils.hasText(type)) {
            return true;
        }
        return type.trim().equals(String.valueOf(row.get("type")));
    }

    private boolean matchRead(Map<String, Object> row, Integer isRead) {
        if (isRead == null) {
            return true;
        }
        Object v = row.get("isRead");
        if (v == null) {
            return false;
        }
        return String.valueOf(isRead).equals(String.valueOf(v));
    }
}