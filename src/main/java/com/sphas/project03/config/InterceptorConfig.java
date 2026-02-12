package com.sphas.project03.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 拦截器注册：对 /api/** 做 JWT 校验，但放行 /api/auth/**
 */
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // ✅ 拦截所有 api
                .addPathPatterns("/api/**")
                // ✅ 放行登录/注册接口
                .excludePathPatterns("/api/auth/**")
                // ✅ 可选：放行错误页
                .excludePathPatterns("/error");
    }
}
