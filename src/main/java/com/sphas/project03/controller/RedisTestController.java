package com.sphas.project03.controller;

import com.sphas.project03.common.R;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Redis连通性测试
 * 说明：只用于开发自测，后面你要删也行
 */
@RestController
@RequestMapping("/api/dev/redis")
public class RedisTestController {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisTestController(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @GetMapping("/ping")
    public R<String> ping() {
        // 写入一个key，再读出来
        String key = "project03:dev:ping";
        String val = "pong@" + LocalDateTime.now();
        stringRedisTemplate.opsForValue().set(key, val);

        String got = stringRedisTemplate.opsForValue().get(key);
        return R.ok(got);
    }
}
