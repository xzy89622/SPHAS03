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


@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    //private static final String JWT_SECRET = "project03_jwt_secret_key_32_bytes_minimum";
    private final FeedbackService feedbackService;
    @Value("${jwt.secret}")
    private String JWT_SECRET;
    private final FileAttachmentService fileAttachmentService;

    public FeedbackController(FeedbackService feedbackService,
                              FileAttachmentService fileAttachmentService) {
        this.feedbackService = feedbackService;
        this.fileAttachmentService = fileAttachmentService;
    }

    /**
     * 用户：提交反馈
     */
    @PostMapping("/submit")
    public R<Boolean> submit(@RequestBody Feedback feedback, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);

        feedback.setUserId(userId);
        feedback.setStatus("OPEN");
        feedback.setCreateTime(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());

        boolean ok = feedbackService.save(feedback);
        return R.ok(ok);
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
    @GetMapping("/{id}")
    public R<Feedback> detail(@PathVariable Long id, HttpServletRequest request) {
        // 先查出来
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return R.fail("反馈不存在");
        }

        // 只允许本人查看（防止越权）
        Long userId = getUserIdFromRequest(request);
        if (!userId.equals(feedback.getUserId())) {
            return R.fail("无权限查看该反馈");
        }

        return R.ok(feedback);
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
        File dir = new File("upload/feedback");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File dest = new File(dir, newName);
        file.transferTo(dest);

        // 4) 生成访问 URL（先用本机地址；后面可做配置化）
        String fileUrl = "http://localhost:8080/upload/feedback/" + newName;

        // 5) 写入附件表
        FileAttachment attachment = new FileAttachment();
        attachment.setBizType("FEEDBACK"); // 业务类型
        attachment.setBizId(null);         // 先不绑定具体 feedback，后面提交反馈时再绑定
        attachment.setFileName(originalName);
        attachment.setFileUrl(fileUrl);
        attachment.setFileSize(file.getSize());
        attachment.setMimeType(file.getContentType());
        attachment.setUploadUserId(userId);
        attachment.setCreateTime(LocalDateTime.now());

        fileAttachmentService.save(attachment);

        // 6) 返回 URL 给前端
        return R.ok(fileUrl);
    }

}

