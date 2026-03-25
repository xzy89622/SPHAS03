package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.BlockChainLog;
import com.sphas.project03.service.BlockChainLogService;
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
 * 用户端：查看自己的区块链存证日志
 */
@RestController
@RequestMapping("/api/block-chain-log/my")
public class UserBlockChainLogController extends BaseController {

    private final BlockChainLogService blockChainLogService;

    public UserBlockChainLogController(BlockChainLogService blockChainLogService) {
        this.blockChainLogService = blockChainLogService;
    }

    /**
     * 查看当前用户自己的链日志
     * bizType: METRIC / RISK_ALERT
     */
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(@RequestParam String bizType,
                                             HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return R.fail("未登录");
        }

        List<BlockChainLog> list = blockChainLogService.list(
                new LambdaQueryWrapper<BlockChainLog>()
                        .eq(BlockChainLog::getUserId, userId)
                        .eq(BlockChainLog::getBizType, bizType)
                        .orderByDesc(BlockChainLog::getId)
        );

        List<Map<String, Object>> res = list.stream()
                .map(this::buildRow)
                .collect(Collectors.toList());

        return R.ok(res);
    }

    /**
     * 校验当前用户自己的链是否完整
     */
    @GetMapping("/verify")
    public R<Map<String, Object>> verify(@RequestParam String bizType,
                                         HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            return R.fail("未登录");
        }

        boolean passed = blockChainLogService.verifyChain(userId, bizType);

        Map<String, Object> res = new HashMap<>();
        res.put("userId", userId);
        res.put("bizType", bizType);
        res.put("passed", passed);
        res.put("message", passed ? "链校验通过，数据未发现异常篡改" : "链校验未通过，存在异常风险");
        return R.ok(res);
    }

    private Map<String, Object> buildRow(BlockChainLog log) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", log.getId());
        row.put("userId", log.getUserId());
        row.put("bizType", log.getBizType());
        row.put("bizTypeText", bizTypeText(log.getBizType()));
        row.put("bizId", log.getBizId());
        row.put("action", log.getAction());
        row.put("actionText", actionText(log.getAction()));
        row.put("dataHash", log.getDataHash());
        row.put("prevHash", log.getPrevHash());
        row.put("blockHash", log.getBlockHash());
        row.put("createTime", log.getCreateTime());
        return row;
    }

    private String bizTypeText(String bizType) {
        if ("METRIC".equals(bizType)) {
            return "健康指标";
        }
        if ("RISK_ALERT".equals(bizType)) {
            return "风险预警";
        }
        return bizType == null ? "-" : bizType;
    }

    private String actionText(String action) {
        if ("WRITE".equals(action)) {
            return "写入";
        }
        if ("READ".equals(action)) {
            return "读取";
        }
        return action == null ? "-" : action;
    }
}