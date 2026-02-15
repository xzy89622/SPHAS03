package com.sphas.project03.service;

import com.sphas.project03.entity.SysUser;

import java.util.List;

public interface AuthService {
    List<SysUser> recentAdmins(Integer limit);

    Long register(String username, String password, String nickname, String phone);


    String login(String username, String password);
    // ✅ 首次初始化管理员（无token，但需要initKey且只能成功一次）
    Long bootstrapAdmin(String username, String password, String nickname, String initKey);

    // ✅ 管理员创建更多管理员（需要ADMIN权限，Controller里校验）
    Long createAdmin(String username, String password, String nickname, String phone);
}
//xiugaichenggong

