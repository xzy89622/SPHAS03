package com.sphas.project03.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具：加密/校验
 */
public class PasswordUtil {

    private static final BCryptPasswordEncoder ENC = new BCryptPasswordEncoder();

    public static String encode(String raw) {
        return ENC.encode(raw); // 明文加密
    }

    public static boolean matches(String raw, String encoded) {
        return ENC.matches(raw, encoded); // 校验密码
    }
}
