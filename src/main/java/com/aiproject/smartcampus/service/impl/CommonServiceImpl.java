package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.Result;
import com.aiproject.smartcampus.exception.UserExpection;
import com.aiproject.smartcampus.mapper.ManagePersonMapper;
import com.aiproject.smartcampus.mapper.StudentMapper;
import com.aiproject.smartcampus.mapper.TeacherMapper;
import com.aiproject.smartcampus.pojo.dto.UserLoginDTO;
import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;
import com.aiproject.smartcampus.service.CommonService;
import com.aiproject.smartcampus.strategy.login.AccountLogin;
import com.aiproject.smartcampus.strategy.login.PhoneLogin;
import com.aiproject.smartcampus.strategy.register.ManagePersonRegister;
import com.aiproject.smartcampus.strategy.UserStrategyContext;
import com.aiproject.smartcampus.strategy.register.StudentRegister;
import com.aiproject.smartcampus.strategy.register.TeacherRegister;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static net.sf.jsqlparser.util.validation.metadata.NamedObject.role;

/**
 * @program: SmartCampus
 * @description: 公用实现类
 * @author: lk
 * @create: 2025-05-20 09:21
 **/

@Service
@RequiredArgsConstructor
public class CommonServiceImpl implements CommonService {

    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final ManagePersonMapper managePersonMapper;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ApplicationContext applicationContext;

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
    public Result userLogin(UserLoginDTO userLoginDTO) {

        UserStrategyContext userStrategyContext = new UserStrategyContext();
        switch (userLoginDTO.getType()) {
            case "Account":
                userStrategyContext.setLoginStrategy(new AccountLogin(stringRedisTemplate,redissonClient,applicationContext));
                break;
            case "Phone":
                userStrategyContext.setLoginStrategy(new PhoneLogin());
                break;

        }


        return null;
    }


}
