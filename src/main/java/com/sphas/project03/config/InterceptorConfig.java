package com.sphas.project03.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * JWT 拦截器注册
 */
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**")
                .excludePathPatterns("/error")
                .excludePathPatterns("/v3/api-docs/**")
                .excludePathPatterns("/swagger-ui.html")
                .excludePathPatterns("/swagger-ui/**")
                .excludePathPatterns("/doc.html");
    }
}