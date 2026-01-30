package com.sphas.project03.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ✅ 一体化 8080 的 Security 配置
 * 关键目标：
 * 1. 放行前端页面 /
 * 2. 放行静态资源 /assets/**
 * 3. 放行登录接口 /api/auth/**
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // 前后端一体化，一般关闭 csrf
                .csrf(csrf -> csrf.disable())

                // 不使用默认登录页
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // ✅ 关键：使用 authorizeHttpRequests（不是 authorizeRequests）
                .authorizeHttpRequests(auth -> auth
                        // 前端页面 & 静态资源必须放行
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/assets/**",
                                "/favicon.ico"
                        ).permitAll()

                        // 登录接口
                        .requestMatchers("/api/auth/**").permitAll()

                        // 其他接口：你如果用 JWT 拦截器，这里可以先放行
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
