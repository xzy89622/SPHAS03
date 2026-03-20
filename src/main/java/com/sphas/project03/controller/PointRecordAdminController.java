package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.PointRecord;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.mapper.PointRecordMapper;
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
 * 管理端：积分流水查看
 */
@RestController
@RequestMapping("/api/point-record/admin")
public class PointRecordAdminController extends BaseController {

    private final PointRecordMapper pointRecordMapper;
    private final SysUserService sysUserService;

    public PointRecordAdminController(PointRecordMapper pointRecordMapper,
                                      SysUserService sysUserService) {
        this.pointRecordMapper = pointRecordMapper;
        this.sysUserService = sysUserService;
    }

    /**
     * 积分流水分页
     */
    @GetMapping("/page")
    public R<Page<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String type,
                                             HttpServletRequest request) {
        requireAdmin(request);

        Page<PointRecord> rawPage = pointRecordMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<PointRecord>()
                        .orderByDesc(PointRecord::getCreateTime)
                        .orderByDesc(PointRecord::getId)
        );

        List<Map<String, Object>> records = rawPage.getRecords().stream()
                .map(this::buildRow)
                .filter(row -> matchKeyword(row, keyword) && matchType(row, type))
                .collect(Collectors.toList());

        Page<Map<String, Object>> res = new Page<>(pageNum, pageSize);
        res.setCurrent(rawPage.getCurrent());
        res.setSize(rawPage.getSize());
        res.setTotal(rawPage.getTotal());
        res.setRecords(records);
        return R.ok(res);
    }

    private boolean matchKeyword(Map<String, Object> row, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String kw = keyword.trim();
        String username = String.valueOf(row.getOrDefault("username", ""));
        String nickname = String.valueOf(row.getOrDefault("nickname", ""));
        String remark = String.valueOf(row.getOrDefault("remark", ""));
        String type = String.valueOf(row.getOrDefault("type", ""));
        String bizId = String.valueOf(row.getOrDefault("bizId", ""));
        return username.contains(kw)
                || nickname.contains(kw)
                || remark.contains(kw)
                || type.contains(kw)
                || bizId.contains(kw);
    }

    private boolean matchType(Map<String, Object> row, String type) {
        if (!StringUtils.hasText(type)) {
            return true;
        }
        return type.trim().equals(String.valueOf(row.get("type")));
    }

    private Map<String, Object> buildRow(PointRecord record) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", record.getId());
        row.put("userId", record.getUserId());
        row.put("points", record.getPoints());
        row.put("type", record.getType());
        row.put("bizId", record.getBizId());
        row.put("remark", record.getRemark());
        row.put("createTime", record.getCreateTime());

        SysUser user = record.getUserId() == null ? null : sysUserService.getById(record.getUserId());
        row.put("username", user == null ? "" : user.getUsername());
        row.put("nickname", user == null ? "" : user.getNickname());
        row.put("phone", user == null ? "" : user.getPhone());

        return row;
    }
}