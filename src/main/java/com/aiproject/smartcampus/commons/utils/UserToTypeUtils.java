package com.aiproject.smartcampus.commons.utils;

import com.aiproject.smartcampus.mapper.AdminMapper;
import com.aiproject.smartcampus.mapper.StudentMapper;
import com.aiproject.smartcampus.mapper.TeacherMapper;
import com.aiproject.smartcampus.mapper.UserMapper;
import com.aiproject.smartcampus.pojo.po.Admin;
import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.pojo.po.Teacher;
import com.aiproject.smartcampus.pojo.po.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @program: ss
 * @description:user转换成对映的身份
 * @author: lk_hhh
 * @create: 2025-07-02 19:25
 **/

@Component
@RequiredArgsConstructor
public class UserToTypeUtils {

    private final UserMapper userMapper;
    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;
    private final AdminMapper adminMapper;

    public String change() {

        User userInfo = UserLocalThreadUtils.getUserInfo();

        Integer Id = null;
        switch (userInfo.getUserType()) {
            case STUDENT: {
                LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Student::getUserId, userInfo.getUserId());
                Student student = studentMapper.selectOne(wrapper);
                Id = student.getStudentId();
            }
            break;

            case TEACHER: {
                LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Teacher::getUserId, userInfo.getUserId());
                Teacher teacher = teacherMapper.selectOne(wrapper);
                Id = teacher.getTeacherId();

            }
            break;

            case ADMIN: {
                LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Admin::getUserId, userInfo.getUserId());
                Admin admin = adminMapper.selectOne(wrapper);
                Id = admin.getAdminId();

            }
            break;

            default:
                throw new IllegalStateException("Unexpected value: " + userInfo.getUserType());
        }


        return Id.toString();
    }


}