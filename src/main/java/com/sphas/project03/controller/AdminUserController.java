package com.sphas.project03.controller;

import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.CreateAdminDTO;
import com.sphas.project03.service.AuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 管理员用户管理接口
 */
@RestController
@RequestMapping("/api/admin/users")
@Validated
public class AdminUserController extends BaseController {

    private final AuthService authService;

    public AdminUserController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 管理员创建新管理员
     */
    @PostMapping("/create-admin")
    public R<Long> createAdmin(@RequestBody @Valid CreateAdminDTO dto, HttpServletRequest request) {

        // ✅ 必须ADMIN
        requireAdmin(request);

        Long id = authService.createAdmin(dto.getUsername(), dto.getPassword(), dto.getNickname());
        return R.ok(id);
    }
}

