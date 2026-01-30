package com.sphas.project03.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * ✅ 全局 CORS 配置
 * 允许前端(http://localhost:5174) 跨域访问后端(http://localhost:8080)
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 1️⃣ 允许的前端来源（开发环境）
        config.setAllowedOrigins(
                Arrays.asList("http://localhost:5175")
        );

        // 2️⃣ 是否允许携带 cookie（JWT 放 header 也可以 true）
        config.setAllowCredentials(true);

        // 3️⃣ 允许的请求头（Authorization 非常关键）
        config.setAllowedHeaders(
                Arrays.asList("*")
        );

        // 4️⃣ 允许的请求方法（OPTIONS 一定要有）
        config.setAllowedMethods(
                Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );

        // 5️⃣ 允许前端读取的响应头
        config.setExposedHeaders(
                Arrays.asList("Authorization")
        );

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
