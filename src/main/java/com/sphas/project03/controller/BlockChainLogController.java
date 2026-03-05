package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.BlockChainLog;
import com.sphas.project03.service.BlockChainLogService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 区块链日志查询（管理员看审计/可追溯）
 */
@RestController
@RequestMapping("/api/admin/blockchain")
public class BlockChainLogController extends BaseController {

    private final BlockChainLogService blockChainLogService;

    public BlockChainLogController(BlockChainLogService blockChainLogService) {
        this.blockChainLogService = blockChainLogService;
    }

    /**
     * 查看某用户某业务的链记录
     * GET /api/admin/blockchain/list?userId=1&bizType=METRIC
     */
    @GetMapping("/list")
    public R<List<BlockChainLog>> list(@RequestParam Long userId,
                                      @RequestParam String bizType,
                                      HttpServletRequest request) {
        requireAdmin(request);

        List<BlockChainLog> list = blockChainLogService.list(new LambdaQueryWrapper<BlockChainLog>()
                .eq(BlockChainLog::getUserId, userId)
                .eq(BlockChainLog::getBizType, bizType)
                .orderByAsc(BlockChainLog::getId)
        );
        return R.ok(list);
    }

    /**
     * 校验链是否完整
     * GET /api/admin/blockchain/verify?userId=1&bizType=METRIC
     */
    @GetMapping("/verify")
    public R<Boolean> verify(@RequestParam Long userId,
                             @RequestParam String bizType,
                             HttpServletRequest request) {
        requireAdmin(request);
        return R.ok(blockChainLogService.verifyChain(userId, bizType));
    }
}
