package com.sphas.project03.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.HealthArticle;
import com.sphas.project03.service.HealthArticleService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康科普文章 Controller
 */
@RestController
@RequestMapping("/api/article")
public class HealthArticleController {

    private final HealthArticleService articleService;

    public HealthArticleController(HealthArticleService articleService) {
        this.articleService = articleService;
    }

    /**
     * 前台：文章列表（只返回已发布的）
     */
    @GetMapping("/list")
    public R<List<HealthArticle>> list() {
        List<HealthArticle> list = articleService.list(
                new QueryWrapper<HealthArticle>()
                        .eq("status", 1)
                        .orderByDesc("publish_time")
        );
        return R.ok(list);
    }

    /**
     * 前台：文章详情
     */
    @GetMapping("/{id}")
    public R<HealthArticle> detail(@PathVariable Long id) {
        HealthArticle article = articleService.getById(id);
        return R.ok(article);
    }

    /**
     * 管理员：发布文章
     */
    @PostMapping("/admin/publish")
    public R<Boolean> publish(@RequestBody HealthArticle article) {
        article.setStatus(1);
        article.setPublishTime(LocalDateTime.now());
        articleService.save(article);
        return R.ok(true);
    }

    /**
     * 管理员：下线文章
     */
    @PostMapping("/admin/offline/{id}")
    public R<Boolean> offline(@PathVariable Long id) {
        HealthArticle article = new HealthArticle();
        article.setId(id);
        article.setStatus(0);
        articleService.updateById(article);
        return R.ok(true);
    }
}

