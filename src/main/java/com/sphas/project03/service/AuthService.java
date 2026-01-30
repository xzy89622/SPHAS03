package com.sphas.project03.service;

public interface AuthService {

    Long register(String username, String password, String nickname);

    String login(String username, String password);
    // ✅ 首次初始化管理员（无token，但需要initKey且只能成功一次）
    Long bootstrapAdmin(String username, String password, String nickname, String initKey);

    // ✅ 管理员创建更多管理员（需要ADMIN权限，Controller里校验）
    Long createAdmin(String username, String password, String nickname);
}
//xiugaichenggong

