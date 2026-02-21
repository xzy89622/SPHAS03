package com.sphas.project03.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 隐私工具：
 * 1) AES 加密/解密（用于敏感字段落库）
 * 2) 模拟区块链哈希（链式指纹）
 */
public class PrivacyUtil {

    // 16位密钥（AES-128）
    private static final String SECRET_KEY = "sphas_security_k";

    /**
     * 模拟区块链哈希（生成数据指纹）
     * 注意：这里是“模拟上链”，用 prevHash 串起来形成链式结构，方便答辩讲“不可篡改 + 可追溯”。
     */
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

    /**
     * 敏感数据加密（AES）
     */
    public static String encrypt(String content) {
        if (content == null) return null;
        try {
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // 兜底：加密失败就存原文，避免主流程挂掉
            return content;
        }
    }

    /**
     * 敏感数据解密（AES）
     * 说明：历史数据里可能有未加密的 JSON（老数据/测试数据），解密失败就直接返回原文。
     */
    public static String decrypt(String encrypted) {
        if (encrypted == null) return null;
        try {
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 兜底：不是合法密文/解密失败，按原文返回
            return encrypted;
        }
    }
}
