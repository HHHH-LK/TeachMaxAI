package com.aiproject.smartcampus.service.impl;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.mapper.AdminMapper;
import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.pojo.po.User;
import com.aiproject.smartcampus.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AdminServiceImplTest {

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private AdminService adminService;

    @Test
    void getAdmin() {
        Result studentByStudentId = adminService.getStudentBystudentNumber("S2024001");
        Object data = studentByStudentId.getData();
        System.out.println(data);
    }

    @Test
    void deleteAdmin() {
        Result studentByStudentId = adminService.deleteStudentBystudentNumber("S2024010");

    }
    @Test
    void updateAdmin() {
        // 创建 User 对象并赋值
        User user = new User()
                .setUserId(9)
                .setUsername("student003")
                .setPasswordHash("$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi")  // 实际测试建议用加密密码
                .setRealName("梨花")
                .setEmail("testuser@example.com")
                .setPhone("13800138000")
                .setUserType(User.UserType.STUDENT)
                .setStatus(User.UserStatus.ACTIVE);

        // 创建 Student 对象并赋值，包括关联的 User
        Student student = new Student()
                .setStudentId(3)           // 如果是新增测试，一般不设置ID，由数据库自增
                .setUserId(user.getUserId())
                .setStudentNumber("S2024003")
                .setGrade("大一")
                .setClassName("计算机1班")
                .setUser(user);

        // 调用业务方法测试更新
        adminService.updateStudentBystudentNumber(student);
    }

    @Test
    void adddStudent() {
        // 创建 User 对象并赋值
        User user = new User()
                .setUsername("student010")
                .setPasswordHash("$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi")  // 实际测试建议用加密密码
                .setRealName("李白")
                .setEmail("testuser@example.com")
                .setPhone("13800138001")
                .setUserType(User.UserType.STUDENT)
                .setStatus(User.UserStatus.ACTIVE);

        // 创建 Student 对象并赋值，包括关联的 User
        Student student = new Student()
                .setUserId(user.getUserId())
                .setStudentNumber("S2024013")
                .setGrade("大一")
                .setClassName("计算机1班")
                .setUser(user);

        adminService.addStudent(student);

    }

}