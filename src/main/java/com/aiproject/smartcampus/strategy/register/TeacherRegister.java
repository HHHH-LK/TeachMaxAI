package com.aiproject.smartcampus.strategy.register;

import com.aiproject.smartcampus.commons.utils.JsonUtils;
import com.aiproject.smartcampus.commons.utils.PasswordEncryptionUtils;
import com.aiproject.smartcampus.commons.utils.Userutils;
import com.aiproject.smartcampus.exception.UserExpection;
import com.aiproject.smartcampus.mapper.TeacherMapper;
import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;
import com.aiproject.smartcampus.pojo.po.Teacher;
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
 * @program: SmartCampus
 * @description: 教师注册
 * @author: lk
 * @create: 2025-05-20 09:32
 **/

@Component
@RequiredArgsConstructor
@Slf4j
public class TeacherRegister implements RegsterStrategy {

    private final TeacherMapper teacherMapper;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    public static PasswordEncryptionUtils passwordEncoder = new PasswordEncryptionUtils();
    private static final String REDIS_KEY_PREFIX = "reg:teacher:";
    private static final Duration PRE_REG_TTL = Duration.ofMinutes(10);

    @Override
    public String Preliminaryregister(UserPreliminaryRegisterDTO userPreliminaryRegisterDTO) {
        //预处理
        if (!Userutils.preregisterInfoIsOK(userPreliminaryRegisterDTO)) {
            throw new UserExpection("注册信息不完整");
        }
        RLock lock = redissonClient.getLock("teacher:preregister");
        boolean acquired = false;
        try {
            acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!acquired) {
                throw new UserExpection("系统繁忙，请稍后重试");
            }
            log.info("教师预处理 start, phone={}, account={}", userPreliminaryRegisterDTO.getPhone(), userPreliminaryRegisterDTO.getUserAccount());
            LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<Teacher>()
                    .eq(userPreliminaryRegisterDTO.getPhone() != null, Teacher::getPhone, userPreliminaryRegisterDTO.getPhone())
                    .eq(userPreliminaryRegisterDTO.getUserAccount() != null, Teacher::getAccount, userPreliminaryRegisterDTO.getUserAccount());
            if (teacherMapper.exists(wrapper)) {
                throw new UserExpection("该教师已注册");
            }
            String token = UUID.randomUUID().toString();
            Teacher temp = new Teacher();
            temp.setAccount(userPreliminaryRegisterDTO.getUserAccount());
            temp.setPassword(passwordEncoder.encryption(userPreliminaryRegisterDTO.getPassword()));
            temp.setPhone(userPreliminaryRegisterDTO.getPhone());
            String payload = JsonUtils.toJsonString(temp);
            stringRedisTemplate.opsForValue().set(REDIS_KEY_PREFIX + token, payload, PRE_REG_TTL);
            log.info("教师预处理 end, token={}", token);
            return token;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UserExpection("线程被中断，请重试");
        } catch (Exception e) {
            log.error("教师预处理失败 error", e);
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
        RLock lock = redissonClient.getLock("teacher:register");
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
            log.info("教师注册 start, token={}", token);
            Teacher pre = (Teacher) JsonUtils.toJsonObject(cached, Teacher.class);
            Teacher tea = dto.getTeacher();
            tea.setAccount(pre.getAccount());
            tea.setPassword(pre.getPassword());
            tea.setPhone(pre.getPhone());
            teacherMapper.insert(tea);
            stringRedisTemplate.delete(key);
            log.info("教师注册 success, teacherId={}", tea.getTeacherId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UserExpection("线程被中断，请重试");
        } catch (Exception e) {
            log.error("教师注册 error", e);
            throw new UserExpection("注册失败，请稍后重试");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}

