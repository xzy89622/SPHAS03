package com.sphas.project03.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动自检：验证 MySQL 和 Redis 是否可用
 */
@Slf4j
@Component
public class StartupCheck implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    public StartupCheck(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    public void run(String... args) {
        // 1) 测试MySQL（失败就让它报错，说明库也没好）
        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        log.info("MySQL OK, SELECT 1 = {}", one);

        // 2) 测试Redis（失败不阻止启动）
        try {
            redisTemplate.opsForValue().set("project03:ping", "pong"); // 写入测试key
            String val = redisTemplate.opsForValue().get("project03:ping"); // 读取测试key
            log.info("Redis OK, project03:ping = {}", val);
        } catch (Exception e) {
            log.warn("Redis 未连接（不影响启动），请确认Redis是否启动：{}", e.getMessage());
        }
    }

}

