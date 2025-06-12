package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.NotificateClient;
import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.websocket.NotificationWebSocketHandler;
import com.aiproject.smartcampus.exception.UserExpection;
import com.aiproject.smartcampus.mapper.*;
import com.aiproject.smartcampus.pojo.bo.NotificationMessage;
import com.aiproject.smartcampus.pojo.dto.UserLoginDTO;
import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;
import com.aiproject.smartcampus.pojo.po.Notificate;
import com.aiproject.smartcampus.pojo.po.NotificationAck;
import com.aiproject.smartcampus.service.CommonService;
import com.aiproject.smartcampus.strategy.login.AccountLogin;
import com.aiproject.smartcampus.strategy.login.PhoneLogin;
import com.aiproject.smartcampus.strategy.register.ManagePersonRegister;
import com.aiproject.smartcampus.strategy.UserStrategyContext;
import com.aiproject.smartcampus.strategy.register.StudentRegister;
import com.aiproject.smartcampus.strategy.register.TeacherRegister;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.management.Notification;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import static com.aiproject.smartcampus.contest.CommonContest.NOTIFICATION_KEY;
import static net.sf.jsqlparser.util.validation.metadata.NamedObject.role;
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

    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final ManagePersonMapper managePersonMapper;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ApplicationContext applicationContext;
    private final NotificateClient notificateClient;
    private final NotificationAckMapper notificationAckMapper;
    private final NotificateMapper notificateMapper;

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

    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 5)
    public Result notificationConfirm(String charater, String userId, String notificatioonId, String notifiationContent) {

        if (charater == null || charater.trim().isEmpty()) {
            throw new IllegalArgumentException("角色不能为空");
        }
        if (charater == null || charater.trim().isEmpty()) {
            throw new IllegalArgumentException("角色为null");
        }
        if (!charater.equals("Student") && !charater.equals("Teacher") && !charater.equals("ManagerPerson")) {
            throw new IllegalArgumentException("角色非法：角色非法：必须为 Student 或 Teacher 或 ManagerPerson");
        }

        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (!userId.matches("\\d+")) {
            throw new IllegalArgumentException("用户ID必须为数字");
        }

        if (notificatioonId == null || notificatioonId.trim().isEmpty()) {
            throw new IllegalArgumentException("通知ID不能为空");
        }
        if (!notificatioonId.matches("\\d+")) {
            throw new IllegalArgumentException("通知ID必须为数字");
        }

        if (notifiationContent == null || notifiationContent.trim().isEmpty()) {
            throw new IllegalArgumentException("通知内容不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        log.info("角色为：{}，id为：{}的用户已接收【{}】通知", character, userId, notifiationContent);

        try {
            // 创建确认消息记录
            NotificationAck notificationAck = new NotificationAck();
            notificationAck.setNotificationId(Long.parseLong(notificatioonId));
            notificationAck.setCreateTime(now);
            notificationAck.setUpdateTime(now);
            notificationAck.setRole(charater);
            notificationAck.setReadTime(now);
            notificationAck.setReceiverId(Integer.valueOf(userId));
            log.info("构建出来的通知绑定信息为【{}】", notificationAck.toString());

            // 进行插入，利用唯一索引避免重复确认
            try {
                int insert = notificationAckMapper.insert(notificationAck);
                if (insert <= 0) {
                    log.error("插入通知确认记录失败");
                    throw new UserExpection("插入通知确认记录失败");
                }
                log.info("通知确认记录插入成功");
            } catch (DuplicateKeyException e) {
                // 重复确认，直接返回成功
                log.warn("用户{}已经确认过通知{}", userId, notificatioonId);
                return Result.success("通知已确认");
            } catch (Exception e) {
                // 判断是否为重复键异常
                if (e.getMessage() != null && (e.getMessage().contains("Duplicate") || e.getMessage().contains("duplicate"))) {
                    log.warn("用户{}已经确认过通知{}", userId, notificatioonId);
                    return Result.success("通知已确认");
                }
                throw e; // 其他异常继续抛出
            }

            // 修改订阅数量 - 使用乐观锁+重试机制
            boolean isSuccess = false;
            Integer tryNum = 5;

            while (!isSuccess && tryNum > 0) {
                try {
                    // 获取当前通知信息
                    Notificate notification = notificateMapper.selectForUpdate(Long.parseLong(notificatioonId));
                    if (notification == null) {
                        log.error("通知不存在，通知ID：{}", notificatioonId);
                        throw new UserExpection("通知不存在");
                    }
                    if (notification.getNum() <= 0) {
                        log.warn("通知{}的订阅人数已为0，无需扣减", notificatioonId);
                        isSuccess = true;
                        break;
                    }
                    // 扣减操作 - 使用乐观锁
                    LambdaUpdateWrapper<Notificate> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.eq(Notificate::getId, Long.parseLong(notificatioonId))
                            .eq(Notificate::getNum, notification.getNum())
                            .setSql("num = num - 1");
                    int updateResult = notificateMapper.update(null, updateWrapper);
                    if (updateResult > 0) {
                        isSuccess = true;
                        log.info("扣减订阅通知人数成功，通知ID：{}，当前人数：{}", notificatioonId, notification.getNum() - 1);
                    } else {
                        log.warn("乐观锁冲突，准备重试，剩余次数：{}", tryNum - 1);
                    }
                } catch (Exception e) {
                    log.warn("扣减通知人数异常，剩余重试次数：{}", tryNum - 1, e);
                }
                // 重试逻辑
                if (!isSuccess) {
                    tryNum--;
                    if (tryNum > 0) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new UserExpection("操作被中断");
                        }
                    }
                }
            }

            if (!isSuccess) {
                log.error("扣减订阅通知人数失败，通知ID：{}，已重试5次", notificatioonId);
                throw new UserExpection("扣减订阅通知人数失败");
            }

            return Result.success("通知确认成功");

        } catch (NumberFormatException e) {
            log.error("数字格式转换错误", e);
            throw new IllegalArgumentException("数字格式转换错误");
        } catch (UserExpection e) {
            throw e;
        } catch (Exception e) {
            log.error("通知确认处理异常", e);
            throw new UserExpection("通知确认处理失败：" + e.getMessage());
        }
    }


}
