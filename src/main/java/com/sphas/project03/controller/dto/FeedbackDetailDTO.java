package com.sphas.project03.controller.dto;

import com.sphas.project03.entity.Feedback;
import com.sphas.project03.entity.FileAttachment;
import com.sphas.project03.entity.FeedbackReply;

import java.util.List;

/**
 * 反馈详情：反馈主信息 + 附件 + 回复
 */
public class FeedbackDetailDTO {

    private Feedback feedback;
    private List<FileAttachment> attachments;
    private List<FeedbackReply> replies;

    public Feedback getFeedback() { return feedback; }
    public void setFeedback(Feedback feedback) { this.feedback = feedback; }

    public List<FileAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<FileAttachment> attachments) { this.attachments = attachments; }

    public List<FeedbackReply> getReplies() { return replies; }
    public void setReplies(List<FeedbackReply> replies) { this.replies = replies; }
}
