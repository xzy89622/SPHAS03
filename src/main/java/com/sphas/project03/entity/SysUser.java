package com.sphas.project03.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 用户表实体
 */
@TableName("sys_user")
public class SysUser {

    private Long id;                 // 主键
    private String username;         // 账号
    private String password;         // 密码(加密后)
    private String role;             // 角色(USER/ADMIN/AI_ADVISOR)

    private String nickname;         // 昵称
    private String phone;            // 手机号
    private Integer status;          // 1正常 0禁用

    private LocalDateTime lastLoginTime; // 最后登录时间
    private LocalDateTime createTime;    // 创建时间
    private LocalDateTime updateTime;    // 更新时间

    // ===== getter/setter（不依赖Lombok，最稳） =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
