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

import java.time.LocalDateTime;
import java.util.List;

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
    public Long register(String username, String password, String nickname, String phone) {
        checkUsernameExists(username);

        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(PasswordUtil.encode(password));
        u.setRole("USER");
        u.setNickname(nickname);
        u.setStatus(1);
        u.setCreateTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());
        fillPhone(u, phone);

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

        u.setLastLoginTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(u);

        return JwtUtil.generateToken(u.getId(), u.getRole(), secret, expireMinutes);
    }

    @Override
    public Long bootstrapAdmin(String username, String password, String nickname, String initKey) {
        if (!adminInitKey.equals(initKey)) {
            throw new BizException("initKey错误，禁止初始化管理员");
        }

        Long adminCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "ADMIN")
        );
        if (adminCount > 0) {
            throw new BizException("系统已存在管理员，禁止重复初始化");
        }

        checkUsernameExists(username);

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

    @Override
    public Long createAdmin(String username, String password, String nickname, String phone) {
        return createStaffUser(username, password, nickname, phone, "ADMIN");
    }

    @Override
    public Long createAiAdvisor(String username, String password, String nickname, String phone) {
        return createStaffUser(username, password, nickname, phone, "AI_ADVISOR");
    }

    @Override
    public List<SysUser> recentAdmins(Integer limit) {
        return recentUsersByRole("ADMIN", limit);
    }

    @Override
    public List<SysUser> recentAiAdvisors(Integer limit) {
        return recentUsersByRole("AI_ADVISOR", limit);
    }

    private Long createStaffUser(String username,
                                 String password,
                                 String nickname,
                                 String phone,
                                 String role) {
        checkUsernameExists(username);

        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(PasswordUtil.encode(password));
        u.setRole(role);
        u.setNickname(nickname);
        u.setStatus(1);
        u.setCreateTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());
        fillPhone(u, phone);

        sysUserMapper.insert(u);
        return u.getId();
    }

    private List<SysUser> recentUsersByRole(String role, Integer limit) {
        int n = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        return sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, role)
                        .orderByDesc(SysUser::getCreateTime)
                        .last("limit " + n)
        );
    }

    private void checkUsernameExists(String username) {
        SysUser exist = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (exist != null) {
            throw new BizException("账号已存在");
        }
    }

    private void fillPhone(SysUser user, String phone) {
        if (phone == null) {
            return;
        }
        String p = phone.trim();
        if (!p.isEmpty()) {
            user.setPhone(p);
        }
    }
}