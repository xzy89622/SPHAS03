package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.Feedback;
import com.sphas.project03.service.FeedbackService;
import com.sphas.project03.service.FileAttachmentService;
import com.sphas.project03.utils.JwtUtil;
import org.springframework.web.bind.annotation.*;
import io.jsonwebtoken.Claims;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import com.sphas.project03.entity.FileAttachment;
import com.sphas.project03.controller.dto.FeedbackSubmitDTO;
import com.sphas.project03.controller.dto.FeedbackDetailDTO;
import com.sphas.project03.entity.FileAttachment;
import com.sphas.project03.service.FeedbackReplyService;
import com.sphas.project03.entity.FeedbackReply;
import com.sphas.project03.controller.dto.FeedbackReplyAddDTO;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    //private static final String JWT_SECRET = "project03_jwt_secret_key_32_bytes_minimum";
    private final FeedbackService feedbackService;
    @Value("${jwt.secret}")
    private String JWT_SECRET;
    private final FileAttachmentService fileAttachmentService;
    private final FeedbackReplyService feedbackReplyService;

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
        Long userId = getUserIdFromRequest(request);

        // 1) 先保存 feedback 主表
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

        // 2) 再保存附件表（bizId = feedback.id）
        if (dto.getAttachmentUrls() != null && !dto.getAttachmentUrls().isEmpty()) {
            for (String url : dto.getAttachmentUrls()) {
                FileAttachment attachment = new FileAttachment();
                attachment.setBizType("FEEDBACK");
                attachment.setBizId(feedback.getId()); // ✅ 关键：不再是 null
                attachment.setFileName("feedback-upload"); // 这里你也可以不存原名（除非你想）
                // 0) URL 修复：如果只传了 host（例如 http://localhost:8080/），直接判错
                if (url == null || url.trim().isEmpty()) {
                    continue; // 或者 return R.fail("附件URL为空");
                }

                String fixedUrl = url.trim();

// 如果用户传的是相对路径（/upload/feedback/xxx.png），补全 host
                if (fixedUrl.startsWith("/upload/")) {
                    fixedUrl = "http://localhost:8080" + fixedUrl;
                }

// 如果 URL 不是我们允许的上传路径，直接拒绝（防止乱塞）
                if (!fixedUrl.contains("/upload/feedback/")) {
                    return R.fail("附件URL不合法：" + fixedUrl);
                }

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
        Long userId = getUserIdFromRequest(request);

        List<Feedback> list = feedbackService.list(
                new QueryWrapper<Feedback>()
                        .eq("user_id", userId)
                        .orderByDesc("create_time")
        );
        return R.ok(list);
    }

    /**
     * 用户：反馈详情（先只返回 feedback 主表）
     * 回复/附件我们下一步加
     */
    //@GetMapping("/{id}")
    /**
     * 反馈详情（包含附件）
     */
    @GetMapping("/{id}/detail")
    public R<FeedbackDetailDTO> detail(@PathVariable Long id,
                                       HttpServletRequest request) {

        // 1. 查反馈
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return R.fail("反馈不存在");
        }

        // 2. 校验权限（只能看自己的）
        Long userId = getUserIdFromRequest(request);
        if (!userId.equals(feedback.getUserId())) {
            return R.fail("无权查看该反馈");
        }

        // 3. 查附件
        List<FileAttachment> attachments = fileAttachmentService.list(
                new QueryWrapper<FileAttachment>()
                        .eq("biz_type", "FEEDBACK")
                        .eq("biz_id", id)
                        .orderByAsc("id")
        );
// 4) 查回复列表
        List<FeedbackReply> replies = feedbackReplyService.list(
                new QueryWrapper<FeedbackReply>()
                        .eq("feedback_id", id)
                        .orderByAsc("create_time")
        );

        // 4. 组装返回
        FeedbackDetailDTO dto = new FeedbackDetailDTO();
        dto.setFeedback(feedback);
        dto.setAttachments(attachments);
        dto.setReplies(replies);
        return R.ok(dto);
    }

    /**
     * 从请求头 Authorization: Bearer xxx 解析 userId
     */

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new RuntimeException("未登录或 token 缺失");
        }

        String token = auth.substring(7);

        Claims claims = JwtUtil.parseToken(token, JWT_SECRET);

        // subject 里就是 userId
        return Long.valueOf(claims.getSubject());
    }
    /**
     * 上传反馈图片（只负责上传并记录附件表，不绑定具体 feedback）
     * 返回：图片访问 URL
     */
    @PostMapping("/upload")
    public R<String> upload(@RequestParam("file") MultipartFile file,
                            HttpServletRequest request) throws IOException {

        if (file == null || file.isEmpty()) {
            return R.fail("文件为空");
        }

        // 1) 解析当前登录用户ID（复用你已写好的方法）
        Long userId = getUserIdFromRequest(request);

        // 2) 生成新文件名（避免重名覆盖）
        String originalName = file.getOriginalFilename();
        String suffix = "";
        if (originalName != null && originalName.contains(".")) {
            suffix = originalName.substring(originalName.lastIndexOf("."));
        }
        String newName = UUID.randomUUID().toString().replace("-", "") + suffix;

        // 3) 保存到本地：upload/feedback/
        // 1) 项目运行目录（一般就是你的项目根目录）
        String baseDir = System.getProperty("user.dir");

// 2) 上传目录：<项目根>/upload/feedback
        File dir = new File(baseDir, "upload/feedback");
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                return R.fail("创建上传目录失败：" + dir.getAbsolutePath());
            }
        }

// 3) 目标文件
        File dest = new File(dir, newName);

// 4) 保存
        file.transferTo(dest);


        // 4) 生成访问 URL（先用本机地址；后面可做配置化）
       // String fileUrl = "http://localhost:8080/upload/feedback/" + newName;

        // 5) 写入附件表
//        FileAttachment attachment = new FileAttachment();
//        attachment.setBizType("FEEDBACK"); // 业务类型
//        attachment.setBizId(null);         // 先不绑定具体 feedback，后面提交反馈时再绑定
//        attachment.setFileName(originalName);
//        attachment.setFileUrl(fileUrl);
//        attachment.setFileSize(file.getSize());
//        attachment.setMimeType(file.getContentType());
//        attachment.setUploadUserId(userId);
//        attachment.setCreateTime(LocalDateTime.now());
//
//        fileAttachmentService.save(attachment);
// 4) 生成访问 URL
        String fileUrl = "http://localhost:8080/upload/feedback/" + newName;

// 直接返回 URL（不写 file_attachment）
        return R.ok(fileUrl);

        // 6) 返回 URL 给前端
        //return R.ok(fileUrl);
    }
    /**
     * 反馈详情（主表 + 附件 + 回复）
     */
    @GetMapping("/{id}/detail/attachments")
    public R<FeedbackDetailDTO> detailWithAttachments(@PathVariable Long id,
                                                      HttpServletRequest request) {

        // 1) 查询反馈
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return R.fail("反馈不存在");
        }

        // 2) 只允许本人查看（你之前已有逻辑，这里复用）
        Long userId = getUserIdFromRequest(request);
        if (!userId.equals(feedback.getUserId())) {
            return R.fail("无权限查看该反馈");
        }

        // 3) 查询附件（bizType + bizId）
        List<FileAttachment> attachments = fileAttachmentService.list(
                new QueryWrapper<FileAttachment>()
                        .eq("biz_type", "FEEDBACK")
                        .eq("biz_id", id)
                        .orderByAsc("id")
        );

        // 4) 组装返回
        FeedbackDetailDTO dto = new FeedbackDetailDTO();
        dto.setFeedback(feedback);
        dto.setAttachments(attachments);
        dto.setReplies(java.util.Collections.emptyList()); // 先空，后面做回复再填

        return R.ok(dto);
    }
    private String getRoleFromRequest(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new RuntimeException("未登录或 token 缺失");
        }
        String token = auth.substring(7);
        Claims claims = JwtUtil.parseToken(token, JWT_SECRET);

        // 你生成 token 时写的是 claim("role", role)
        Object role = claims.get("role");
        return role == null ? "" : role.toString();
    }
    /**
     * 管理员回复反馈
     */
    @PostMapping("/{id}/reply")
    public R<Boolean> adminReply(@PathVariable Long id,
                                 @RequestBody FeedbackReplyAddDTO dto,
                                 HttpServletRequest request) {

        // 0) 校验：只有管理员能回复
        String role = getRoleFromRequest(request);
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return R.fail("仅管理员可回复");
        }

        // 1) 反馈是否存在
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return R.fail("反馈不存在");
        }

        // 2) 取管理员 userId（token 里 subject）
        Long adminId = getUserIdFromRequest(request);

        // 3) 写入 reply
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
     * GET /api/feedback/admin/list
     */
    @GetMapping("/admin/list")
    public R<List<Feedback>> adminList(HttpServletRequest request) {
        String role = getRoleFromRequest(request);
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return R.fail("仅管理员可查看");
        }

        List<Feedback> list = feedbackService.list(
                new QueryWrapper<Feedback>()
                        .orderByDesc("create_time")
        );
        return R.ok(list);
    }

    /**
     * 管理端：反馈详情（仅管理员，含附件+回复）
     * GET /api/feedback/admin/detail?id=123
     */
    @GetMapping("/admin/detail")
    public R<FeedbackDetailDTO> adminDetail(@RequestParam("id") Long id, HttpServletRequest request) {
        String role = getRoleFromRequest(request);
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return R.fail("仅管理员可查看");
        }

        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) return R.fail("反馈不存在");

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
     * POST /api/feedback/admin/close?id=123
     */
    @PostMapping("/admin/close")
    public R<Boolean> adminClose(@RequestParam("id") Long id, HttpServletRequest request) {

        // 0) 仅管理员可操作
        String role = getRoleFromRequest(request);
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return R.fail("仅管理员可操作");
        }

        // 1) 反馈是否存在
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return R.fail("反馈不存在");
        }

        // 2) 如果已关闭，直接返回 true（幂等）
        if ("CLOSED".equalsIgnoreCase(feedback.getStatus())) {
            return R.ok(true);
        }

        // 3) 更新状态
        feedback.setStatus("CLOSED");
        feedback.setUpdateTime(LocalDateTime.now());
        boolean ok = feedbackService.updateById(feedback);

        // 4) 可选：写一条“系统/管理员操作记录”到回复表，便于时间线展示
        if (ok) {
            Long adminId = getUserIdFromRequest(request);

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


}

