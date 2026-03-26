package com.sphas.project03.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 配置
 * 这里把接口按模块分组，联调时更清楚
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI projectOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SPHAS 接口文档")
                        .description("智能化个人健康系统后端接口文档，供微信小程序端与管理端统一联调使用。")
                        .version("v1.1.0")
                        .contact(new Contact()
                                .name("SPHAS Team")
                                .email("team@sphas.local"))
                        .license(new License()
                                .name("Internal Use Only")
                                .url("https://localhost")))
                .externalDocs(new ExternalDocumentation()
                        .description("项目说明")
                        .url("https://localhost/project-doc"));
    }

    /**
     * 认证与用户模块
     */
    @Bean
    public GroupedOpenApi authUserApi() {
        return GroupedOpenApi.builder()
                .group("01-认证与用户")
                .pathsToMatch(
                        "/api/auth/**",
                        "/api/user/**"
                )
                .build();
    }

    /**
     * 健康档案、分析、报告模块
     */
    @Bean
    public GroupedOpenApi healthApi() {
        return GroupedOpenApi.builder()
                .group("02-健康档案与分析")
                .pathsToMatch(
                        "/api/health/**",
                        "/api/assessment/**",
                        "/api/risk/**",
                        "/api/dashboard/**"
                )
                .build();
    }

    /**
     * 推荐与用户计划模块
     */
    @Bean
    public GroupedOpenApi recommendApi() {
        return GroupedOpenApi.builder()
                .group("03-推荐与计划")
                .pathsToMatch(
                        "/api/recommend/**",
                        "/api/plan/**"
                )
                .build();
    }

    /**
     * 公告、文章、反馈、消息模块
     */
    @Bean
    public GroupedOpenApi contentApi() {
        return GroupedOpenApi.builder()
                .group("04-内容与消息")
                .pathsToMatch(
                        "/api/article/**",
                        "/api/notice/**",
                        "/api/feedback/**",
                        "/api/message/**"
                )
                .build();
    }

    /**
     * 社交、挑战、积分、排行榜模块
     */
    @Bean
    public GroupedOpenApi socialApi() {
        return GroupedOpenApi.builder()
                .group("05-社交与激励")
                .pathsToMatch(
                        "/api/social/**",
                        "/api/challenge/**",
                        "/api/leaderboard/**",
                        "/api/points/**",
                        "/api/badge/**"
                )
                .build();
    }

    /**
     * 管理端接口
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("06-后台管理")
                .pathsToMatch(
                        "/api/admin/**",
                        "/api/question-bank/**",
                        "/api/bmi-standard/**",
                        "/api/ops/**",
                        "/api/report/admin/**",
                        "/api/feedback/admin/**",
                        "/api/article/admin/**",
                        "/api/notice/admin/**",
                        "/api/message/admin/**",
                        "/api/social/audit/**",
                        "/api/recommend/admin/**",
                        "/api/point-record/admin/**",
                        "/api/user-plan/admin/**",
                        "/api/blockchain/**"
                )
                .build();
    }

    /**
     * AI 顾问与 AI 服务模块
     */
    @Bean
    public GroupedOpenApi aiApi() {
        return GroupedOpenApi.builder()
                .group("07-AI服务")
                .pathsToMatch(
                        "/api/ai/**",
                        "/api/advisor/**"
                )
                .build();
    }
}