package com.sphas.project03.controller.dto;

import javax.validation.constraints.NotBlank;

/**
 * 微信快捷登录入参
 */
public class WxLoginDTO {

    @NotBlank
    private String code;

    private String nickname;

    private String avatarUrl;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}