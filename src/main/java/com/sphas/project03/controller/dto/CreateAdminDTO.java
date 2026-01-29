package com.sphas.project03.controller.dto;

import javax.validation.constraints.NotBlank;

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

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}

