package com.sphas.project03.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security 配置
 * 这里先按你当前 Spring Boot 2.7 的版本来写
 * 先保证能编译、能启动，再慢慢补完整
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // 前后端分离，先关掉 csrf
                .csrf().disable()

                // 不用表单登录
                .formLogin().disable()

                // 不用 httpBasic
                .httpBasic().disable()

                // JWT 项目改成无状态
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()

                .authorizeRequests()

                // 首页和静态资源
                .antMatchers(
                        "/",
                        "/index.html",
                        "/assets/**",
                        "/favicon.ico",
                        "/upload/**",
                        "/ping"
                ).permitAll()

                // 登录注册
                .antMatchers("/api/auth/**").permitAll()

                // Swagger / OpenAPI
                .antMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/doc.html"
                ).permitAll()

                // 其他接口先放行
                // 这里先不强制 authenticated
                // 因为你现在真正的 token 校验还是靠 JwtInterceptor
                .anyRequest().permitAll();

        return http.build();
    }
}