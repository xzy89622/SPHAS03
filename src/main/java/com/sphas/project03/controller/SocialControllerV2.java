package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.PointRecord;
import com.sphas.project03.entity.SocialComment;
import com.sphas.project03.entity.SocialLike;
import com.sphas.project03.entity.SocialPost;
import com.sphas.project03.service.AchievementService;
import com.sphas.project03.service.PointRecordService;
import com.sphas.project03.service.PointsLeaderboardService;
import com.sphas.project03.service.SocialCommentService;
import com.sphas.project03.service.SocialLikeService;
import com.sphas.project03.service.SocialPostService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 社交日志模块（用户端）
 */
@RestController
@RequestMapping("/api/social")
public class SocialControllerV2 extends BaseController {

    private final SocialPostService postService;
    private final SocialCommentService commentService;
    private final SocialLikeService likeService;

    private final PointRecordService pointRecordService;
    private final PointsLeaderboardService pointsLeaderboardService;
    private final AchievementService achievementService;

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.baseUrl:http://localhost:8080}")
    private String baseUrl;

    public SocialControllerV2(SocialPostService postService,
                              SocialCommentService commentService,
                              SocialLikeService likeService,
                              PointRecordService pointRecordService,
                              PointsLeaderboardService pointsLeaderboardService,
                              AchievementService achievementService,
                              StringRedisTemplate stringRedisTemplate) {
        this.postService = postService;
        this.commentService = commentService;
        this.likeService = likeService;
        this.pointRecordService = pointRecordService;
        this.pointsLeaderboardService = pointsLeaderboardService;
        this.achievementService = achievementService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * ✅ 社区图片上传（返回可访问URL）
     * 前端先上传拿到URL，拼进 imagesJson 再发帖
     */
    @PostMapping("/upload")
    public R<String> upload(@RequestParam("file") MultipartFile file,
                            HttpServletRequest request) throws IOException {

        // ✅ 你项目里没有 requireUserId()，这里按你原来习惯写
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        if (file == null || file.isEmpty()) {
            return R.fail("文件为空");
        }

        // 1) 新文件名
        String originalName = file.getOriginalFilename();
        String suffix = "";
        if (originalName != null && originalName.contains(".")) {
            suffix = originalName.substring(originalName.lastIndexOf("."));
        }
        String newName = UUID.randomUUID().toString().replace("-", "") + suffix;

        // 2) 保存到本地：<项目根>/upload/social
        String baseDir = System.getProperty("user.dir");
        File dir = new File(baseDir, "upload/social");
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                return R.fail("创建上传目录失败：" + dir.getAbsolutePath());
            }
        }

        File dest = new File(dir, newName);
        file.transferTo(dest);

        // 3) 返回访问URL（WebConfig 已把 upload/ 映射到 /upload/**）
        String fileUrl = baseUrl + "/upload/social/" + newName;
        return R.ok(fileUrl);
    }

    /**
     * 发帖（默认待审核 status=2）
     */
    @PostMapping("/post/create")
    @Transactional(rollbackFor = Exception.class)
    public R<Long> createPost(@RequestBody CreatePostDTO dto, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");
        if (dto == null || !StringUtils.hasText(dto.getContent())) throw new BizException("content不能为空");

        LocalDateTime now = LocalDateTime.now();

        SocialPost p = new SocialPost();
        p.setUserId(userId);
        p.setContent(dto.getContent());
        p.setImagesJson(dto.getImagesJson());
        p.setLikeCount(0);
        p.setCommentCount(0);
        p.setStatus(2); // ✅ 待审核
        p.setCreateTime(now);
        p.setUpdateTime(now);
        postService.save(p);

        // ✅ 发帖积分 +5（幂等）
        addPointsOnce(userId, 5, "POST_CREATE", p.getId(), "发布日志/帖子");

        // ✅ 触发勋章：发帖次数成就
        achievementService.onPostCreated(userId);

        return R.ok(p.getId());
    }

    /**
     * 帖子分页（用户端只看通过 status=1）
     */
    @GetMapping("/post/page")
    public R<Page<SocialPost>> page(@RequestParam(defaultValue = "1") long pageNum,
                                    @RequestParam(defaultValue = "10") long pageSize) {

        Page<SocialPost> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SocialPost> qw = new LambdaQueryWrapper<>();
        qw.eq(SocialPost::getStatus, 1);
        qw.orderByDesc(SocialPost::getCreateTime);

        return R.ok(postService.page(page, qw));
    }

    /**
     * 帖子详情：通过可看；待审/驳回仅作者可看
     */
    @GetMapping("/post/detail")
    public R<PostDetailVO> detail(@RequestParam Long postId, HttpServletRequest request) {
        SocialPost p = postService.getById(postId);
        if (p == null) throw new BizException("帖子不存在");

        Long userId = getUserId(request);

        // ✅ 通过：所有人可看
        if (p.getStatus() != null && p.getStatus() == 1) {
            return R.ok(buildDetail(p, userId));
        }

        // ✅ 待审/驳回/隐藏：只有作者本人能看
        if (userId == null || !userId.equals(p.getUserId())) {
            throw new BizException("帖子未通过审核或已隐藏");
        }

        return R.ok(buildDetail(p, userId));
    }

    /**
     * 评论分页（帖子必须通过）
     */
    @GetMapping("/comment/page")
    public R<Page<SocialComment>> commentPage(@RequestParam Long postId,
                                              @RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize) {

        SocialPost p = postService.getById(postId);
        if (p == null || p.getStatus() == null || p.getStatus() != 1) {
            throw new BizException("帖子未通过审核");
        }

        Page<SocialComment> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SocialComment> qw = new LambdaQueryWrapper<>();
        qw.eq(SocialComment::getPostId, postId);
        qw.orderByAsc(SocialComment::getCreateTime);

        return R.ok(commentService.page(page, qw));
    }

    /**
     * 评论（帖子必须通过；+2积分）
     */
    @PostMapping("/comment/add")
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> addComment(@RequestBody AddCommentDTO dto, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");
        if (dto == null || dto.getPostId() == null) throw new BizException("postId不能为空");
        if (!StringUtils.hasText(dto.getContent())) throw new BizException("content不能为空");

        SocialPost p = postService.getById(dto.getPostId());
        if (p == null || p.getStatus() == null || p.getStatus() != 1) throw new BizException("帖子未通过审核");

        SocialComment c = new SocialComment();
        c.setPostId(dto.getPostId());
        c.setUserId(userId);
        c.setContent(dto.getContent());
        c.setCreateTime(LocalDateTime.now());
        commentService.save(c);

        p.setCommentCount((p.getCommentCount() == null ? 0 : p.getCommentCount()) + 1);
        p.setUpdateTime(LocalDateTime.now());
        postService.updateById(p);

        addPointsOnce(userId, 2, "POST_COMMENT", dto.getPostId(), "评论帖子");

        return R.ok(true);
    }

    /**
     * 点赞/取消点赞（帖子必须通过；点赞+1积分）
     */
    @PostMapping("/like/toggle")
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> toggleLike(@RequestBody LikeDTO dto, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");
        if (dto == null || dto.getPostId() == null) throw new BizException("postId不能为空");

        SocialPost p = postService.getById(dto.getPostId());
        if (p == null || p.getStatus() == null || p.getStatus() != 1) throw new BizException("帖子未通过审核");

        SocialLike exist = likeService.getOne(new LambdaQueryWrapper<SocialLike>()
                .eq(SocialLike::getPostId, dto.getPostId())
                .eq(SocialLike::getUserId, userId));

        if (exist == null) {
            SocialLike l = new SocialLike();
            l.setPostId(dto.getPostId());
            l.setUserId(userId);
            l.setCreateTime(LocalDateTime.now());
            likeService.save(l);

            p.setLikeCount((p.getLikeCount() == null ? 0 : p.getLikeCount()) + 1);
            p.setUpdateTime(LocalDateTime.now());
            postService.updateById(p);

            addPointsOnce(userId, 1, "POST_LIKE", dto.getPostId(), "点赞帖子");
            return R.ok(true);
        } else {
            likeService.removeById(exist.getId());

            int lc = p.getLikeCount() == null ? 0 : p.getLikeCount();
            p.setLikeCount(Math.max(0, lc - 1));
            p.setUpdateTime(LocalDateTime.now());
            postService.updateById(p);

            return R.ok(true);
        }
    }

    /**
     * 删除帖子（本人删除，status=0）
     */
    @PostMapping("/post/delete")
    public R<Boolean> delete(@RequestParam Long postId, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        SocialPost p = postService.getById(postId);
        if (p == null) throw new BizException("帖子不存在");
        if (!userId.equals(p.getUserId())) throw new BizException("只能删除自己的帖子");

        p.setStatus(0);
        p.setUpdateTime(LocalDateTime.now());
        postService.updateById(p);

        return R.ok(true);
    }

    // =========================
    // 详情构建
    // =========================

    private PostDetailVO buildDetail(SocialPost p, Long userId) {
        PostDetailVO vo = new PostDetailVO();
        vo.setPost(p);

        if (userId == null) {
            vo.setLiked(false);
            return vo;
        }

        SocialLike like = likeService.getOne(new LambdaQueryWrapper<SocialLike>()
                .eq(SocialLike::getPostId, p.getId())
                .eq(SocialLike::getUserId, userId));
        vo.setLiked(like != null);
        return vo;
    }

    // =========================
    // 积分工具方法（幂等）
    // =========================

    private void addPointsOnce(Long userId, int points, String type, Long bizId, String remark) {
        PointRecord exist = pointRecordService.getOne(new LambdaQueryWrapper<PointRecord>()
                .eq(PointRecord::getUserId, userId)
                .eq(PointRecord::getType, type)
                .eq(PointRecord::getBizId, bizId));
        if (exist != null) return;

        PointRecord pr = new PointRecord();
        pr.setUserId(userId);
        pr.setPoints(points);
        pr.setType(type);
        pr.setBizId(bizId);
        pr.setRemark(remark);
        pr.setCreateTime(LocalDateTime.now());
        pointRecordService.save(pr);

        // ✅ 刷新积分榜
        pointsLeaderboardService.incrPoints(userId, points);

        // ✅ 触发勋章：积分达标（从Redis读总分，读不到就跳过）
        Integer total = readTotalPointsFromRedis(userId);
        if (total != null) {
            achievementService.onPointsChanged(userId, total);
        }
    }

    private Integer readTotalPointsFromRedis(Long userId) {
        try {
            Double score = stringRedisTemplate.opsForZSet().score("project03:points:total", String.valueOf(userId));
            if (score == null) return null;
            return score.intValue();
        } catch (Exception e) {
            return null;
        }
    }

    // =========================
    // DTO/VO
    // =========================

    public static class CreatePostDTO {
        private String content;
        private String imagesJson;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getImagesJson() { return imagesJson; }
        public void setImagesJson(String imagesJson) { this.imagesJson = imagesJson; }
    }

    public static class AddCommentDTO {
        private Long postId;
        private String content;

        public Long getPostId() { return postId; }
        public void setPostId(Long postId) { this.postId = postId; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class LikeDTO {
        private Long postId;

        public Long getPostId() { return postId; }
        public void setPostId(Long postId) { this.postId = postId; }
    }

    public static class PostDetailVO {
        private SocialPost post;
        private boolean liked;

        public SocialPost getPost() { return post; }
        public void setPost(SocialPost post) { this.post = post; }

        public boolean isLiked() { return liked; }
        public void setLiked(boolean liked) { this.liked = liked; }
    }
}