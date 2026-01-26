package com.sphas.project03.controller;

import com.sphas.project03.common.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 测试接口
 */
@RestController
public class PingController {

    @GetMapping("/ping")
    public R<String> ping() {
        return R.ok("pong");
    }

    @GetMapping("/api/secure/ping")
    public R<String> securePing(HttpServletRequest request) {
        String userId = String.valueOf(request.getAttribute("userId"));
        String role = String.valueOf(request.getAttribute("role"));
        return R.ok("secure pong, userId=" + userId + ", role=" + role);
    }
}
