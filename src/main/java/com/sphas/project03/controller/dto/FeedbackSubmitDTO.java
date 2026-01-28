package com.sphas.project03.controller.dto;

import java.util.List;

/**
 * 提交反馈：包含反馈内容 + 附件URL列表
 */
public class FeedbackSubmitDTO {
    private String title;
    private String content;

    /** 上传接口返回的图片URL列表 */
    private List<String> attachmentUrls;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<String> getAttachmentUrls() { return attachmentUrls; }
    public void setAttachmentUrls(List<String> attachmentUrls) { this.attachmentUrls = attachmentUrls; }
}
