package com.sphas.project03.controller.dto;

import javax.validation.constraints.NotBlank;

/**
 * 登录入参
 */
public class LoginDTO {

    @NotBlank
    private String username; // 账号

    @NotBlank
    private String password; // 密码

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

