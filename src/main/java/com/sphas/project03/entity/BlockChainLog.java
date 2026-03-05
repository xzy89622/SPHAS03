package com.sphas.project03.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 模拟“区块链”日志表：用 hash 链把敏感数据的写入/访问串起来
 * 说明：这里不是上链到真正的联盟链，而是用“不可篡改链式校验”的方式完成毕设要求里的可追溯。
 */
@TableName("block_chain_log")
public class BlockChainLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;            // 关联用户（谁的数据）
    private String bizType;         // 业务类型：METRIC/RISK_ALERT/PROFILE 等
    private Long bizId;             // 业务主键：recordId/alertId...
    private String action;          // WRITE/READ

    private String dataHash;        // 数据指纹（对敏感内容先加密/脱敏后再做hash）
    private String prevHash;        // 上一区块hash
    private String blockHash;       // 当前区块hash

    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }

    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDataHash() { return dataHash; }
    public void setDataHash(String dataHash) { this.dataHash = dataHash; }

    public String getPrevHash() { return prevHash; }
    public void setPrevHash(String prevHash) { this.prevHash = prevHash; }

    public String getBlockHash() { return blockHash; }
    public void setBlockHash(String blockHash) { this.blockHash = blockHash; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
