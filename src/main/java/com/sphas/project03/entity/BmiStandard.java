package com.sphas.project03.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BMI 评估标准（管理员可维护）
 */
@TableName("bmi_standard")
public class BmiStandard {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 区间最小值 */
    private BigDecimal minValue;

    /** 区间最大值 */
    private BigDecimal maxValue;

    /** 等级：偏瘦/正常/超重/肥胖... */
    private String level;

    /** 建议（给用户看的） */
    private String advice;

    /** 状态：1启用 0停用 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // getter/setter ...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getMinValue() { return minValue; }
    public void setMinValue(BigDecimal minValue) { this.minValue = minValue; }
    public BigDecimal getMaxValue() { return maxValue; }
    public void setMaxValue(BigDecimal maxValue) { this.maxValue = maxValue; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getAdvice() { return advice; }
    public void setAdvice(String advice) { this.advice = advice; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
