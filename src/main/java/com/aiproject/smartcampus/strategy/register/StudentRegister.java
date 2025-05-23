package com.aiproject.smartcampus.strategy.register;

import com.aiproject.smartcampus.commons.utils.JsonUtils;
import com.aiproject.smartcampus.commons.utils.PasswordEncryptionUtils;
import com.aiproject.smartcampus.commons.utils.Userutils;
import com.aiproject.smartcampus.exception.UserExpection;
import com.aiproject.smartcampus.mapper.StudentMapper;
import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;
import com.aiproject.smartcampus.pojo.po.Student;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;



/**
 * 优化后的学生注册实现
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StudentRegister implements RegsterStrategy {

    private final StudentMapper studentMapper;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    public static PasswordEncryptionUtils passwordEncoder = new PasswordEncryptionUtils();
    private static final String REDIS_KEY_PREFIX = "smartcampus:reg:student:";
    private static final Duration PRE_REG_TTL = Duration.ofMinutes(10);

    @Override
    public String Preliminaryregister(UserPreliminaryRegisterDTO dto) {
        if (!Userutils.preregisterInfoIsOK(dto)) {
            throw new UserExpection("注册信息不完整");
        }

        RLock lock = redissonClient.getLock("student:preregister");
        boolean acquired = false;
        try {
            acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!acquired) {
                    throw new UserExpection("系统繁忙，请稍后重试");
            }
            log.info("预处理 start, phone={}, account={}", dto.getPhone(), dto.getUserAccount());
            LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<Student>()
                    .eq(dto.getPhone() != null, Student::getPhone, dto.getPhone())
                    .eq(dto.getUserAccount() != null, Student::getStudentAccount, dto.getUserAccount());
            if (studentMapper.exists(wrapper)) {
                throw new UserExpection("该用户已经注册");
            }
            String token = UUID.randomUUID().toString();
            Student temp = new Student();
            temp.setStudentAccount(dto.getUserAccount());
            //对存入的密码进行加密处理
            temp.setPassword(passwordEncoder.encryption(dto.getPassword()));
            temp.setPhone(dto.getPhone());
            String payload = JsonUtils.toJsonString(temp);
            stringRedisTemplate.opsForValue().set(REDIS_KEY_PREFIX + token, payload, PRE_REG_TTL);
            log.info("预处理 end, token={}", token);
            return token;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程被中断，请重试");
        } catch (Exception e) {
            log.error("预处理失败 error", e);
            throw new UserExpection("预注册失败，请稍后重试");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String token, UserRegisterDTO dto) {
        if (!Userutils.validate(dto)) {
            throw new UserExpection("个人信息填写有误");
        }
        RLock lock = redissonClient.getLock("student:register");
        boolean acquired = false;
        try {
            acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!acquired) {
                throw new UserExpection("系统繁忙，请稍后重试");
            }
            String key = REDIS_KEY_PREFIX + token;
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached == null) {
                throw new UserExpection("注册已过期或无效，请重新预注册");
            }
            log.info("注册 start, token={}", token);
            Student pre = (Student) JsonUtils.toJsonObject(cached, Student.class);
            Student stu = dto.getStudent();
            stu.setStudentAccount(pre.getStudentAccount());
            stu.setPassword(pre.getPassword());
            stu.setPhone(pre.getPhone());

            studentMapper.insert(stu);
            stringRedisTemplate.delete(key);
            log.info("注册 success, studentId={}", stu.getStudentId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程被中断，请重试");
        } catch (Exception e) {
            log.error("注册 error", e);
            throw new UserExpection("注册失败，请稍后重试");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
