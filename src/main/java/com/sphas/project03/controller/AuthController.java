package com.sphas.project03.controller;

import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.LoginDTO;
import com.sphas.project03.controller.dto.RegisterDTO;
import com.sphas.project03.service.AuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.sphas.project03.controller.dto.BootstrapAdminDTO;

import javax.validation.Valid;

/**
 * 登录注册接口
 */
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public R<Long> register(@RequestBody @Valid RegisterDTO dto) {
        Long id = authService.register(dto.getUsername(), dto.getPassword(), dto.getNickname());
        return R.ok(id); // 返回用户ID
    }

    @PostMapping("/login")
    public R<String> login(@RequestBody @Valid LoginDTO dto) {
        String token = authService.login(dto.getUsername(), dto.getPassword());
        return R.ok(token); // 返回JWT
    }
    @PostMapping("/bootstrap-admin")
    public R<Long> bootstrapAdmin(@RequestBody @Valid BootstrapAdminDTO dto) {
        Long id = authService.bootstrapAdmin(dto.getUsername(), dto.getPassword(), dto.getNickname(), dto.getInitKey());
        return R.ok(id);
    }

}

