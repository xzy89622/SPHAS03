package com.sphas.project03;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类：项目入口
 */
@SpringBootApplication
@MapperScan("com.sphas.project03.mapper")
public class Project03Application {

    public static void main(String[] args) {
        SpringApplication.run(Project03Application.class, args); // 启动Spring Boot
    }
}
