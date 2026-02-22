package com.sphas.project03.controller;

import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.service.SysUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 用户信息（给小程序“我的/个人中心”用）
 * 说明：不返回 password，避免泄露
 */
@RestController
@RequestMapping("/api/user")
public class UserController extends BaseController {

    private final SysUserService sysUserService;

    public UserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    /**
     * 获取当前登录用户信息
     * GET /api/user/profile
     */
    @GetMapping("/profile")
    public R<UserProfileVO> profile(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) throw new BizException("未登录");

        SysUser u = sysUserService.getById(userId);
        if (u == null) throw new BizException("用户不存在");

        UserProfileVO vo = new UserProfileVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setRole(u.getRole());
        vo.setNickname(u.getNickname());
        vo.setPhone(u.getPhone());
        vo.setStatus(u.getStatus());
        vo.setLastLoginTime(u.getLastLoginTime());
        vo.setCreateTime(u.getCreateTime());
        return R.ok(vo);
    }

    /**
     * 简单VO：只放小程序需要展示的字段
     */
    public static class UserProfileVO {
        private Long id;
        private String username;
        private String role;
        private String nickname;
        private String phone;
        private Integer status;
        private LocalDateTime lastLoginTime;
        private LocalDateTime createTime;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }

        public LocalDateTime getLastLoginTime() { return lastLoginTime; }
        public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }

        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    }
}