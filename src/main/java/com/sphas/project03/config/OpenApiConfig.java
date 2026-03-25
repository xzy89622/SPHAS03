package com.sphas.project03.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI projectOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SPHAS 接口文档")
                        .description("智能化个人健康系统后端接口文档，供小程序端、管理端联调使用。")
                        .version("v1.0.0")
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
}