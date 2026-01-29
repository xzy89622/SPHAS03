package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.mapper.SysUserMapper;
import com.sphas.project03.service.AuthService;
import com.sphas.project03.utils.JwtUtil;
import com.sphas.project03.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;

/**
 * 登录注册业务
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expireMinutes}")
    private int expireMinutes;

    @Value("${app.adminInitKey}")
    private String adminInitKey;


    public AuthServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public Long register(String username, String password, String nickname) {
        // 查重
        SysUser exist = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (exist != null) {
            throw new BizException("账号已存在");
        }

        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(PasswordUtil.encode(password)); // 密码加密
        u.setRole("USER"); // 默认普通用户
        u.setNickname(nickname);
        u.setStatus(1);

        sysUserMapper.insert(u);
        return u.getId();
    }

    @Override
    public String login(String username, String password) {
        SysUser u = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (u == null) {
            throw new BizException("账号或密码错误");
        }
        if (u.getStatus() != null && u.getStatus() == 0) {
            throw new BizException("账号已被禁用");
        }
        if (!PasswordUtil.matches(password, u.getPassword())) {
            throw new BizException("账号或密码错误");
        }

        u.setLastLoginTime(LocalDateTime.now()); // 记录登录时间
        sysUserMapper.updateById(u);

        // 生成JWT token
        return JwtUtil.generateToken(u.getId(), u.getRole(), secret, expireMinutes);
    }
    @Override
    public Long bootstrapAdmin(String username, String password, String nickname, String initKey) {

        // 1) 校验 initKey
        if (!adminInitKey.equals(initKey)) {
            throw new BizException("initKey错误，禁止初始化管理员");
        }

        // 2) 如果系统里已经存在 ADMIN，则禁止再次初始化
        Long adminCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "ADMIN")
        );

        if (adminCount > 0) {
            throw new BizException("系统已存在管理员，禁止重复初始化");
        }

        // 3) 用户名查重
        SysUser exist = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (exist != null) {
            throw new BizException("账号已存在");
        }

        // 4) 创建管理员
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(PasswordUtil.encode(password));
        u.setRole("ADMIN");           // ✅ 管理员角色
        u.setNickname(nickname);
        u.setStatus(1);
        u.setCreateTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());

        sysUserMapper.insert(u);
        return u.getId();
    }

    @Override
    public Long createAdmin(String username, String password, String nickname) {

        // 1) 用户名查重
        SysUser exist = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (exist != null) {
            throw new BizException("账号已存在");
        }

        // 2) 创建管理员
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(PasswordUtil.encode(password));
        u.setRole("ADMIN");
        u.setNickname(nickname);
        u.setStatus(1);
        u.setCreateTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());

        sysUserMapper.insert(u);
        return u.getId();
    }

}
