package com.aiproject.smartcampus.commons.utils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * @program: MedicalAgent
 * @description: 密码加密和解密（AES）
 * @author: lk
 * @create: 2025-05-01 22:06
 **/

//基于AES加密算法实现
public class PasswordEncryptionUtils {

    //加密
    public static String encryption(String password) throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException {
        // 生成一个随机的初始化向量（IV）
        byte[] iv = new byte[16]; // 128位的IV
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        // 创建密钥
        SecretKey secretKey = new javax.crypto.spec.SecretKeySpec("1".getBytes(), "AES");
        // 初始化Cipher对象，指定加密模式为AES CBC
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
        // 执行加密
        byte[] encryptedBytes = cipher.doFinal(password.getBytes());
        // 将加密结果转为Base64字符串返回
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    //解密
    public static String decryption(String password) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        // 生成一个随机的初始化向量（IV）
        byte[] iv = new byte[16]; // 128位的IV
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        // 创建密钥
        SecretKey secretKey = new javax.crypto.spec.SecretKeySpec("1".getBytes(), "AES");
        // 初始化Cipher对象，指定解密模式为AES CBC
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
        // 进行Base64解码，并解密
        byte[] decodedBytes = Base64.getDecoder().decode(password);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        // 将解密后的字节数组转换为字符串并返回
        return new String(decryptedBytes);


    }


}
