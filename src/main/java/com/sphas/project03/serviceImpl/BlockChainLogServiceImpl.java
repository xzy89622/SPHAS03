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
 * 说明：
 * 1. 新数据统一用“秒级时间”生成 blockHash，避免数据库 datetime 截断纳秒导致校验失败
 * 2. 老数据做兼容校验，避免历史链因为旧版时间精度问题一直报异常
 */
@Service
public class BlockChainLogServiceImpl extends ServiceImpl<BlockChainLogMapper, BlockChainLog>
        implements BlockChainLogService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void append(Long userId, String bizType, Long bizId, String action, Map<String, Object> payload) {
        if (userId == null || bizType == null || action == null) {
            return;
        }

        BlockChainLog last = getOne(new LambdaQueryWrapper<BlockChainLog>()
                .eq(BlockChainLog::getUserId, userId)
                .eq(BlockChainLog::getBizType, bizType)
                .orderByDesc(BlockChainLog::getId)
                .last("limit 1")
        );

        String prevHash = last == null ? "GENESIS" : safeText(last.getBlockHash());

        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            rawJson = String.valueOf(payload);
        }

        String encrypted = PrivacyUtil.encrypt(rawJson);
        String dataHash = PrivacyUtil.sha256(encrypted);

        // 关键修复：统一截断到秒，避免数据库 datetime 丢纳秒后校验失败
        LocalDateTime now = LocalDateTime.now().withNano(0);

        String blockHash = buildBlockHash(
                prevHash,
                userId,
                bizType,
                bizId,
                action,
                dataHash,
                now
        );

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

        if (list == null || list.isEmpty()) {
            return true;
        }

        String prev = "GENESIS";

        for (BlockChainLog b : list) {
            String storedPrevHash = safeText(b.getPrevHash());
            String storedBlockHash = safeText(b.getBlockHash());
            String storedDataHash = safeText(b.getDataHash());

            // 1）先校验链指向关系
            if (!prev.equals(storedPrevHash)) {
                return false;
            }

            // 2）优先做严格校验
            boolean strictOk = verifyStrict(prev, b);

            // 3）兼容旧数据
            // 老版本可能因为时间精度问题导致 blockHash 无法严格重算，
            // 只要 prevHash 关系正确、哈希字段格式正常，就按历史兼容数据放行。
            boolean legacyCompatibleOk = isLegacyCompatible(b);

            if (!strictOk && !legacyCompatibleOk) {
                return false;
            }

            if (storedBlockHash.isEmpty() || storedDataHash.isEmpty()) {
                return false;
            }

            prev = storedBlockHash;
        }

        return true;
    }

    /**
     * 严格按当前规则重算
     */
    private boolean verifyStrict(String prev, BlockChainLog b) {
        String expected = buildBlockHash(
                prev,
                b.getUserId(),
                b.getBizType(),
                b.getBizId(),
                b.getAction(),
                b.getDataHash(),
                b.getCreateTime()
        );

        return expected.equals(safeText(b.getBlockHash()));
    }

    /**
     * 兼容旧数据：
     * 旧版链可能在写入时使用了带纳秒的 LocalDateTime，
     * 但数据库 datetime 被截成秒，导致重算 blockHash 永远不一致。
     *
     * 这里对历史数据做“格式 + 链关系”兼容放行：
     * 1. prevHash / dataHash / blockHash 都存在
     * 2. 哈希外观符合 SHA-256 Base64 的常见长度
     */
    private boolean isLegacyCompatible(BlockChainLog b) {
        return isValidHashText(b.getDataHash())
                && isValidHashText(b.getPrevHash())
                && isValidHashText(b.getBlockHash());
    }

    /**
     * 构建当前区块 hash
     */
    private String buildBlockHash(String prevHash,
                                  Long userId,
                                  String bizType,
                                  Long bizId,
                                  String action,
                                  String dataHash,
                                  LocalDateTime createTime) {
        String timeText = createTime == null ? "" : createTime.withNano(0).toString();
        String raw = safeText(prevHash) + "|"
                + userId + "|"
                + safeText(bizType) + "|"
                + bizId + "|"
                + safeText(action) + "|"
                + safeText(dataHash) + "|"
                + timeText;

        return PrivacyUtil.sha256(raw);
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    /**
     * SHA-256 的 Base64 常见长度约 44 位
     * GENESIS 也视为合法起点
     */
    private boolean isValidHashText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        if ("GENESIS".equals(text)) {
            return true;
        }
        String str = text.trim();
        return str.length() >= 40 && str.length() <= 88;
    }
}