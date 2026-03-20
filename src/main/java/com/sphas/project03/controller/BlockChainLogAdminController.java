package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.mapper.BlockChainLogMapper;
import com.sphas.project03.entity.BlockChainLog;
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
 * 管理端：区块链日志查看
 */
@RestController
@RequestMapping("/api/block-chain-log/admin")
public class BlockChainLogAdminController extends BaseController {

    private final BlockChainLogMapper blockChainLogMapper;
    private final SysUserService sysUserService;

    public BlockChainLogAdminController(BlockChainLogMapper blockChainLogMapper,
                                        SysUserService sysUserService) {
        this.blockChainLogMapper = blockChainLogMapper;
        this.sysUserService = sysUserService;
    }

    /**
     * 日志分页
     */
    @GetMapping("/page")
    public R<Page<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String bizType,
                                             HttpServletRequest request) {
        requireAdmin(request);

        Page<BlockChainLog> rawPage = blockChainLogMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<BlockChainLog>()
                        .orderByDesc(BlockChainLog::getCreateTime)
                        .orderByDesc(BlockChainLog::getId)
        );

        List<Map<String, Object>> records = rawPage.getRecords().stream()
                .map(this::buildRow)
                .filter(row -> matchKeyword(row, keyword) && matchBizType(row, bizType))
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
        String bizType = String.valueOf(row.getOrDefault("bizType", ""));
        String action = String.valueOf(row.getOrDefault("action", ""));
        String bizId = String.valueOf(row.getOrDefault("bizId", ""));
        return username.contains(kw)
                || nickname.contains(kw)
                || bizType.contains(kw)
                || action.contains(kw)
                || bizId.contains(kw);
    }

    private boolean matchBizType(Map<String, Object> row, String bizType) {
        if (!StringUtils.hasText(bizType)) {
            return true;
        }
        return bizType.trim().equals(String.valueOf(row.get("bizType")));
    }

    private Map<String, Object> buildRow(BlockChainLog log) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", log.getId());
        row.put("userId", log.getUserId());
        row.put("bizType", log.getBizType());
        row.put("bizId", log.getBizId());
        row.put("action", log.getAction());
        row.put("dataHash", log.getDataHash());
        row.put("prevHash", log.getPrevHash());
        row.put("blockHash", log.getBlockHash());
        row.put("createTime", log.getCreateTime());

        SysUser user = log.getUserId() == null ? null : sysUserService.getById(log.getUserId());
        row.put("username", user == null ? "" : user.getUsername());
        row.put("nickname", user == null ? "" : user.getNickname());
        row.put("phone", user == null ? "" : user.getPhone());

        return row;
    }
}