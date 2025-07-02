package com.aiproject.smartcampus.service;

import com.aiproject.smartcampus.commons.client.Result;
import com.aiproject.smartcampus.pojo.po.Student;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AdminService   {

    //根据学号获取学生信息
    public Result getStudentBystudentNumber(String studentNumber);

    Result deleteStudentBystudentNumber(String studentNumber);

    Result updateStudentBystudentNumber(Student student);

    Result addStudent(Student student);

    Result updateUserStatus(String userId, String status);
}
