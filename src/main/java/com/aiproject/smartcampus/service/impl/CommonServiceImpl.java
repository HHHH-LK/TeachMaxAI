package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.Result;
import com.aiproject.smartcampus.mapper.ManagePersonMapper;
import com.aiproject.smartcampus.mapper.StudentMapper;
import com.aiproject.smartcampus.mapper.TeacherMapper;
import com.aiproject.smartcampus.pojo.dto.UserPreliminaryRegisterDTO;
import com.aiproject.smartcampus.pojo.dto.UserRegisterDTO;
import com.aiproject.smartcampus.service.CommonService;
import com.aiproject.smartcampus.strategy.register.ManagePersonRegister;
import com.aiproject.smartcampus.strategy.register.RegistrationContext;
import com.aiproject.smartcampus.strategy.register.StudentRegister;
import com.aiproject.smartcampus.strategy.register.TeacherRegister;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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

    /**
     * 预注册
     * */
    @Override
    public Result userPreliminaryregister(UserPreliminaryRegisterDTO userPreliminaryRegisterDTO) throws Exception {

       RegistrationContext registrationContext = new RegistrationContext();
       switch (userPreliminaryRegisterDTO.getCharacter()){
           case "student": registrationContext.setRegsterStrategy(new StudentRegister(studentMapper,redissonClient,stringRedisTemplate));break;
           case "teacher": registrationContext.setRegsterStrategy(new TeacherRegister(teacherMapper,redissonClient,stringRedisTemplate));break;
           case "managerPerson": registrationContext.setRegsterStrategy(new ManagePersonRegister(managePersonMapper)); break;
       }
       //进行预处理
        String preliminaryregisterregistertoken = registrationContext.Preliminaryregisterregister(userPreliminaryRegisterDTO);

        return Result.success(preliminaryregisterregistertoken);
    }

    /**
     * 注册完成（填写个人信息）
     * */
    @Override
    public Result userRegister(String registrationToken, UserRegisterDTO userRegisterDTO) throws Exception {

        RegistrationContext registrationContext = new RegistrationContext();
        switch (userRegisterDTO.getCharacter()){
            case "student": registrationContext.setRegsterStrategy(new StudentRegister(studentMapper,redissonClient,stringRedisTemplate));break;
            case "teacher": registrationContext.setRegsterStrategy(new TeacherRegister(teacherMapper,redissonClient,stringRedisTemplate));break;
            case "managerPerson": registrationContext.setRegsterStrategy(new ManagePersonRegister(managePersonMapper)); break;
        }
        registrationContext.register(registrationToken,userRegisterDTO);

        return Result.success();
    }



}
