package com.sphas.project03.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具：生成token/解析token
 */
public class JwtUtil {

    public static String generateToken(Long userId, String role, String secret, int expireMinutes) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMinutes * 60L * 1000L);

        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // 用户ID
                .claim("role", role)                // 角色
                .setIssuedAt(now)                   // 签发时间
                .setExpiration(exp)                 // 过期时间
                .signWith(SignatureAlgorithm.HS256, secret.getBytes(StandardCharsets.UTF_8)) // 用字节签名
                .compact();
    }

    public static Claims parseToken(String token, String secret) {
        return Jwts.parser()
                .setSigningKey(secret.getBytes(StandardCharsets.UTF_8)) // 用同样的字节密钥解析
                .parseClaimsJws(token)
                .getBody();
    }
}
