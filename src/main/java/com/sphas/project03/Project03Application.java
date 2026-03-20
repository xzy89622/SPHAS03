package com.sphas.project03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 项目启动类
 */
@EnableScheduling
@SpringBootApplication
public class Project03Application {

    public static void main(String[] args) {
        SpringApplication.run(Project03Application.class, args);
    }
}