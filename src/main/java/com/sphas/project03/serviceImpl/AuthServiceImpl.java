package com.sphas.project03.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sphas.project03.common.BizException;
import com.sphas.project03.entity.SysUser;
import com.sphas.project03.mapper.SysUserMapper;
import com.sphas.project03.service.AuthService;
import com.sphas.project03.utils.JwtUtil;
import com.sphas.project03.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 登录注册业务
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expireMinutes}")
    private int expireMinutes;

    @Value("${app.adminInitKey}")
    private String adminInitKey;

    @Value("${wechat.miniapp.appid:}")
    private String wechatAppid;

    @Value("${wechat.miniapp.secret:}")
    private String wechatSecret;

    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           RestTemplate restTemplate,
                           ObjectMapper objectMapper) {
        this.sysUserMapper = sysUserMapper;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Long register(String username, String password, String nickname, String phone) {
        String finalUsername = normalize(username);
        String finalPhone = normalize(phone);
        String finalNickname = normalize(nickname);

        checkUsernameExists(finalUsername);
        checkPhoneExists(finalPhone);

        SysUser u = new SysUser();
        u.setUsername(finalUsername);
        u.setPassword(PasswordUtil.encode(password));
        u.setRole("USER");
        u.setNickname(finalNickname);
        u.setStatus(1);
        u.setCreateTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());
        fillPhone(u, finalPhone);

        sysUserMapper.insert(u);
        return u.getId();
    }

    @Override
    public String login(String username, String password) {
        String account = normalize(username);

        SysUser u = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .and(wrapper -> wrapper
                                .eq(SysUser::getUsername, account)
                                .or()
                                .eq(SysUser::getPhone, account))
                        .last("limit 1")
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

        touchLoginInfo(u);
        return JwtUtil.generateToken(u.getId(), u.getRole(), secret, expireMinutes);
    }

    @Override
    public String wxLogin(String code, String nickname, String avatarUrl) {
        String finalCode = normalize(code);
        if (finalCode == null) {
            throw new BizException("微信登录凭证不能为空");
        }

        Map<String, Object> wxResult = requestWxSession(finalCode);

        String openid = text(wxResult.get("openid"));
        String unionid = text(wxResult.get("unionid"));

        if (openid == null) {
            throw new BizException("微信登录失败：未获取到openid");
        }

        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getWxOpenid, openid)
                        .last("limit 1")
        );

        String finalNickname = normalize(nickname);
        String finalAvatarUrl = normalize(avatarUrl);

        if (user == null) {
            user = new SysUser();
            user.setUsername(buildWxUsername());
            user.setPassword(PasswordUtil.encode(UUID.randomUUID().toString()));
            user.setRole("USER");
            user.setNickname(finalNickname != null ? finalNickname : "微信用户");
            user.setWxOpenid(openid);
            user.setWxUnionid(unionid);
            user.setAvatarUrl(finalAvatarUrl);
            user.setStatus(1);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            user.setLastLoginTime(LocalDateTime.now());
            sysUserMapper.insert(user);
        } else {
            if (user.getStatus() != null && user.getStatus() == 0) {
                throw new BizException("账号已被禁用");
            }

            if (finalNickname != null) {
                user.setNickname(finalNickname);
            }
            if (finalAvatarUrl != null) {
                user.setAvatarUrl(finalAvatarUrl);
            }
            if (unionid != null && isBlank(user.getWxUnionid())) {
                user.setWxUnionid(unionid);
            }

            touchLoginInfo(user);
        }

        return JwtUtil.generateToken(user.getId(), user.getRole(), secret, expireMinutes);
    }

    @Override
    public String wxPhoneLogin(String loginCode, String phoneCode, String nickname, String avatarUrl) {
        String finalLoginCode = normalize(loginCode);
        String finalPhoneCode = normalize(phoneCode);

        if (finalLoginCode == null) {
            throw new BizException("loginCode不能为空");
        }
        if (finalPhoneCode == null) {
            throw new BizException("phoneCode不能为空");
        }

        Map<String, Object> sessionResult = requestWxSession(finalLoginCode);
        String openid = text(sessionResult.get("openid"));
        String unionid = text(sessionResult.get("unionid"));

        if (openid == null) {
            throw new BizException("微信登录失败：未获取到openid");
        }

        String accessToken = requestWxAccessToken();
        String phoneNumber = requestWxPhoneNumber(accessToken, finalPhoneCode);

        if (phoneNumber == null) {
            throw new BizException("获取手机号失败");
        }

        String finalNickname = normalize(nickname);
        String finalAvatarUrl = normalize(avatarUrl);

        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getPhone, phoneNumber)
                        .last("limit 1")
        );

        if (user == null) {
            user = new SysUser();
            user.setUsername(phoneNumber);
            user.setPassword(PasswordUtil.encode(UUID.randomUUID().toString()));
            user.setRole("USER");
            user.setNickname(finalNickname != null ? finalNickname : "微信用户");
            user.setPhone(phoneNumber);
            user.setWxOpenid(openid);
            user.setWxUnionid(unionid);
            user.setAvatarUrl(finalAvatarUrl);
            user.setStatus(1);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            user.setLastLoginTime(LocalDateTime.now());
            sysUserMapper.insert(user);
            return JwtUtil.generateToken(user.getId(), user.getRole(), secret, expireMinutes);
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被禁用");
        }

        if (!isBlank(user.getWxOpenid()) && !openid.equals(user.getWxOpenid())) {
            throw new BizException("该手机号已绑定其他微信账号");
        }

        user.setPhone(phoneNumber);

        if (isBlank(user.getWxOpenid())) {
            user.setWxOpenid(openid);
        }
        if (unionid != null && isBlank(user.getWxUnionid())) {
            user.setWxUnionid(unionid);
        }
        if (finalNickname != null) {
            user.setNickname(finalNickname);
        }
        if (finalAvatarUrl != null) {
            user.setAvatarUrl(finalAvatarUrl);
        }

        touchLoginInfo(user);
        return JwtUtil.generateToken(user.getId(), user.getRole(), secret, expireMinutes);
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

        String finalUsername = normalize(username);
        String finalNickname = normalize(nickname);

        checkUsernameExists(finalUsername);

        SysUser u = new SysUser();
        u.setUsername(finalUsername);
        u.setPassword(PasswordUtil.encode(password));
        u.setRole("ADMIN");
        u.setNickname(finalNickname);
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
        String finalUsername = normalize(username);
        String finalPhone = normalize(phone);
        String finalNickname = normalize(nickname);

        checkUsernameExists(finalUsername);
        checkPhoneExists(finalPhone);

        SysUser u = new SysUser();
        u.setUsername(finalUsername);
        u.setPassword(PasswordUtil.encode(password));
        u.setRole(role);
        u.setNickname(finalNickname);
        u.setStatus(1);
        u.setCreateTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());
        fillPhone(u, finalPhone);

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
        if (isBlank(username)) {
            throw new BizException("账号不能为空");
        }

        SysUser exist = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .last("limit 1")
        );
        if (exist != null) {
            throw new BizException("账号已存在");
        }
    }

    private void checkPhoneExists(String phone) {
        if (phone == null || phone.isEmpty()) {
            return;
        }

        SysUser exist = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getPhone, phone)
                        .last("limit 1")
        );
        if (exist != null) {
            throw new BizException("手机号已存在");
        }
    }

    private void fillPhone(SysUser user, String phone) {
        if (phone == null || phone.isEmpty()) {
            return;
        }
        user.setPhone(phone);
    }

    private void touchLoginInfo(SysUser user) {
        user.setLastLoginTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    private Map<String, Object> requestWxSession(String code) {
        ensureWechatConfig();

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api.weixin.qq.com/sns/jscode2session")
                    .queryParam("appid", wechatAppid)
                    .queryParam("secret", wechatSecret)
                    .queryParam("js_code", code)
                    .queryParam("grant_type", "authorization_code")
                    .toUriString();

            String body = restTemplate.getForObject(url, String.class);
            Map<String, Object> result = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});

            checkWechatError(result, "微信登录失败");
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("微信登录失败，请稍后重试");
        }
    }

    private String requestWxAccessToken() {
        ensureWechatConfig();

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api.weixin.qq.com/cgi-bin/token")
                    .queryParam("grant_type", "client_credential")
                    .queryParam("appid", wechatAppid)
                    .queryParam("secret", wechatSecret)
                    .toUriString();

            String body = restTemplate.getForObject(url, String.class);
            Map<String, Object> result = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});

            checkWechatError(result, "获取微信access_token失败");

            String accessToken = text(result.get("access_token"));
            if (accessToken == null) {
                throw new BizException("获取微信access_token失败");
            }
            return accessToken;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("获取微信access_token失败");
        }
    }

    private String requestWxPhoneNumber(String accessToken, String phoneCode) {
        try {
            String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + accessToken;

            Map<String, Object> payload = new HashMap<>();
            payload.put("code", phoneCode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            Map<String, Object> result = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<Map<String, Object>>() {}
            );

            checkWechatError(result, "获取手机号失败");

            Object phoneInfoObj = result.get("phone_info");
            if (!(phoneInfoObj instanceof Map)) {
                throw new BizException("获取手机号失败");
            }

            Map<?, ?> phoneInfo = (Map<?, ?>) phoneInfoObj;
            String phoneNumber = text(phoneInfo.get("phoneNumber"));
            if (phoneNumber == null) {
                throw new BizException("获取手机号失败");
            }
            return phoneNumber;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("获取手机号失败，请稍后重试");
        }
    }

    private void checkWechatError(Map<String, Object> result, String defaultMsg) {
        Object errcode = result.get("errcode");
        String errMsg = text(result.get("errmsg"));

        if (errcode == null) {
            return;
        }

        String codeText = String.valueOf(errcode);
        if (!"0".equals(codeText)) {
            throw new BizException(defaultMsg + "：" + (errMsg != null ? errMsg : codeText));
        }
    }

    private void ensureWechatConfig() {
        if (isBlank(wechatAppid) || isBlank(wechatSecret)) {
            throw new BizException("请先在 application.yml 配置 wechat.miniapp.appid 和 wechat.miniapp.secret");
        }
    }

    private String buildWxUsername() {
        return "wx_" + System.currentTimeMillis();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}