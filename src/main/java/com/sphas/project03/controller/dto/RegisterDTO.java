package com.sphas.project03.controller.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 注册入参
 */
public class RegisterDTO {

    @NotBlank
    private String username; // 账号

    @NotBlank
    private String password; // 密码

    private String nickname; // 昵称

    // ✅ 新增：手机号（可选）
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
