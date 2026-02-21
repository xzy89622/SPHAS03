package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.HealthArticle;
import com.sphas.project03.service.HealthArticleService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康科普文章 Controller
 */
@RestController
@RequestMapping("/api/article")
public class HealthArticleController extends BaseController {

    private final HealthArticleService articleService;

    public HealthArticleController(HealthArticleService articleService) {
        this.articleService = articleService;
    }

    // ==========================
    // 用户端：列表/详情（只看已发布）
    // ==========================

    @GetMapping("/list")
    public R<List<HealthArticle>> list() {
        List<HealthArticle> list = articleService.list(
                new LambdaQueryWrapper<HealthArticle>()
                        .eq(HealthArticle::getStatus, 1)
                        .orderByDesc(HealthArticle::getPublishTime)
        );
        return R.ok(list);
    }

    @GetMapping("/{id}")
    public R<HealthArticle> detail(@PathVariable Long id) {
        HealthArticle article = articleService.getById(id);
        if (article == null || article.getStatus() == null || article.getStatus() != 1) {
            throw new BizException("文章不存在或已下线");
        }
        return R.ok(article);
    }

    // ==========================
    // 管理端：分页/保存/上下线
    // ==========================

    /**
     * 管理端：文章分页（看全部，可按标题模糊，可按状态筛选）
     * GET /api/article/admin/page?pageNum=1&pageSize=10&title=xxx&status=1
     */
    @GetMapping("/admin/page")
    public R<Page<HealthArticle>> adminPage(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String title,
                                            @RequestParam(required = false) Integer status,
                                            HttpServletRequest request) {

        requireAdmin(request);

        Page<HealthArticle> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<HealthArticle> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(title)) {
            qw.like(HealthArticle::getTitle, title);
        }
        if (status != null) {
            qw.eq(HealthArticle::getStatus, status);
        }
        qw.orderByDesc(HealthArticle::getPublishTime)
                .orderByDesc(HealthArticle::getCreateTime);

        return R.ok(articleService.page(page, qw));
    }

    /**
     * 管理端：新增或更新（id有值=更新；id为空=新增）
     * POST /api/article/admin/save
     */
    @PostMapping("/admin/save")
    public R<Long> save(@RequestBody HealthArticle article, HttpServletRequest request) {

        requireAdmin(request);

        LocalDateTime now = LocalDateTime.now();

        if (article.getId() == null) {
            // 新增
            if (!StringUtils.hasText(article.getTitle())) throw new BizException("title不能为空");
            if (!StringUtils.hasText(article.getContent())) throw new BizException("content不能为空");

            if (article.getStatus() == null) article.setStatus(1); // 默认发布
            if (article.getPublishTime() == null && article.getStatus() == 1) {
                article.setPublishTime(now);
            }

            article.setCreateTime(now);
            article.setUpdateTime(now);

            // 作者：如果你后端有拦截器写入 userId，可填上（没有也不影响）
            Long authorId = getUserId(request);
            if (authorId != null) article.setAuthorId(authorId);

            articleService.save(article);
        } else {
            // 更新
            HealthArticle db = articleService.getById(article.getId());
            if (db == null) throw new BizException("文章不存在");

            if (StringUtils.hasText(article.getTitle())) db.setTitle(article.getTitle());
            if (StringUtils.hasText(article.getSummary())) db.setSummary(article.getSummary());
            if (StringUtils.hasText(article.getCoverUrl())) db.setCoverUrl(article.getCoverUrl());
            if (StringUtils.hasText(article.getContent())) db.setContent(article.getContent());

            if (article.getStatus() != null) {
                db.setStatus(article.getStatus());
                // 从下线切回上线时，补 publishTime
                if (article.getStatus() == 1 && db.getPublishTime() == null) {
                    db.setPublishTime(now);
                }
            }

            db.setUpdateTime(now);
            articleService.updateById(db);
        }

        return R.ok(article.getId());
    }

    /**
     * 管理端：下线文章
     * POST /api/article/admin/offline/{id}
     */
    @PostMapping("/admin/offline/{id}")
    public R<Boolean> offline(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);

        HealthArticle db = articleService.getById(id);
        if (db == null) throw new BizException("文章不存在");

        db.setStatus(0);
        db.setUpdateTime(LocalDateTime.now());
        return R.ok(articleService.updateById(db));
    }

    /**
     * 管理端：上线文章（可选，但前端会用到更方便）
     * POST /api/article/admin/online/{id}
     */
    @PostMapping("/admin/online/{id}")
    public R<Boolean> online(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);

        HealthArticle db = articleService.getById(id);
        if (db == null) throw new BizException("文章不存在");

        db.setStatus(1);
        if (db.getPublishTime() == null) db.setPublishTime(LocalDateTime.now());
        db.setUpdateTime(LocalDateTime.now());
        return R.ok(articleService.updateById(db));
    }
}
