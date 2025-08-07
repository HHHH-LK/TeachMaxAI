package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.JwtUtils;
import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import com.aiproject.smartcampus.exception.UserExpection;
import com.aiproject.smartcampus.mapper.*;
import com.aiproject.smartcampus.pojo.bo.NotificationMessage;
import com.aiproject.smartcampus.pojo.dto.LoginDTO;
import com.aiproject.smartcampus.pojo.dto.PasswordResetDTO;
import com.aiproject.smartcampus.pojo.dto.PasswordResetVerificationDTO;
import com.aiproject.smartcampus.pojo.dto.RegisterDTO;
import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.aiproject.smartcampus.pojo.po.User;
import com.aiproject.smartcampus.service.CommonService;

//import com.aiproject.smartcampus.service.EmailService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static net.sf.jsqlparser.util.validation.metadata.NamedObject.role;
import static net.sf.jsqlparser.util.validation.metadata.NamedObject.user;
import static org.apache.poi.hslf.model.textproperties.TextPropCollection.TextPropType.character;

/**
 * @program: SmartCampus
 * @description: 公用实现类
 * @author: lk
 * @create: 2025-05-20 09:21
 **/

@Service
@Slf4j
@RequiredArgsConstructor
public class CommonServiceImpl implements CommonService {
    private final UserMapper userMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final RedisTemplate<String,User> userRedisTemplate;
    private final PasswordEncoder passwordEncoder;
    //    private final EmailService emailService;
    private final String tokenKey = "token:" + "TOKEN_";

    //登录
    @Override
    @Transactional
    public Result login(LoginDTO loginDTO) {

        String loginAccount = null;

        // 根据登录类型确定登录账号
        if ("account".equals(loginDTO.getPrincipal())) {
            loginAccount = loginDTO.getUsername();
        } else if ("phone".equals(loginDTO.getPrincipal())) {
            loginAccount = loginDTO.getPhone();
        } else if ("email".equals(loginDTO.getPrincipal())) {
            loginAccount = loginDTO.getEmail();
        } else {
            return Result.error("不支持的登录类型");
        }

        // 1. 查询用户
        User user = null;
        if ("account".equals(loginDTO.getPrincipal())) {
            user = userMapper.findByUsername(loginAccount);
        } else if ("phone".equals(loginDTO.getPrincipal())) {
            user = userMapper.findByUserPhone(loginAccount);
        } else {
            user = userMapper.findByUserEmail(loginAccount);
        }

        // 2. 验证用户
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPasswordHash())) {
            System.out.println(passwordEncoder.matches(loginDTO.getPassword(), user.getPasswordHash()));
            return Result.error("密码错误");
        }

        // 4. 生成令牌
        String token = JwtUtils.generateToken(user.getUserId(), user.getUserType());
        log.info("用户 {} 登录成功，生成令牌: {}", user.getUsername(), token);

        String key = tokenKey + token;
        user.setCreatedAt(LocalDateTime.now());
        userRedisTemplate.opsForValue().set(key, user, 7, TimeUnit.DAYS);

        System.out.println(user.getUserType());
        System.out.println(user.getCreatedAt() + "time");
        return Result.success(user.getUserType().getValue() + ":" + token);


    }

    //注册
    @Override
    public Result register(RegisterDTO registerDTO) {
        //校验用户名
        if (userMapper.findByUsername(registerDTO.getUsername()) != null) {
            return Result.error("用户名已被注册");
        }
        if (!registerDTO.getPassword().equals(registerDTO.getRepassword())) {
            return Result.error("两次输入的密码不一致");
        }


        // 创建用户
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPasswordHash(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRealName("默认用户名");
        user.setUserType(User.UserType.fromValue(registerDTO.getUserType()));
        userMapper.insert(user);

        // 创建对应实体
        if ("teacher".equals(registerDTO.getUserType())) {
            Teacher teacher = new Teacher();
            teacher.setUserId(user.getUserId());
            teacherMapper.insert(teacher);
        } else if ("student".equals(registerDTO.getUserType())) {
            Student student = new Student();
            student.setUserId(user.getUserId());
            studentMapper.insert(student);
        }

        return Result.success("注册成功");

    }

    @Override
    public Result resetPassword(PasswordResetDTO dto) {
        if (userMapper.findByUsername(dto.getUsername()) == null) {
            return Result.error("用户未注册");
        }

        if (!dto.getNewPassword().equals(dto.getReNewPassword())) {
            return Result.error("两次密码输入不一致");
        }

        User user = userMapper.findByUsername(dto.getUsername());
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);

        return Result.success("密码重置成功");
    }


    @Override
    public Result logOut(String userId) {

        String token = UserLocalThreadUtils.getToken();
        UserLocalThreadUtils.removeUserInfo();
        String key = tokenKey + token;
        Boolean delete = userRedisTemplate.delete(key);
        if (delete) {
            return Result.success();
        }

        return Result.error("退出失败");

    }

//
//    @Override
//    public Result sendPasswordResetCode(PasswordResetVerificationDTO dto) {
//
//            User user = null;
//            String identifier = null;
//
//            // 根据类型确定查询方式
//            if ("phone".equals(dto.getCredential())) {
//                user = userMapper.findByUserPhone(dto.getValue());
//                identifier = dto.getValue();
//            } else if ("email".equals(dto.getCredential())) {
//                user = userMapper.findByUserEmail((dto.getValue()));
//                identifier = dto.getValue();
//            }
//
//            if (user == null) {
//                return Result.error("未找到关联账户");
//            }
//
//            // 生成6位验证码
//            String verificationCode = String.valueOf((int) (Math.random() * 900000 + 100000));
//
//            // 存储验证码到Redis（5分钟有效）
//            String redisKey = "pwd_reset:" + identifier;
//            redisTemplate.opsForValue().set(redisKey, verificationCode, 5, TimeUnit.MINUTES);
//
//            // 发送验证码
//            if ("phone".equals(dto.getCredential())) {
//                // smsService.sendVerificationCode(dto.getValue(), verificationCode);
//                System.out.println("发送短信验证码到: " + dto.getValue() + ", 验证码: " + verificationCode);
//            } else if ("email".equals(dto.getCredential())) {
//
////            emailService.sendPasswordResetEmail(dto.getValue(), verificationCode);
//                System.out.println("发送邮件验证码到: " + dto.getValue() + ", 验证码: " + verificationCode);
//            }
//
//            return Result.success("验证码已发送");
//
//
//    }
//`1s
//    @Override
//    public Result resetPassword(PasswordResetDTO dto) {
//
//            String redisKey = "pwd_reset:" + dto.getValue();
//            String storedCode = redisTemplate.opsForValue().get(redisKey);
//
//            // 验证码校验
//            if (storedCode == null || !storedCode.equals(dto.getVerifyCode())) {
//                return Result.error("验证码错误或已过期");
//            }
//
//            // 查询用户
//            User user = null;
//            if ("phone".equals(dto.getCredential())) {
//                user = userMapper.findByUserPhone(dto.getValue());
//            } else if ("email".equals(dto.getCredential())) {
//                user = userMapper.findByUserEmail(dto.getValue());
//            }
//
//            if (user == null) {
//                return Result.error("用户不存在");
//            }
//
//            // 更新密码
//            user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
//            userMapper.updateById(user);
//
//            // 清除验证码
//            redisTemplate.delete(redisKey);
//
//            return Result.success("密码重置成功");
//
//
//    }

}
