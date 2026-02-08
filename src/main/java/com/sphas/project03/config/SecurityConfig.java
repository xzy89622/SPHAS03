package com.sphas.project03.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * ✅ 一体化 8080 的 Security 配置（兼容你当前项目版本）
 * 放行：前端页面 + 静态资源 + 登录接口
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // 关闭 csrf（前后端分离/一体化常用）
                .csrf(csrf -> csrf.disable())

                // 不使用默认登录页
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .authorizeHttpRequests(auth -> auth
                        // ✅ 放行前端页面与静态资源
                        .requestMatchers(
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/index.html"),
                                new AntPathRequestMatcher("/assets/**"),
                                new AntPathRequestMatcher("/favicon.ico")
                        ).permitAll()

                        // ✅ 放行登录/注册接口
                        .requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()

                        // 其他接口：你如果 JWT 在拦截器里做，这里可以先放行
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
