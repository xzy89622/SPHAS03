package com.sphas.project03.controller;

import com.sphas.project03.common.R;
import com.sphas.project03.controller.dto.CreateAdminDTO;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.service.AuthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

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
        requireAdmin(request);
        Long id = authService.createAdmin(
                dto.getUsername(),
                dto.getPassword(),
                dto.getNickname(),
                dto.getPhone()
        );
        return R.ok(id);
    }

    /**
     * 管理员创建 AI 健康顾问
     */
    @PostMapping("/create-ai-advisor")
    public R<Long> createAiAdvisor(@RequestBody @Valid CreateAdminDTO dto, HttpServletRequest request) {
        requireAdmin(request);
        Long id = authService.createAiAdvisor(
                dto.getUsername(),
                dto.getPassword(),
                dto.getNickname(),
                dto.getPhone()
        );
        return R.ok(id);
    }

    /**
     * 最近创建的管理员
     */
    @GetMapping("/recent")
    public R<List<SysUser>> recentAdmins(@RequestParam(defaultValue = "10") Integer limit,
                                         HttpServletRequest request) {
        requireAdmin(request);
        return R.ok(authService.recentAdmins(limit));
    }

    /**
     * 最近创建的 AI 健康顾问
     */
    @GetMapping("/recent-ai-advisors")
    public R<List<SysUser>> recentAiAdvisors(@RequestParam(defaultValue = "10") Integer limit,
                                             HttpServletRequest request) {
        requireAdmin(request);
        return R.ok(authService.recentAiAdvisors(limit));
    }
}