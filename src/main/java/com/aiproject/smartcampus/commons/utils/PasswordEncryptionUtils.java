package com.aiproject.smartcampus.commons.utils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;

/**
 * @program: SmartCampus
 * @description: 密码加密和解密（AES），固定密钥因子，实现单参数加解密
 * @author: lk
 * @create: 2025-05-01 22:06
 **/
public class PasswordEncryptionUtils {

    // 固定密码因子（派生AES密钥使用）
    private static final String SECRET_FACTOR = "lk2025FixedFactor!"; // 16+ 字节字符串

    // 派生 AES-128 密钥
    private static SecretKeySpec deriveKey() throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha256.digest(SECRET_FACTOR.getBytes(StandardCharsets.UTF_8));
        byte[] keyBytes = Arrays.copyOf(hash, 16); // 16字节 = 128bit
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 加密：随机 IV（16字节），密文前16字节存 IV，剩余为实际密文
     * @param plainText 要加密的明文密码
     * @return Base64编码的IV+密文
     */
    public static String encryption(String plainText) {
        try {
            SecretKeySpec keySpec = deriveKey();

            byte[] iv = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            byte[] ivAndCipher = buffer.array();

            return Base64.getEncoder().encodeToString(ivAndCipher);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密：Base64解码后，前16字节为IV，后续为实际密文
     * @param base64IvAndCipher Base64编码的IV+密文
     * @return 解密后的明文密码
     */
    public static String decryption(String base64IvAndCipher) {
        try {
            SecretKeySpec keySpec = deriveKey();

            byte[] ivAndCipher = Base64.getDecoder().decode(base64IvAndCipher);
            ByteBuffer buffer = ByteBuffer.wrap(ivAndCipher);
            byte[] iv = new byte[16];
            buffer.get(iv);
            byte[] cipherBytes = new byte[buffer.remaining()];
            buffer.get(cipherBytes);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decrypted = cipher.doFinal(cipherBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }
}
