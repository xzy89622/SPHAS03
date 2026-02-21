package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.FeedbackDetailDTO;
import com.sphas.project03.controller.dto.FeedbackReplyAddDTO;
import com.sphas.project03.controller.dto.FeedbackSubmitDTO;
import com.sphas.project03.entity.Feedback;
import com.sphas.project03.entity.FeedbackReply;
import com.sphas.project03.entity.FileAttachment;
import com.sphas.project03.service.FeedbackReplyService;
import com.sphas.project03.service.FeedbackService;
import com.sphas.project03.service.FileAttachmentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 反馈模块
 */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController extends BaseController {

    private final FeedbackService feedbackService;
    private final FileAttachmentService fileAttachmentService;
    private final FeedbackReplyService feedbackReplyService;

    /**
     * 访问前缀（用于拼接上传后的 URL）
     * 例如：http://localhost:8080
     */
    @Value("${app.baseUrl:http://localhost:8080}")
    private String baseUrl;

    public FeedbackController(FeedbackService feedbackService,
                              FileAttachmentService fileAttachmentService,
                              FeedbackReplyService feedbackReplyService) {
        this.feedbackService = feedbackService;
        this.fileAttachmentService = fileAttachmentService;
        this.feedbackReplyService = feedbackReplyService;
    }

    /**
     * 用户：提交反馈
     */
    @PostMapping("/submit")
    public R<Boolean> submit(@RequestBody FeedbackSubmitDTO dto, HttpServletRequest request) {

        Long userId = requireUserId(request);

        // 1) 保存 feedback 主表
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setTitle(dto.getTitle());
        feedback.setContent(dto.getContent());
        feedback.setStatus("OPEN");
        feedback.setCreateTime(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());

        boolean ok = feedbackService.save(feedback);
        if (!ok) {
            return R.fail("保存反馈失败");
        }

        // 2) 保存附件表（bizId = feedback.id）
        if (dto.getAttachmentUrls() != null && !dto.getAttachmentUrls().isEmpty()) {
            for (String url : dto.getAttachmentUrls()) {
                String fixedUrl = normalizeFeedbackUploadUrl(url);
                if (fixedUrl == null) {
                    // 空的就忽略
                    continue;
                }

                FileAttachment attachment = new FileAttachment();
                attachment.setBizType("FEEDBACK");
                attachment.setBizId(feedback.getId());
                attachment.setFileName("feedback-upload");
                attachment.setFileUrl(fixedUrl);
                attachment.setFileSize(0L);
                attachment.setMimeType("image/*");
                attachment.setUploadUserId(userId);
                attachment.setCreateTime(LocalDateTime.now());

                fileAttachmentService.save(attachment);
            }
        }

        return R.ok(true);
    }

    /**
     * 用户：我的反馈列表
     */
    @GetMapping("/my")
    public R<List<Feedback>> myList(HttpServletRequest request) {
        Long userId = requireUserId(request);

        List<Feedback> list = feedbackService.list(
                new QueryWrapper<Feedback>()
                        .eq("user_id", userId)
                        .orderByDesc("create_time")
        );
        return R.ok(list);
    }

    /**
     * 用户：反馈详情（主表 + 附件 + 回复）
     */
    @GetMapping("/{id}/detail")
    public R<FeedbackDetailDTO> detail(@PathVariable Long id, HttpServletRequest request) {

        Long userId = requireUserId(request);

        // 1) 查反馈
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return R.fail("反馈不存在");
        }

        // 2) 只能看自己的
        if (!userId.equals(feedback.getUserId())) {
            return R.fail("无权查看该反馈");
        }

        // 3) 查附件
        List<FileAttachment> attachments = fileAttachmentService.list(
                new QueryWrapper<FileAttachment>()
                        .eq("biz_type", "FEEDBACK")
                        .eq("biz_id", id)
                        .orderByAsc("id")
        );

        // 4) 查回复
        List<FeedbackReply> replies = feedbackReplyService.list(
                new QueryWrapper<FeedbackReply>()
                        .eq("feedback_id", id)
                        .orderByAsc("create_time")
        );

        // 5) 组装返回
        FeedbackDetailDTO dto = new FeedbackDetailDTO();
        dto.setFeedback(feedback);
        dto.setAttachments(attachments);
        dto.setReplies(replies);

        return R.ok(dto);
    }

    /**
     * 兼容旧接口：前端如果还在调 /detail/attachments，就直接走新版 detail
     */
    @GetMapping("/{id}/detail/attachments")
    public R<FeedbackDetailDTO> detailWithAttachments(@PathVariable Long id, HttpServletRequest request) {
        return detail(id, request);
    }

    /**
     * 用户：上传反馈图片（只负责上传，返回可访问 URL）
     */
    @PostMapping("/upload")
    public R<String> upload(@RequestParam("file") MultipartFile file,
                            HttpServletRequest request) throws IOException {

        // 这里会触发 JwtInterceptor，所以正常情况 userId 一定存在
        requireUserId(request);

        if (file == null || file.isEmpty()) {
            return R.fail("文件为空");
        }

        // 1) 生成新文件名（避免重名覆盖）
        String originalName = file.getOriginalFilename();
        String suffix = "";
        if (originalName != null && originalName.contains(".")) {
            suffix = originalName.substring(originalName.lastIndexOf("."));
        }
        String newName = UUID.randomUUID().toString().replace("-", "") + suffix;

        // 2) 保存到本地：<项目根>/upload/feedback
        String baseDir = System.getProperty("user.dir");
        File dir = new File(baseDir, "upload/feedback");
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                return R.fail("创建上传目录失败：" + dir.getAbsolutePath());
            }
        }

        File dest = new File(dir, newName);
        file.transferTo(dest);

        // 3) 返回访问 URL（注意：这里不要写死 localhost）
        String fileUrl = baseUrl + "/upload/feedback/" + newName;
        return R.ok(fileUrl);
    }

    /**
     * 管理员：回复反馈
     */
    @PostMapping("/{id}/reply")
    public R<Boolean> adminReply(@PathVariable Long id,
                                 @RequestBody FeedbackReplyAddDTO dto,
                                 HttpServletRequest request) {

        // 仅管理员可操作
        requireAdmin(request);

        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return R.fail("反馈不存在");
        }

        Long adminId = requireUserId(request);

        FeedbackReply reply = new FeedbackReply();
        reply.setFeedbackId(id);
        reply.setSenderRole("ADMIN");
        reply.setSenderId(adminId);
        reply.setContent(dto.getContent());
        reply.setCreateTime(LocalDateTime.now());

        boolean ok = feedbackReplyService.save(reply);
        return R.ok(ok);
    }

    /**
     * 管理端：反馈列表（仅管理员）
     */
    @GetMapping("/admin/list")
    public R<List<Feedback>> adminList(HttpServletRequest request) {
        requireAdmin(request);

        List<Feedback> list = feedbackService.list(
                new QueryWrapper<Feedback>()
                        .orderByDesc("create_time")
        );
        return R.ok(list);
    }

    /**
     * 管理端：反馈详情（仅管理员，含附件+回复）
     */
    @GetMapping("/admin/detail")
    public R<FeedbackDetailDTO> adminDetail(@RequestParam("id") Long id, HttpServletRequest request) {
        requireAdmin(request);

        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return R.fail("反馈不存在");
        }

        List<FileAttachment> attachments = fileAttachmentService.list(
                new QueryWrapper<FileAttachment>()
                        .eq("biz_type", "FEEDBACK")
                        .eq("biz_id", id)
                        .orderByAsc("id")
        );

        List<FeedbackReply> replies = feedbackReplyService.list(
                new QueryWrapper<FeedbackReply>()
                        .eq("feedback_id", id)
                        .orderByAsc("create_time")
        );

        FeedbackDetailDTO dto = new FeedbackDetailDTO();
        dto.setFeedback(feedback);
        dto.setAttachments(attachments);
        dto.setReplies(replies);
        return R.ok(dto);
    }

    /**
     * 管理端：关闭反馈（标记为已处理）
     */
    @PostMapping("/admin/close")
    public R<Boolean> adminClose(@RequestParam("id") Long id, HttpServletRequest request) {

        requireAdmin(request);

        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return R.fail("反馈不存在");
        }

        // 已关闭直接返回（幂等）
        if ("CLOSED".equalsIgnoreCase(feedback.getStatus())) {
            return R.ok(true);
        }

        feedback.setStatus("CLOSED");
        feedback.setUpdateTime(LocalDateTime.now());
        boolean ok = feedbackService.updateById(feedback);

        // 可选：写一条操作记录，方便前端做时间线
        if (ok) {
            Long adminId = requireUserId(request);

            FeedbackReply reply = new FeedbackReply();
            reply.setFeedbackId(id);
            reply.setSenderRole("ADMIN");
            reply.setSenderId(adminId);
            reply.setContent("已标记为已处理（关闭反馈）");
            reply.setCreateTime(LocalDateTime.now());
            feedbackReplyService.save(reply);
        }

        return R.ok(ok);
    }

    // =========================
    // 小工具方法
    // =========================

    /**
     * 强制要求登录：从 request attribute 取 userId
     */
    private Long requireUserId(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            throw new BizException("未登录或 token 缺失");
        }
        return userId;
    }

    /**
     * 规范化附件 URL：只允许 feedback 上传目录
     */
    private String normalizeFeedbackUploadUrl(String url) {
        if (url == null) return null;
        String fixedUrl = url.trim();
        if (fixedUrl.isEmpty()) return null;

        // 1) 传相对路径：/upload/feedback/xxx.png
        if (fixedUrl.startsWith("/upload/")) {
            fixedUrl = baseUrl + fixedUrl;
        }

        // 2) 安全校验：只允许 upload/feedback
        if (!fixedUrl.contains("/upload/feedback/")) {
            throw new BizException("附件URL不合法：" + fixedUrl);
        }

        return fixedUrl;
    }
}
