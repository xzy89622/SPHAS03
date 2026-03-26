package com.sphas.project03.controller.dto;

import javax.validation.constraints.NotBlank;

/**
 * 微信手机号授权登录入参
 */
public class WxPhoneLoginDTO {

    @NotBlank
    private String loginCode;

    @NotBlank
    private String phoneCode;

    private String nickname;

    private String avatarUrl;

    public String getLoginCode() {
        return loginCode;
    }

    public void setLoginCode(String loginCode) {
        this.loginCode = loginCode;
    }

    public String getPhoneCode() {
        return phoneCode;
    }

    public void setPhoneCode(String phoneCode) {
        this.phoneCode = phoneCode;
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