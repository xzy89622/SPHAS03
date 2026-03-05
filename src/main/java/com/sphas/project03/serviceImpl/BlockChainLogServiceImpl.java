package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sphas.project03.entity.BlockChainLog;
import com.sphas.project03.mapper.BlockChainLogMapper;
import com.sphas.project03.service.BlockChainLogService;
import com.sphas.project03.utils.PrivacyUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 区块链日志实现：用 SHA-256 做 hash 链
 */
@Service
public class BlockChainLogServiceImpl extends ServiceImpl<BlockChainLogMapper, BlockChainLog>
        implements BlockChainLogService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void append(Long userId, String bizType, Long bizId, String action, Map<String, Object> payload) {

        if (userId == null || bizType == null || action == null) return;

        // 1) 拿到当前链的最后一个区块hash
        BlockChainLog last = getOne(new LambdaQueryWrapper<BlockChainLog>()
                .eq(BlockChainLog::getUserId, userId)
                .eq(BlockChainLog::getBizType, bizType)
                .orderByDesc(BlockChainLog::getId)
                .last("limit 1")
        );
        String prevHash = last == null ? "GENESIS" : last.getBlockHash();

        // 2) 生成 dataHash（payload 转 JSON -> AES 加密 -> 再做 SHA-256）
        //    这样做可以同时满足“敏感数据加密 + 不可篡改校验”
        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            rawJson = String.valueOf(payload);
        }
        String encrypted = PrivacyUtil.encrypt(rawJson);
        String dataHash = PrivacyUtil.sha256(encrypted);

        // 3) 生成区块hash（把 prevHash + 关键字段串起来再 hash）
        LocalDateTime now = LocalDateTime.now();
        String blockHash = PrivacyUtil.sha256(prevHash + "|" + userId + "|" + bizType + "|" + bizId + "|" + action + "|" + dataHash + "|" + now);

        // 4) 入库
        BlockChainLog log = new BlockChainLog();
        log.setUserId(userId);
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setAction(action);
        log.setPrevHash(prevHash);
        log.setDataHash(dataHash);
        log.setBlockHash(blockHash);
        log.setCreateTime(now);

        save(log);
    }

    @Override
    public boolean verifyChain(Long userId, String bizType) {

        List<BlockChainLog> list = list(new LambdaQueryWrapper<BlockChainLog>()
                .eq(BlockChainLog::getUserId, userId)
                .eq(BlockChainLog::getBizType, bizType)
                .orderByAsc(BlockChainLog::getId)
        );

        String prev = "GENESIS";
        for (BlockChainLog b : list) {
            // 按同一规则重新算一次区块hash，只要对不上就说明链被改过
            String expect = PrivacyUtil.sha256(prev + "|" + b.getUserId() + "|" + b.getBizType() + "|" + b.getBizId()
                    + "|" + b.getAction() + "|" + b.getDataHash() + "|" + b.getCreateTime());
            if (!expect.equals(b.getBlockHash())) {
                return false;
            }
            prev = b.getBlockHash();
        }
        return true;
    }
}
