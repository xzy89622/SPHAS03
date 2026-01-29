package com.sphas.project03.controller.dto;

import javax.validation.constraints.NotBlank;

/**
 * 首次初始化管理员 DTO（仅允许创建一次）
 */
public class BootstrapAdminDTO {

    @NotBlank(message = "username不能为空")
    private String username;

    @NotBlank(message = "password不能为空")
    private String password;

    @NotBlank(message = "nickname不能为空")
    private String nickname;

    @NotBlank(message = "initKey不能为空")
    private String initKey;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getInitKey() { return initKey; }
    public void setInitKey(String initKey) { this.initKey = initKey; }
}
