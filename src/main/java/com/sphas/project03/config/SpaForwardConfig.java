package com.sphas.project03.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ✅ SPA（Vue）前端路由兜底
 * 让 /dashboard /notice 等路径刷新不会 404
 * /api/** 不会受影响，因为它会被 Controller 匹配优先处理
 */
@Configuration
public class SpaForwardConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {

        // 形如 /dashboard
        registry.addViewController("/{path:[^\\.]*}")
                .setViewName("forward:/index.html");

        // 形如 /admin/notice/list
        registry.addViewController("/**/{path:[^\\.]*}")
                .setViewName("forward:/index.html");
    }
}
