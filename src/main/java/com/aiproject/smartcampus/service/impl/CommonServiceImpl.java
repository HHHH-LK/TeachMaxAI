package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.NotificateClient;
import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.websocket.NotificationWebSocketHandler;
import com.aiproject.smartcampus.exception.UserExpection;
import com.aiproject.smartcampus.mapper.ManagePersonMapper;
import com.aiproject.smartcampus.mapper.StudentMapper;
import com.aiproject.smartcampus.mapper.TeacherMapper;
import com.aiproject.smartcampus.pojo.bo.NotificationMessage;
import com.aiproject.smartcampus.pojo.dto.UserLoginDTO;
import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;
import com.aiproject.smartcampus.pojo.po.Notificate;
import com.aiproject.smartcampus.service.CommonService;
import com.aiproject.smartcampus.strategy.login.AccountLogin;
import com.aiproject.smartcampus.strategy.login.PhoneLogin;
import com.aiproject.smartcampus.strategy.register.ManagePersonRegister;
import com.aiproject.smartcampus.strategy.UserStrategyContext;
import com.aiproject.smartcampus.strategy.register.StudentRegister;
import com.aiproject.smartcampus.strategy.register.TeacherRegister;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.aiproject.smartcampus.contest.CommonContest.NOTIFICATION_KEY;
import static net.sf.jsqlparser.util.validation.metadata.NamedObject.role;

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

    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final ManagePersonMapper managePersonMapper;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ApplicationContext applicationContext;
    private final NotificateClient notificateClient;

    /**
     * 预注册
     */
    @Override
    public Result userPreliminaryregister(UserPreliminaryRegisterDTO userPreliminaryRegisterDTO) throws Exception {

        UserStrategyContext userStrategyContext = new UserStrategyContext();
        switch (userPreliminaryRegisterDTO.getCharacter()) {
            case "Student":
                userStrategyContext.setRegsterStrategy(new StudentRegister(studentMapper, redissonClient, stringRedisTemplate));
                break;
            case "Teacher":
                userStrategyContext.setRegsterStrategy(new TeacherRegister(teacherMapper, redissonClient, stringRedisTemplate));
                break;
            case "ManagerPerson":
                userStrategyContext.setRegsterStrategy(new ManagePersonRegister(managePersonMapper, redissonClient, stringRedisTemplate));
                break;
            default:
                throw new UserExpection("无效的角色：" + role);
        }
        //进行预处理
        String preliminaryregisterregistertoken = userStrategyContext.Preliminaryregisterregister(userPreliminaryRegisterDTO);

        return Result.success(preliminaryregisterregistertoken);
    }

    /**
     * 注册完成（填写个人信息）
     */
    @Override
    public Result userRegister(String registrationToken, UserRegisterDTO userRegisterDTO) throws Exception {

        UserStrategyContext userStrategyContext = new UserStrategyContext();
        switch (userRegisterDTO.getCharacter()) {
            case "Student":
                userStrategyContext.setRegsterStrategy(new StudentRegister(studentMapper, redissonClient, stringRedisTemplate));
                break;
            case "Teacher":
                userStrategyContext.setRegsterStrategy(new TeacherRegister(teacherMapper, redissonClient, stringRedisTemplate));
                break;
            case "ManagerPerson":
                userStrategyContext.setRegsterStrategy(new ManagePersonRegister(managePersonMapper, redissonClient, stringRedisTemplate));
                break;
            default:
                throw new UserExpection("无效的角色：" + role);
        }
        userStrategyContext.register(registrationToken, userRegisterDTO);

        return Result.success();
    }

    /**
     * 用户登录功能
     */
    @Override
    public Result userLogin(UserLoginDTO userLoginDTO) throws Exception {

        UserStrategyContext userStrategyContext = new UserStrategyContext();
        switch (userLoginDTO.getType()) {
            case "Account":
                userStrategyContext.setLoginStrategy(new AccountLogin(stringRedisTemplate, redissonClient, applicationContext));
                break;
            case "Phone":
                userStrategyContext.setLoginStrategy(new PhoneLogin(stringRedisTemplate, redissonClient, applicationContext));
                break;
            default:
                throw new UserExpection("无效的登录方式");
        }

        String logintoken = userStrategyContext.login(userLoginDTO);
        return Result.success(logintoken);
    }

    @Override
    public Result userLogout(String token) {
        //基于账号进行退出
        String user = stringRedisTemplate.opsForValue().get(token);
        if (StrUtil.isBlank(user)) {
            throw new UserExpection("用户未登录");
        }
        stringRedisTemplate.delete(token);
        return Result.success();

    }

    @Override
    public Result upload(MultipartFile file) {
        //todo 待完成

        return null;
    }

    @Override
    public Result notificateService(Integer userId) {
        try {
            Notificate notificate = notificateClient.getNotificate(String.valueOf(userId));
            if (notificate != null && !StrUtil.isBlank(notificate.getContent())) {
                log.info("获取到用户 {} 的通知: {}", userId, notificate.getContent());

                NotificationMessage message = NotificationMessage.builder()
                        .id(notificate.getId())
                        .content(notificate.getContent())
                        .type(String.valueOf(notificate.getType()))
                        .createTime(notificate.getCreateTime())
                        .receiverId(notificate.getReceiverId())
                        .messageType("notification")
                        .build();

                NotificationWebSocketHandler.sendNotificationToUser(String.valueOf(userId), message);
                return Result.success(message);
            } else {
                log.info("用户 {} 暂无新通知", userId);
                return Result.success("暂无新通知");
            }
        } catch (Exception e) {
            log.error("获取用户 {} 通知失败", userId, e);
            return Result.error("获取通知失败");
        }
    }

    @Override
    public Result broadcastNotification(String content) {
        try {
            NotificationMessage message = NotificationMessage.builder()
                    .content(content)
                    .type("system")
                    .createTime(LocalDateTime.now())
                    .messageType("broadcast")
                    .build();

            NotificationWebSocketHandler.broadcastNotification(message);
            log.info("系统广播通知发送成功: {}", content);
            return Result.success("广播发送成功");
        } catch (Exception e) {
            log.error("系统广播通知发送失败", e);
            return Result.error("广播发送失败");
        }
    }




}
