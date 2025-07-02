package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.utils.PasswordEncryptionUtils;
import com.aiproject.smartcampus.mapper.StudentMapper;
import com.aiproject.smartcampus.mapper.UserMapper;
import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.pojo.po.User;
import com.aiproject.smartcampus.pojo.vo.StudentSelectAllVO;
import com.aiproject.smartcampus.service.StudentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    private final UserMapper userMapper;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 查询所有学生信息
     * @return
     */
    @Override
    public List<StudentSelectAllVO> selectAllStudents() {
        return studentMapper.selectAllStudents();
    }

    /**
     * 根据学生学号查询学生信息
     * @param studentNumber
     * @return
     */
    @Override
    public Student findByStudentNumber(String studentNumber) {
        Student student = studentMapper.findByStudentNumber(studentNumber);
        return student;
    }

    /**
     * 根据班级名称查询学生信息
     * @param className
     * @return
     */
    @Override
    public List<Student> findByClassName(String className) {
        return studentMapper.findByClassName(className);
    }

    /**
     * 更新学生信息
     * @param student
     */
    @Override
    @Transactional//添加事务注解，保证数据一致性
    public void updateStudent(Student student) {
        User user = new User();
        BeanUtils.copyProperties(student.getUser(), user);
         // 更新学生信息
        studentMapper.updateById(student);
        // 更新学生基本信息
         //加密密码
        user.setPasswordHash(PasswordEncryptionUtils.encryption(user.getPasswordHash()));
        userMapper.updateById(user);
    }

}
