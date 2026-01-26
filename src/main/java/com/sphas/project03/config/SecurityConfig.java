package com.sphas.project03.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security配置：关闭默认登录，放行我们自己的接口
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable() // 关闭csrf，方便前后端分离
                .httpBasic().disable() // 关闭默认Basic认证
                .formLogin().disable() // 关闭默认登录页
                .authorizeRequests()
                .antMatchers("/ping").permitAll() // 放行测试
                .antMatchers("/api/auth/**").permitAll() // 放行注册登录
                .anyRequest().permitAll(); // 其他先放行（JWT由拦截器控制）

        return http.build();
    }
}
