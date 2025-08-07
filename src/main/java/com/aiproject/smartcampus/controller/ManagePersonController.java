package com.aiproject.smartcampus.controller;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.Student;
import com.aiproject.smartcampus.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @program: SmartCampus
 * @description: 管理人控制层
 * @author: lk
 * @create: 2025-05-20 09:49
 **/

@RequiredArgsConstructor
@RestController
@RequestMapping("/managePerson")
public class ManagePersonController {

    private final AdminService adminService;


    /**
     * 根据学号查询学生信息
     */
    @GetMapping("/getstudent/student_number")
    public Result<Student> getStudentsBystudentNumber(@RequestParam(value = "studentNumber") String studentNumber) {

        return adminService.getStudentBystudentNumber(studentNumber);

    }

    /**
     * 根据学号删除学生信息
     */
    @PostMapping("/deletestudent/student_number")
    public Result deleteStudentsBystudentNumber(@RequestParam(value = "studentNumber") String studentNumber) {

        return adminService.deleteStudentBystudentNumber(studentNumber);

    }

    /**
     * 根据学号修改学生信息
     */
    @PostMapping("/updatestudent/student_number")
    public Result updateStudentsByStudentId(@RequestBody Student student) {

        return adminService.updateStudentBystudentNumber(student);

    }

    /**
     * 新增学生信息
     */
    @PostMapping("/addstudent")
    public Result addStudent(@RequestBody Student student) {

        return adminService.addStudent(student);
    }

    /**
     * 修改用户状态
     */
    @PostMapping("/updateuserstatus")
    public Result updateUserStatus(String userId, String status) {

        return adminService.updateUserStatus(userId, status);
    }


}
