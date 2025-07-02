package com.aiproject.smartcampus.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.commons.utils.UserLocalThreadUtils;
import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.pojo.po.User;
import com.aiproject.smartcampus.service.CourseEnrollmentService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class CourseEnrollmentServiceImplTest {

    @Autowired
    private CourseEnrollmentService courseEnrollmentService;


    @Test
    void getCourseEnrollmentById() {

        User user = new User()
                .setUserId(9)
                .setUsername("student003")
                .setPasswordHash("$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi")  // 实际测试建议用加密密码
                .setRealName("梨花")
                .setEmail("testuser@example.com")
                .setPhone("13800138000")
                .setUserType(User.UserType.STUDENT)
                .setStatus(User.UserStatus.ACTIVE);


        UserLocalThreadUtils.setUserInfo(user);

        Result<String> stringResult = courseEnrollmentService.addCourseEnrollment(3);
        boolean success = stringResult.isSuccess();
        System.out.println(BooleanUtil.isTrue(success));


    }

}