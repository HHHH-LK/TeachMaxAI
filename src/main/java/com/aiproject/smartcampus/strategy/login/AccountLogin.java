package com.aiproject.smartcampus.strategy.login;

import com.aiproject.smartcampus.commons.utils.JsonUtils;
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



import static com.aiproject.smartcampus.contest.CommonContest.*;
import static com.aiproject.smartcampus.strategy.register.StudentRegister.passwordEncoder;

/**
 * @program: SmartCampus
 * @description: 账号登录
 * @author: lk
 * @create: 2025-05-23 18:01
 **/

@Service
@Slf4j
@RequiredArgsConstructor
//todo 加锁
public class AccountLogin implements LoginStrategy {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final ApplicationContext applicationContext;
    private final String LOGIN_KEY = "smartcampus:login:";
    private final long   ttl = TOKEN_PRE_TTL * 60 * 60 * 24;


    @Override
    public String login(UserLoginDTO userLoginDTO) throws Exception {
        //获取登录的人的角色
        String character = userLoginDTO.getCharacter();
        //根据反射获取对应的mapper
        String mapperName = character + "Mapper";
        Class<?> aClass = Class.forName(MAPPER_SCAN_NAME + mapperName);
        Object mapper = applicationContext.getBean(aClass);
        //设置一个结果接收token（基于account登录）
        String token = null;
        RLock studentlock = redissonClient.getLock("student:login");
        RLock teacherlock = redissonClient.getLock("teacher:login");
        RLock managerlock = redissonClient.getLock("manager:login");
        //根据不同的角色进行加锁
        try {
            //根据角色获取对应的mapper
            if (mapper instanceof StudentMapper) {
                boolean acquired = studentlock.tryLock(5, 30, TimeUnit.SECONDS);
                if (!acquired) {
                    throw new UserExpection("系统繁忙，请稍后重试");
                }
                StudentMapper studentMapper = (StudentMapper) mapper;
                //学生登录
                String userAccount = userLoginDTO.getUserAccount();
                String password = userLoginDTO.getPassword();
                if (StrUtil.isBlank(userAccount) || StrUtil.isBlank(password)) {
                    throw new UserExpection("登录填写信息不能为空");
                }
                LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Student::getStudentAccount, userAccount);
                Student student = studentMapper.selectOne(wrapper);
                if (student == null) {
                    throw new UserExpection("该账号尚未注册");
                }
                //获取密码
                String studentPassword = student.getPassword();
                if (!passwordEncoder.decryption(studentPassword).equals(password)) {
                    throw new UserExpection("密码错误");
                }
                //生成时间戳
                String currentTimeMillis = String.valueOf(System.currentTimeMillis());
                //生成token
                token = userAccount + currentTimeMillis + ttl;
                //将token存入redis
                stringRedisTemplate.opsForValue().set(LOGIN_KEY + "student:" + token, JsonUtils.toJsonString(student), REFRESH_TIEM, TimeUnit.DAYS);

            }
            if (mapper instanceof TeacherMapper) {
                boolean acquired = studentlock.tryLock(5, 30, TimeUnit.SECONDS);
                if (!acquired) {
                    throw new UserExpection("系统繁忙，请稍后重试");
                }
                TeacherMapper teacherMapper = (TeacherMapper) mapper;
                //教师登录
                String userAccount = userLoginDTO.getUserAccount();
                String password = userLoginDTO.getPassword();
                if (StrUtil.isBlank(userAccount) || StrUtil.isBlank(password)) {
                    throw new UserExpection("登录填写信息不能为空");
                }
                LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Teacher::getAccount, userAccount);
                Teacher teacher = teacherMapper.selectOne(wrapper);
                if (teacher == null) {
                    throw new UserExpection("该账号尚未注册");
                }
                //获取密码
                String teacherPassword = teacher.getPassword();
                if (!passwordEncoder.decryption(teacherPassword).equals(password)) {
                    throw new UserExpection("密码错误");
                }
                //生成时间戳
                String currentTimeMillis = String.valueOf(System.currentTimeMillis());
                //生成token
                token = userAccount + currentTimeMillis + ttl;
                //将token存入redis
                stringRedisTemplate.opsForValue().set(LOGIN_KEY + "teacher" + token, JsonUtils.toJsonString(teacher), REFRESH_TIEM, TimeUnit.DAYS);
            }
            if (mapper instanceof ManagePersonMapper) {
                boolean acquired = studentlock.tryLock(5, 30, TimeUnit.SECONDS);
                if (!acquired) {
                    throw new UserExpection("系统繁忙，请稍后重试");
                }
                ManagePersonMapper managePersonMapper = (ManagePersonMapper) mapper;
                //管理员登录
                String userAccount = userLoginDTO.getUserAccount();
                String password = userLoginDTO.getPassword();
                if (StrUtil.isBlank(userAccount) || StrUtil.isBlank(password)) {
                    throw new UserExpection("登录填写信息不能为空");
                }
                LambdaQueryWrapper<ManagePerson> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(ManagePerson::getAccount, userAccount);
                ManagePerson managePerson = managePersonMapper.selectOne(wrapper);
                if (managePerson == null) {
                    throw new UserExpection("该账号尚未注册");
                }
                //  获取密码
                String managePersonPassword = managePerson.getPassword();
                if (!passwordEncoder.decryption(managePersonPassword).equals(password)) {
                    throw new UserExpection("密码错误");
                }
                //生成时间戳
                String currentTimeMillis = String.valueOf(System.currentTimeMillis());
                //生成token
                token = userAccount + currentTimeMillis + ttl;
                //将token存入redis
                stringRedisTemplate.opsForValue().set(LOGIN_KEY + "teacher" + token, JsonUtils.toJsonString(managePerson), REFRESH_TIEM, TimeUnit.DAYS);

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程被中断，请重试");
        } catch (Exception e) {
            log.error("管理员注册 error", e);
            throw new UserExpection("注册失败，请稍后重试");
        } finally {
            //释放锁
            if(studentlock!=null && studentlock.isHeldByCurrentThread()){
                studentlock.unlock();
            }
            if (teacherlock != null && teacherlock.isHeldByCurrentThread()) {
                teacherlock.unlock();
            }
            if (managerlock != null && managerlock.isHeldByCurrentThread()) {
                managerlock.unlock();
            }
        }

        return token;
    }
}
