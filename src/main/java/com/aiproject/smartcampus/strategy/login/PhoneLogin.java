package com.aiproject.smartcampus.strategy.login;

import com.aiproject.smartcampus.commons.utils.PasswordEncryptionUtils;
import com.aiproject.smartcampus.exception.UserExpection;
import com.aiproject.smartcampus.mapper.ManagePersonMapper;
import com.aiproject.smartcampus.mapper.StudentMapper;
import com.aiproject.smartcampus.mapper.TeacherMapper;
import com.aiproject.smartcampus.pojo.dto.UserLoginDTO;
import com.aiproject.smartcampus.pojo.po.ManagePerson;
import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.aiproject.smartcampus.contest.CommonContest.LOGIN_KEY;
import static com.aiproject.smartcampus.contest.CommonContest.ttl;

/**
 * @program: SmartCampus
 * @description: 电话登录
 * @author: lk
 * @create: 2025-05-23 18:02
 **/

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneLogin implements LoginStrategy {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final ApplicationContext applicationContext;


    @Override
    public String login(UserLoginDTO userLoginDTO) throws ClassNotFoundException {

        if (StrUtil.isBlank(userLoginDTO.getUserPhone()) || StrUtil.isBlank(userLoginDTO.getCharacter()) || StrUtil.isBlank(userLoginDTO.getPassword())) {
            throw new UserExpection("用户信息缺失");
        }
        String logintoken = null;
        //todo 电话登录
        String character = userLoginDTO.getCharacter();
        String mapperName = character + "Mapper";
        Class<?> aClass = Class.forName(mapperName);
        Object mapper = applicationContext.getBean(aClass);
        RLock studentlock = redissonClient.getLock("student:login");
        RLock teacherlock = redissonClient.getLock("teacher:login");
        RLock managerlock = redissonClient.getLock("manager:login");
        try {
            //进行判断是属于那个角色
            if (mapper instanceof StudentMapper) {
                boolean acquired = studentlock.tryLock(5, 30, TimeUnit.SECONDS);
                if (!acquired) {
                    throw new UserExpection("系统繁忙，请稍后重试");
                }
                //执行student的登录方法
                StudentMapper studentMapper = (StudentMapper) mapper;
                //获取手机号
                String phone = userLoginDTO.getUserPhone();
                //查询是否存在账号
                LambdaQueryWrapper<Student> lambdaQueryWrapper = new LambdaQueryWrapper<>();
                lambdaQueryWrapper.eq(!StrUtil.isBlank(phone), Student::getPhone, phone);
                Student student = studentMapper.selectOne(lambdaQueryWrapper);
                if (student == null) {
                    throw new UserExpection("用户还未注册");
                }
                //判断密码是否正确
                String decryptionpassword = PasswordEncryptionUtils.decryption(student.getPassword());
                if (!decryptionpassword.equals(userLoginDTO.getPassword())) {
                    throw new UserExpection("密码错误");
                }
                String l = String.valueOf(System.currentTimeMillis());
                //创建token
                String token = phone + l + ttl;
                //将token存入redis
                stringRedisTemplate.opsForValue().set(LOGIN_KEY + "student:" + token, phone, ttl);
                logintoken = token;

            } else if (mapper instanceof TeacherMapper) {
                //执行teacher的登录方法
                boolean acquired = teacherlock.tryLock(5, 30, TimeUnit.SECONDS);
                if (!acquired) {
                    throw new UserExpection("系统繁忙，请稍后重试");
                }
                //执行student的登录方法
                TeacherMapper teacherMapper = (TeacherMapper) mapper;
                //获取手机号
                String phone = userLoginDTO.getUserPhone();
                //查询是否存在账号
                LambdaQueryWrapper<Teacher> lambdaQueryWrapper = new LambdaQueryWrapper<>();
                lambdaQueryWrapper.eq(!StrUtil.isBlank(phone), Teacher::getPhone, phone);
                Teacher teacher = teacherMapper.selectOne(lambdaQueryWrapper);
                if (teacher == null) {
                    throw new UserExpection("用户还未注册");
                }
                //判断密码是否正确
                String decryptionpassword = PasswordEncryptionUtils.decryption(teacher.getPassword());
                if (!decryptionpassword.equals(userLoginDTO.getPassword())) {
                    throw new UserExpection("密码错误");
                }
                String l = String.valueOf(System.currentTimeMillis());
                //创建token
                String token = phone + l + ttl;
                //将token存入redis
                stringRedisTemplate.opsForValue().set(LOGIN_KEY + "teacher:" + token, phone, ttl);
                logintoken = token;
            } else if (mapper instanceof ManagePersonMapper) {
                //执行managePerson的登录方法
                boolean acquired = managerlock.tryLock(5, 30, TimeUnit.SECONDS);
                if (!acquired) {
                    throw new UserExpection("系统繁忙，请稍后重试");
                }
                //执行student的登录方法
                ManagePersonMapper managePersonMapper = (ManagePersonMapper) mapper;
                //获取手机号
                String phone = userLoginDTO.getUserPhone();
                //查询是否存在账号
                LambdaQueryWrapper<ManagePerson> lambdaQueryWrapper = new LambdaQueryWrapper<>();
                lambdaQueryWrapper.eq(!StrUtil.isBlank(phone), ManagePerson::getPhone, phone);
                ManagePerson managePerson = managePersonMapper.selectOne(lambdaQueryWrapper);
                if (managePerson == null) {
                    throw new UserExpection("用户还未注册");
                }
                //判断密码是否正确
                String decryptionpassword = PasswordEncryptionUtils.decryption(managePerson.getPassword());
                if (!decryptionpassword.equals(userLoginDTO.getPassword())) {
                    throw new UserExpection("密码错误");
                }
                String l = String.valueOf(System.currentTimeMillis());
                //创建token
                String token = phone + l + ttl;
                //将token存入redis
                stringRedisTemplate.opsForValue().set(LOGIN_KEY + "manage:" + token, phone, ttl);
                logintoken = token;


            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程被中断，请重试");
        } catch (Exception e) {
            log.error("登录失败", e);
            throw new UserExpection("登录失败，请稍后重试");
        } finally {
            //释放锁
            if (studentlock != null && studentlock.isHeldByCurrentThread()) {
                studentlock.unlock();
            }
            if (teacherlock != null && teacherlock.isHeldByCurrentThread()) {
                teacherlock.unlock();
            }
            if (managerlock != null && managerlock.isHeldByCurrentThread()) {
                managerlock.unlock();
            }

        }
        return logintoken;
    }
}
