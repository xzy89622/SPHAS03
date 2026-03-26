package com.sphas.project03.service;

import com.sphas.project03.entity.SysUser;

import java.util.List;

public interface AuthService {
    // 最近创建的管理员
    List<SysUser> recentAdmins(Integer limit);

    // 最近创建的 AI 健康顾问
    List<SysUser> recentAiAdvisors(Integer limit);

    Long register(String username, String password, String nickname, String phone);

    String login(String username, String password);

    // 微信快捷登录
    String wxLogin(String code, String nickname, String avatarUrl);

    // 微信手机号授权一键登录 / 自动注册
    String wxPhoneLogin(String loginCode, String phoneCode, String nickname, String avatarUrl);

    // 首次初始化管理员（无 token，但需要 initKey 且只能成功一次）
    Long bootstrapAdmin(String username, String password, String nickname, String initKey);

    // 管理员创建更多管理员
    Long createAdmin(String username, String password, String nickname, String phone);

    // 管理员创建 AI 健康顾问
    Long createAiAdvisor(String username, String password, String nickname, String phone);
}