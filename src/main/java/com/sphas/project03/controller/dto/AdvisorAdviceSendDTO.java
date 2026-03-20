package com.sphas.project03.controller.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * AI 健康顾问发送建议 DTO
 */
public class AdvisorAdviceSendDTO {

    @NotNull(message = "userId不能为空")
    private Long userId;

    /**
     * 可选：关联某条风险预警记录
     */
    private Long riskAlertId;

    @NotBlank(message = "title不能为空")
    @Size(max = 50, message = "title长度不能超过50")
    private String title;

    @NotBlank(message = "content不能为空")
    @Size(max = 1000, message = "content长度不能超过1000")
    private String content;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRiskAlertId() {
        return riskAlertId;
    }

    public void setRiskAlertId(Long riskAlertId) {
        this.riskAlertId = riskAlertId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}