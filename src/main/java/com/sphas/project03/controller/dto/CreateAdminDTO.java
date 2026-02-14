package com.sphas.project03.controller.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 管理员创建管理员 DTO（需要 ADMIN token）
 */
public class CreateAdminDTO {

    @NotBlank(message = "username不能为空")
    private String username;

    @NotBlank(message = "password不能为空")
    private String password;

    @NotBlank(message = "nickname不能为空")
    private String nickname;

    /**
     * ✅ 新增：手机号（可选）
     * 允许空；不为空时校验 11 位手机号（简单校验）
     */
    @Pattern(
            regexp = "^$|^1\\d{10}$",
            message = "phone格式不正确（需11位手机号）"
    )
    private String phone;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
