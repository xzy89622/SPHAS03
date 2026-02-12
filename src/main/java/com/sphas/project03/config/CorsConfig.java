package com.sphas.project03.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ 允许本机所有端口（解决你 5173/5174/5175 变动导致的 403）
        // Spring Boot 2.7 支持 allowedOriginPatterns
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));

        // 你这里用 JWT 放 Header，不依赖 Cookie，其实可以 false
        // 为了兼容性，这里保持 true 也可以
        config.setAllowCredentials(true);

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        // 如需前端读取 Authorization 等响应头
        config.setExposedHeaders(Arrays.asList("Authorization"));

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
