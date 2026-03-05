package com.sphas.project03.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sphas.project03.entity.BlockChainLog;

import java.util.Map;

/**
 * 模拟区块链服务：负责把敏感数据的写入/读取记录成 hash 链
 */
public interface BlockChainLogService extends IService<BlockChainLog> {

    /**
     * 追加一条链式日志（写入/访问）
     * @param userId 用户ID
     * @param bizType 业务类型
     * @param bizId 业务主键
     * @param action WRITE/READ
     * @param payload 用来生成 dataHash 的业务数据（建议已经做了加密/脱敏）
     */
    void append(Long userId, String bizType, Long bizId, String action, Map<String, Object> payload);

    /**
     * 校验链完整性（从头到尾重新计算，发现不一致就返回 false）
     */
    boolean verifyChain(Long userId, String bizType);
}
