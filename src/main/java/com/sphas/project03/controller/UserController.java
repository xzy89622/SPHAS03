package com.sphas.project03.controller;

import com.sphas.project03.common.BizException;
import com.sphas.project03.common.R;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.service.SysUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户信息接口
 * 给小程序“我的 / 编辑资料”页面用
 */
@RestController
@RequestMapping("/api/user")
public class UserController extends BaseController {

    private final SysUserService sysUserService;

    public UserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    /**
     * 获取当前登录用户资料
     */
    @GetMapping("/profile")
    public R<UserProfileVO> profile(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            throw new BizException("未登录");
        }

        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }

        return R.ok(toVO(user));
    }

    /**
     * 更新当前登录用户资料
     */
    @PutMapping("/profile")
    public R<UserProfileVO> updateProfile(@RequestBody UpdateProfileDTO dto, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            throw new BizException("未登录");
        }

        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }

        String nickname = dto.getNickname() == null ? "" : dto.getNickname().trim();
        String phone = dto.getPhone() == null ? "" : dto.getPhone().trim();
        String gender = dto.getGender() == null ? "" : dto.getGender().trim();

        // 昵称长度控制一下
        if (nickname.length() > 50) {
            throw new BizException("昵称长度不能超过50");
        }

        // 手机号可以为空，填了就校验
        if (!phone.isEmpty() && !phone.matches("^1\\d{10}$")) {
            throw new BizException("手机号格式不正确");
        }

        // 年龄校验
        if (dto.getAge() != null) {
            if (dto.getAge() < 1 || dto.getAge() > 120) {
                throw new BizException("年龄范围不正确");
            }
        }

        // 性别校验
        if (!gender.isEmpty()
                && !("男".equals(gender) || "女".equals(gender) || "其它".equals(gender))) {
            throw new BizException("性别只能填写男、女或其它");
        }

        // 身高校验
        if (dto.getHeightCm() != null) {
            if (dto.getHeightCm().compareTo(new BigDecimal("50")) < 0
                    || dto.getHeightCm().compareTo(new BigDecimal("250")) > 0) {
                throw new BizException("身高范围不正确");
            }
        }

        // 初始体重校验
        if (dto.getInitialWeightKg() != null) {
            if (dto.getInitialWeightKg().compareTo(new BigDecimal("10")) < 0
                    || dto.getInitialWeightKg().compareTo(new BigDecimal("500")) > 0) {
                throw new BizException("初始体重范围不正确");
            }
        }

        user.setNickname(nickname.isEmpty() ? null : nickname);
        user.setPhone(phone.isEmpty() ? null : phone);
        user.setAge(dto.getAge());
        user.setGender(gender.isEmpty() ? null : gender);
        user.setHeightCm(dto.getHeightCm());
        user.setInitialWeightKg(dto.getInitialWeightKg());

        boolean ok = sysUserService.updateById(user);
        if (!ok) {
            throw new BizException("保存失败");
        }

        SysUser latest = sysUserService.getById(userId);
        return R.ok(toVO(latest));
    }

    /**
     * 实体转返回对象
     */
    private UserProfileVO toVO(SysUser user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setAge(user.getAge());
        vo.setGender(user.getGender());
        vo.setHeightCm(user.getHeightCm());
        vo.setInitialWeightKg(user.getInitialWeightKg());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /**
     * 更新资料请求对象
     */
    public static class UpdateProfileDTO {
        private String nickname;
        private String phone;
        private Integer age;
        private String gender;
        private BigDecimal heightCm;
        private BigDecimal initialWeightKg;

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public BigDecimal getHeightCm() {
            return heightCm;
        }

        public void setHeightCm(BigDecimal heightCm) {
            this.heightCm = heightCm;
        }

        public BigDecimal getInitialWeightKg() {
            return initialWeightKg;
        }

        public void setInitialWeightKg(BigDecimal initialWeightKg) {
            this.initialWeightKg = initialWeightKg;
        }
    }

    /**
     * 返回给前端的资料对象
     */
    public static class UserProfileVO {
        private Long id;
        private String username;
        private String role;
        private String nickname;
        private String phone;
        private Integer age;
        private String gender;
        private BigDecimal heightCm;
        private BigDecimal initialWeightKg;
        private Integer status;
        private LocalDateTime lastLoginTime;
        private LocalDateTime createTime;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public BigDecimal getHeightCm() {
            return heightCm;
        }

        public void setHeightCm(BigDecimal heightCm) {
            this.heightCm = heightCm;
        }

        public BigDecimal getInitialWeightKg() {
            return initialWeightKg;
        }

        public void setInitialWeightKg(BigDecimal initialWeightKg) {
            this.initialWeightKg = initialWeightKg;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public LocalDateTime getLastLoginTime() {
            return lastLoginTime;
        }

        public void setLastLoginTime(LocalDateTime lastLoginTime) {
            this.lastLoginTime = lastLoginTime;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }
    }
}