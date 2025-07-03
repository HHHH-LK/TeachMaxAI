package com.aiproject.smartcampus.commons.utils;
import com.aiproject.smartcampus.pojo.po.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
/**
 * @program: SmartCampus
 * @description: jwt解析工具
 * @author: lk
 * @create: 2025-05-17 17:15
 **/

public class JwtUtils {

    private static final String SECRET_KEY = "your-strong-secret-key-here";
    private static final long EXPIRATION_MS = 86400000; // 24小时

    public static String generateToken(Integer userId, User.UserType role) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
                .compact();
    }


}
