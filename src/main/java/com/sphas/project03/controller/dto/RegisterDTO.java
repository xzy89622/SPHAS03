package com.sphas.project03.controller.dto;

import javax.validation.constraints.NotBlank;

/**
 * 注册入参
 */
public class RegisterDTO {

    @NotBlank
    private String username; // 账号

    @NotBlank
    private String password; // 密码

    private String nickname; // 昵称

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
