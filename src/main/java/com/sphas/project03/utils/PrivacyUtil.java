package com.sphas.project03.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class PrivacyUtil {

    private static final String SECRET_KEY = "sphas_security_k"; // 16位密钥

    // 1. 模拟区块链哈希（生成数据指纹）
    public static String calculateBlockHash(Long userId, int score, String reason, String prevHash) {
        String raw = userId + "|" + score + "|" + reason + "|" + prevHash + "|" + System.currentTimeMillis();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "HASH_ERROR";
        }
    }

    // 2. 敏感数据加密（AES）
    public static String encrypt(String content) {
        try {
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(content.getBytes()));
        } catch (Exception e) {
            return content; // 兜底：加密失败存原文
        }
    }
}
