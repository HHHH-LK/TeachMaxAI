package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.mapper.StudentMapper;
import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.service.StudentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @program: SmartCampus
 * @description:
 * @author: lk
 * @create: 2025-05-20 08:38
 **/

@Service
@RequiredArgsConstructor
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    private final StudentMapper studentMapper;
    private final StringRedisTemplate stringRedisTemplate;


}
