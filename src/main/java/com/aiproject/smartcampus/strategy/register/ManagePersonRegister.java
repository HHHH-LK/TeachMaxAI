package com.aiproject.smartcampus.strategy.register;

import com.aiproject.smartcampus.commons.utils.JsonUtils;
import com.aiproject.smartcampus.commons.utils.PasswordEncryptionUtils;
import com.aiproject.smartcampus.commons.utils.Userutils;
import com.aiproject.smartcampus.exception.UserExpection;
import com.aiproject.smartcampus.mapper.ManagePersonMapper;
import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;
import com.aiproject.smartcampus.pojo.po.ManagePerson;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.aiproject.smartcampus.commons.utils.LockUtils.Redo;


/**
 * @program: SmartCampus
 * @description: 管理人员注册
 * @author: lk
 * @create: 2025-05-20 09:33
 **/

@Component
@RequiredArgsConstructor
@Slf4j
public class ManagePersonRegister implements RegsterStrategy {

    //预处理
    private final ManagePersonMapper managePersonMapper;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    public static PasswordEncryptionUtils passwordEncoder = new PasswordEncryptionUtils();
    private static final String REDIS_KEY_PREFIX = "smartcampus:reg:manager:";
    private static final Duration PRE_REG_TTL = Duration.ofMinutes(10);

    @Override
    public String Preliminaryregister(UserPreliminaryRegisterDTO userPreliminaryRegisterDTO) {

            if (!Userutils.preregisterInfoIsOK(userPreliminaryRegisterDTO)) {
                throw new UserExpection("注册信息不完整");
            }
            RLock lock = redissonClient.getLock("manager:preregister");
            boolean acquired = false;
            try {
                acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
                //重试机制
                if (!acquired) {
                    throw new UserExpection("系统繁忙，请稍后重试");
                }
                log.info("管理员预处理 start, phone={}, account={}", userPreliminaryRegisterDTO.getPhone(), userPreliminaryRegisterDTO.getUserAccount());
                LambdaQueryWrapper<ManagePerson> wrapper = new LambdaQueryWrapper<ManagePerson>()
                        .eq(userPreliminaryRegisterDTO.getPhone() != null, ManagePerson::getPhone, userPreliminaryRegisterDTO.getPhone())
                        .eq(userPreliminaryRegisterDTO.getUserAccount() != null, ManagePerson::getAccount, userPreliminaryRegisterDTO.getUserAccount());
                if (managePersonMapper.exists(wrapper)) {
                    throw new UserExpection("该管理员已注册");
                }
                String token = UUID.randomUUID().toString();
                ManagePerson temp = new ManagePerson();
                temp.setAccount(userPreliminaryRegisterDTO.getUserAccount());
                temp.setPassword(passwordEncoder.encryption(userPreliminaryRegisterDTO.getPassword()));
                temp.setPhone(userPreliminaryRegisterDTO.getPhone());
                String payload = JsonUtils.toJsonString(temp);
                stringRedisTemplate.opsForValue().set(REDIS_KEY_PREFIX + token, payload, PRE_REG_TTL);
                log.info("管理员预处理 end, token={}", token);
                return token;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("线程被中断，请重试");
            } catch (Exception e) {
                log.error("管理员预处理失败 error", e);
                throw new UserExpection("预注册失败，请稍后重试");
            } finally {
                if (acquired && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
    }

    @Override
    public void register(String token, UserRegisterDTO userRegisterDTO) throws Exception {
        //注册
        if (!Userutils.validate(userRegisterDTO)) {
            throw new UserExpection("个人信息填写有误");
        }
        RLock lock = redissonClient.getLock("manager:register");
        boolean acquired = false;
        try {
            if (!acquired) {
                throw new UserExpection("系统繁忙，请稍后重试");
            }
            String key = REDIS_KEY_PREFIX + token;
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached == null) {
                throw new UserExpection("注册已过期或无效，请重新预注册");
            }
            log.info("管理员注册 start, token={}", token);
            ManagePerson pre = (ManagePerson) JsonUtils.toJsonObject(cached, ManagePerson.class);
            ManagePerson mgr = userRegisterDTO.getManagePerson();
            mgr.setAccount(pre.getAccount());
            mgr.setPassword(pre.getPassword());
            mgr.setPhone(pre.getPhone());
            managePersonMapper.insert(mgr);
            stringRedisTemplate.delete(key);
            log.info("管理员注册 success, adminId={}", mgr.getAdminId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程被中断，请重试");
        } catch (Exception e) {
            log.error("管理员注册 error", e);
            throw new UserExpection("注册失败，请稍后重试");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
